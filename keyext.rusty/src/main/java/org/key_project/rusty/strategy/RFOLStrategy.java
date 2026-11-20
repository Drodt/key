/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.TermOrdering;
import org.key_project.prover.rules.IBuiltInRule;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.strategy.ComponentStrategy;
import org.key_project.prover.strategy.FOLStrategy;
import org.key_project.prover.strategy.IFormulaTermFeatures;
import org.key_project.prover.strategy.costbased.CostBand;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.RuleSetDispatchFeature;
import org.key_project.prover.strategy.costbased.feature.SumFeature;
import org.key_project.prover.strategy.costbased.quantifierHeuristics.TriggerTreatment;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.strategy.quantifierHeuristics.ClausesSmallerThanFeature;
import org.key_project.rusty.strategy.termfeature.ContainsExecutableCodeTermFeature;

import org.jspecify.annotations.NonNull;

import static org.key_project.prover.strategy.costbased.quantifierHeuristics.TriggerTreatment.*;
import static org.key_project.rusty.strategy.RustStaticFeatureCollection.literalsSmallerThan;
import static org.key_project.rusty.strategy.RustStaticFeatureCollection.sequentContainsNoPrograms;

public class RFOLStrategy extends FOLStrategy<Goal> implements ComponentStrategy<Goal> {
    public static final Name NAME = new Name("JFOL Strategy");
    protected final StrategyProperties strategyProperties;

    private final Proof proof;

    public RFOLStrategy(IFormulaTermFeatures ff, Proof proof,
            StrategyProperties strategyProperties) {
        super(ff, new RustFeatureConstants(), CostBand.DEFAULT.cost());
        this.proof = proof;
        this.strategyProperties = (StrategyProperties) strategyProperties.clone();
        init();
    }

    @Override
    protected void setupCostComputationF(@NonNull RuleSetDispatchFeature d) {
        super.setupCostComputationF(d);
        bindRuleSet(d, "apply_auxiliary_eq", inftyConst());
    }

    @Override
    protected void setupFormulaNormalisation(RuleSetDispatchFeature d) {
        super.setupFormulaNormalisation(d);
        var numbers = getServices().getLDTs().getIntLDT();
        bindRuleSet(d, "cnf_orComm",
            SumFeature.createSum(applyTF("commRight", ff.clause()),
                applyTFNonStrict("commResidue", ff.clauseSet()),
                or(applyTF("commLeft", ff.andF()),
                    add(applyTF("commLeft", ff.literal()),
                        literalsSmallerThan("commRight", "commLeft", numbers))),
                longConst(-100)));
        bindRuleSet(d, "cnf_andComm",
            SumFeature.createSum(applyTF("commLeft", ff.clause()),
                applyTF("commRight", ff.clauseSet()),
                applyTFNonStrict("commResidue", ff.clauseSet()),
                // at least one of the subformulas has to be a literal;
                // otherwise, sorting is not likely to have any big effect
                ifZero(
                    add(applyTF("commLeft", not(ff.literal())),
                        applyTF("commRight", rec(ff.andF(), not(ff.literal())))),
                    longConst(100), longConst(-60)),
                clausesSmallerThan("commRight", "commLeft", numbers)));
    }

    @Override
    protected void setupSplitting(RuleSetDispatchFeature d) {
        super.setupSplitting(d);
        ProjectionToTerm cutFormula = instOf("cutFormula");
        bindRuleSet(d, "cut_direct",
            SumFeature
                    .createSum(
                        ifZero(notBelowQuantifier(ff),
                            longConst(1000))));
    }

    private Feature clausesSmallerThan(String smaller, String bigger, IntLDT numbers) {
        return ClausesSmallerThanFeature.create(instOf(smaller), instOf(bigger),
            createTermOrdering(), numbers);
    }

    @Override
    protected Services getServices() {
        return proof.getServices();
    }

    /// the value of the trigger option, fixed when this strategy was constructed
    @Override
    protected String triggersOption() {
        return strategyProperties.getProperty(StrategyProperties.TRIGGERS_OPTIONS_KEY);
    }

    @Override
    protected TriggerTreatment triggerTreatment() {
        return forOption(triggersOption());
    }

    public TriggerTreatment forOption(String option) {
        if (StrategyProperties.TRIGGERS_CLASSIC.equals(option)) {
            return CLASSIC;
        }
        return StrategyProperties.TRIGGERS_GOOD.equals(option) ? GOOD : BEST;
    }

    @Override
    public Name name() {
        return NAME;
    }

    @Override
    protected boolean instQuantifiersWithQueries() {
        return StrategyProperties.QUANTIFIERS_INSTANTIATE
                .equals(strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY));
    }

    @Override
    protected boolean quantifiersMightSplit() {
        return StrategyProperties.QUANTIFIERS_INSTANTIATE
                .equals(strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY))
                || StrategyProperties.QUANTIFIERS_NON_SPLITTING_WITH_PROGS.equals(
                    strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY));
    }

    @Override
    protected Feature allowQuantifierSplitting() {
        if (StrategyProperties.QUANTIFIERS_INSTANTIATE.equals(
            strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY))) {
            return longConst(0);
        }
        if (StrategyProperties.QUANTIFIERS_NON_SPLITTING_WITH_PROGS.equals(
            strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY))) {
            return sequentContainsNoPrograms();
        }
        return inftyConst();
    }

    @Override
    protected boolean quantifierInstantiatedEnabled() {
        return !StrategyProperties.QUANTIFIERS_NONE
                .equals(strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY));
    }

    @Override
    protected Feature allowSplitting(ProjectionToTerm focus) {
        if (normalSplitting()) {
            return longConst(0);
        }
        if (StrategyProperties.SPLITTING_DELAYED
                .equals(strategyProperties.getProperty(StrategyProperties.SPLITTING_OPTIONS_KEY))) {
            return or(applyTF(focus, ContainsExecutableCodeTermFeature.PROGRAMS),
                sequentContainsNoPrograms());
        }
        // else: SPLITTING_OFF
        return applyTF(focus, ContainsExecutableCodeTermFeature.PROGRAMS);
    }

    @Override
    protected boolean normalSplitting() {
        return StrategyProperties.SPLITTING_NORMAL
                .equals(strategyProperties.getProperty(StrategyProperties.SPLITTING_OPTIONS_KEY));
    }

    @Override
    protected TermOrdering createTermOrdering() {
        return RustStaticFeatureCollection.createTermOrdering();
    }

    @Override
    public boolean isResponsibleFor(IBuiltInRule<Goal> rule) {
        return false;
    }

    @Override
    protected RuleSet getHeuristic(String p_name) {
        final NamespaceSet nss = proof.getNamespaces();
        final Namespace<@NonNull RuleSet> ns = nss.ruleSets();
        final RuleSet h = ns.lookup(new Name(p_name));

        assert h != null : "Did not find the rule set " + p_name;

        return h;
    }
}
