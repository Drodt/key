/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import java.util.HashSet;
import java.util.Set;

import org.key_project.logic.*;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.IBuiltInRule;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.ComponentStrategy;
import org.key_project.prover.strategy.IFormulaTermFeatures;
import org.key_project.prover.strategy.IntegerStrategy;
import org.key_project.prover.strategy.costbased.DivModCost;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.*;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.strategy.feature.*;

import org.jspecify.annotations.NonNull;

/// This strategy implements reasoning for integer arithmetics. In particular,
/// it supports linear integer arithmetics via Gaussian elimination,
/// Fourier-Motzkin; and non-linear integer reasoning via cross-multiplication
/// and Gröbner basis.
///
/// Do not create directly, instead use [IntegerStrategyFactory].
public class RustIntegerStrategy extends IntegerStrategy<Goal> implements ComponentStrategy<Goal> {
    private final Proof proof;

    /// Caps how often a cross multiplication is applied on a branch.
    /// Justified by empirical measurements. Candidate to be exposed in
    /// a settings strategy pane (not the strategy pane)
    private static final int BRANCH_MULT_CAP = 8;

    public RustIntegerStrategy(Config cfg, IFormulaTermFeatures ff, Proof proof) {
        super(cfg, ff, new RustFeatureConstants());
        this.proof = proof;
        init();
    }

    @Override
    protected void setupApprovalDispatcher(RuleSetDispatchFeature d) {
        super.setupApprovalDispatcher(d);

        bindRuleSet(d, "defOps_rdiv", NonDuplicateAppModPositionFeature.INSTANCE);
    }


    // //////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////
    //
    // Axiomatisation and algorithms for further arithmetic operations:
    // division, modulus, modular Java operations
    //
    // //////////////////////////////////////////////////////////////////////////
    // //////////////////////////////////////////////////////////////////////////

    @Override
    protected void setupDefOpsPrimaryCategories(RuleSetDispatchFeature d) {
        super.setupDefOpsPrimaryCategories(d);
        if (arith != ArithTreatment.BASIC) {
            bindRuleSet(d, "defOps_rdiv",
                SumFeature.createSum(NonDuplicateAppModPositionFeature.INSTANCE,
                    applyTF("divNum", tf.polynomial), applyTF("divDenom", tf.polynomial),
                    applyTF("divNum", tf.notContainsDivMod),
                    applyTF("divDenom", tf.notContainsDivMod),
                    ifZero(isBelow(ff.modalOperator()), longConst(DivModCost.BELOW_MODALITY))));

            bindRuleSet(d, "defOps_rdiv_inline", add(applyTF("divNum", tf.literal),
                applyTF("divDenom", tf.polynomial), longConst(DivModCost.INLINE)));
        } else {
            bindRuleSet(d, "defOps_rdiv", inftyConst());
            bindRuleSet(d, "defOps_rdiv_inline", add(applyTF("divNum", tf.literal),
                applyTF("divDenom", tf.literal), longConst(DivModCost.MOD)));
        }
    }

    @Override
    public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
        return !(approvalDispatcher.computeCost(app, pio, goal,
            new MutableState()) == TopRuleAppCost.INSTANCE);

    }

    @Override
    public RuleAppCost instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
            MutableState mState) {
        return instantiationDispatcher.computeCost(app, pio, goal, mState);
    }

    @Override
    public @NonNull Name name() {
        return NAME;
    }

    @Override
    public RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        return this.costComputationDispatcher.computeCost(app, pos, goal, mState);
    }

    @Override
    public Set<RuleSet> getResponsibilities(StrategyAspect aspect) {
        return new HashSet<>(getDispatcher(aspect).ruleSets());
    }

    @Override
    public RuleSetDispatchFeature getDispatcher(StrategyAspect aspect) {
        return switch (aspect) {
            case StrategyAspect.Cost -> costComputationDispatcher;
            case StrategyAspect.Instantiation -> instantiationDispatcher;
            case StrategyAspect.Approval -> approvalDispatcher;
        };
    }

    @Override
    public boolean isResponsibleFor(IBuiltInRule<Goal> rule) {
        return false;
    }

    @Override
    protected Feature getBranchCountFeatureFor(String prefix) {
        return BranchMultiplicationCountFeature.atMost(prefix, BRANCH_MULT_CAP);
    }

    @Override
    protected Term zTerm(String num) {
        return proof.getServices().getTermBuilder().zTerm(num);
    }

    @Override
    protected TermOrdering createTermOrdering() {
        return RustStaticFeatureCollection.createTermOrdering();
    }

    @Override
    protected RuleSet getHeuristic(String p_name) {
        final NamespaceSet nss = proof.getNamespaces();
        final Namespace<@NonNull RuleSet> ns = nss.ruleSets();
        final RuleSet h = ns.lookup(new Name(p_name));

        assert h != null : "Did not find the rule set " + p_name;

        return h;
    }

    @Override
    protected LogicServices getServices() {
        return proof.getServices();
    }
}
