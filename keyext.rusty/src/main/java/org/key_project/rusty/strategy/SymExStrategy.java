/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import org.key_project.logic.Name;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.FindDepthFeature;
import org.key_project.prover.strategy.costbased.feature.ScaleFeature;
import org.key_project.prover.strategy.costbased.feature.SumFeature;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.strategy.feature.DiffFindAndIfFeature;
import org.key_project.rusty.strategy.feature.MatchedAssumesFeature;
import org.key_project.rusty.strategy.feature.RuleSetDispatchFeature;
import org.key_project.rusty.strategy.feature.findprefix.FindPrefixRestrictionFeature;
import org.key_project.rusty.strategy.termProjection.TermBuffer;
import org.key_project.rusty.strategy.termgenerator.SuperTermGenerator;

import org.jspecify.annotations.NonNull;

/// Strategy for symbolic execution rules
public class SymExStrategy extends AbstractFeatureStrategy {
    public static final Name NAME = new Name("SymExStrategy");

    private final FormulaTermFeatures ff;

    private final StrategyProperties strategyProperties;

    private final RuleSetDispatchFeature costComputationDispatcher;
    private final Feature costComputationF;
    private final RuleSetDispatchFeature instantiationDispatcher;
    private final Feature instantiationF;

    public SymExStrategy(Proof proof, StrategyProperties strategyProperties) {
        super(proof);

        this.strategyProperties = strategyProperties;
        var tf = new ArithTermFeatures(getServices().getLDTs().getIntLDT());
        ff = new FormulaTermFeatures(tf);

        costComputationDispatcher = setupCostComputationF();
        instantiationDispatcher = new RuleSetDispatchFeature();

        costComputationF = setupGlobalF(costComputationDispatcher);
        instantiationF = setupGlobalF(instantiationDispatcher);
    }

    @Override
    public boolean isResponsibleFor(RuleSet rs) {
        return costComputationDispatcher.get(rs) != null || instantiationDispatcher.get(rs) != null;
    }

    private Feature setupGlobalF(Feature dispatcher) {
        final Feature functionSpecF;
        final String methProp =
            strategyProperties.getProperty(StrategyProperties.FUNCTION_OPTIONS_KEY);
        switch (methProp) {
            case StrategyProperties.FUNCTION_CONTRACT ->
                functionSpecF = functionSpecFeature(longConst(-20));
            case StrategyProperties.FUNCTION_EXPAND, StrategyProperties.FUNCTION_NONE ->
                functionSpecF =
                    functionSpecFeature(inftyConst());
            default -> {
                functionSpecF = null;
                assert false;
            }
        }

        return SumFeature.createSum(functionSpecF, dispatcher);
    }

    private RuleSetDispatchFeature setupCostComputationF() {
        final RuleSetDispatchFeature d = new RuleSetDispatchFeature();
        boolean programsToRight = true; // XXX

        bindRuleSet(d, "simplify_prog",
            ifZero(isBelow(add(ff.forF, not(ff.atom))), longConst(200), longConst(-100)));

        bindRuleSet(d, "simplify_prog_subset", longConst(-4000));

        bindRuleSet(d, "simplify_expression", -100);

        bindRuleSet(d, "simplify_rust", -4500);

        bindRuleSet(d, "executeIntegerAssignment", -100);

        final Feature findDepthFeature =
            FindDepthFeature.getInstance();
        bindRuleSet(d, "concrete_rust",
            add(longConst(-11000),
                ScaleFeature.createScaled(findDepthFeature, 10.0)));

        // taclets for special invariant handling
        bindRuleSet(d, "loopInvariant", -20000);

        boolean useLoopExpand = strategyProperties.getProperty(StrategyProperties.LOOP_OPTIONS_KEY)
                .equals(StrategyProperties.LOOP_EXPAND);
        boolean useLoopInvTaclets =
            strategyProperties.getProperty(StrategyProperties.LOOP_OPTIONS_KEY)
                    .equals(StrategyProperties.LOOP_INVARIANT);

        bindRuleSet(d, "loop_expand", useLoopExpand ? longConst(1000) : inftyConst());
        bindRuleSet(d, "loop_scope_inv_taclet", useLoopInvTaclets ? longConst(0) : inftyConst());


        final String fnProp =
            strategyProperties.getProperty(StrategyProperties.FUNCTION_OPTIONS_KEY);
        switch (fnProp) {
            case StrategyProperties.FUNCTION_CONTRACT ->
                /*
                 * If function treatment by contracts is chosen, this does not mean that function
                 * expansion
                 * is disabled. The original cost was 200 and is now increased to 2000 in order to
                 * repress function expansion stronger when function treatment by contracts is
                 * chosen.
                 */
                bindRuleSet(d, "function_expand", longConst(2000));
            case StrategyProperties.FUNCTION_EXPAND ->
                bindRuleSet(d, "function_expand", longConst(100));
            case StrategyProperties.FUNCTION_NONE ->
                bindRuleSet(d, "function_expand", inftyConst());
            default -> throw new RuntimeException("Unexpected strategy property " + fnProp);
        }

        bindRuleSet(d, "modal_tautology", longConst(-10000));

        if (programsToRight) {
            bindRuleSet(d, "boxDiamondConv",
                SumFeature.createSum(
                    new FindPrefixRestrictionFeature(
                        FindPrefixRestrictionFeature.PositionModifier.ALLOW_UPDATE_AS_PARENT,
                        FindPrefixRestrictionFeature.PrefixChecker.ANTEC_POLARITY),
                    longConst(-1000)));
        } else {
            bindRuleSet(d, "boxDiamondConv", inftyConst());
        }

        bindRuleSet(d, "confluence_restricted",
            ifZero(MatchedAssumesFeature.INSTANCE, DiffFindAndIfFeature.INSTANCE));

        final var superFor = new TermBuffer();
        bindRuleSet(d, "split_if",
            add(sum(superFor, SuperTermGenerator.upwards(any(), getServices()),
                applyTF(superFor, not(ff.program))), longConst(50)));

        return d;
    }

    @Override
    protected RuleAppCost instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
            MutableState mState) {
        return instantiationF.computeCost(app, pio, goal, mState);
    }

    @Override
    public boolean isStopAtFirstNonCloseableGoal() {
        return false;
    }

    @Override
    public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
        return true;
    }

    @Override
    public @NonNull Name name() {
        return NAME;
    }

    @Override
    public <GOAL extends ProofGoal<@NonNull GOAL>> RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, GOAL goal, MutableState mState) {
        return costComputationF.computeCost(app, pos, goal, mState);
    }

    @Override
    protected RuleSetDispatchFeature getCostDispatcher() {
        return costComputationDispatcher;
    }
}
