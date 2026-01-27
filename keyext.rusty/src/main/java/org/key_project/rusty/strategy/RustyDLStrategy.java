/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

import org.key_project.logic.Name;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.rule.BuiltInRule;
import org.key_project.rusty.strategy.ComponentStrategy.StrategyAspect;
import org.key_project.rusty.strategy.feature.CountBranchFeature;
import org.key_project.rusty.strategy.feature.EqNonDuplicateAppFeature;
import org.key_project.rusty.strategy.feature.NonDuplicateAppFeature;
import org.key_project.rusty.strategy.feature.RuleSetDispatchFeature;
import org.key_project.rusty.strategy.termfeature.IsInductionVariable;

import org.jspecify.annotations.NonNull;

import static org.key_project.prover.strategy.costbased.feature.CompareCostsFeature.leq;

/// Strategy tailored to be used as long as a Rust program can be found in the sequent.
public final class RustyDLStrategy extends AbstractFeatureStrategy implements ComponentStrategy {
    public static final AtomicLong PERF_COMPUTE = new AtomicLong();
    public static final AtomicLong PERF_APPROVE = new AtomicLong();
    public static final AtomicLong PERF_INSTANTIATE = new AtomicLong();

    public static final String RUSTY_DL_STRATEGY = "RustyDLStrategy";

    private final StrategyProperties strategyProperties;

    private final RuleSetDispatchFeature costComputationDispatcher;
    private final Feature costComputationF;
    private final RuleSetDispatchFeature approvalDispatcher;
    private final Feature approvalF;
    private final RuleSetDispatchFeature instantiationDispatcher;
    private final Feature instantiationF;

    private final ArithTermFeatures tf;
    private final FormulaTermFeatures ff;


    public RustyDLStrategy(Proof proof, StrategyProperties strategyProperties) {
        super(proof);

        this.strategyProperties = (StrategyProperties) strategyProperties.clone();

        this.tf = new ArithTermFeatures(getServices().getLDTs().getIntLDT());
        this.ff = new FormulaTermFeatures(this.tf);

        costComputationDispatcher = setupCostComputationF();
        approvalDispatcher = setupApprovalDispatcher();
        instantiationDispatcher = setupInstantiationF();

        costComputationF = setupGlobalF(costComputationDispatcher);
        instantiationF = setupGlobalF(instantiationDispatcher);
        approvalF = add(setupApprovalF(), approvalDispatcher);
    }

    @Override
    public boolean isResponsibleFor(RuleSet rs) {
        return costComputationDispatcher.get(rs) != null || instantiationDispatcher.get(rs) != null
                || approvalDispatcher.get(rs) != null;
    }

    @Override
    public @NonNull Name name() {
        return new Name(RUSTY_DL_STRATEGY);
    }

    /// Evaluate the cost of a <code>RuleApp</code>.
    ///
    /// @param app rule application
    /// @param pio corresponding [PosInOccurrence]
    /// @param goal corresponding goal
    /// @param mState the [MutableState] to query for information like current value of
    /// [TermBuffer]s or
    /// [ChoicePoint]s
    /// @return the cost of the rule application expressed as a <code>RuleAppCost</code> object.
    /// <code>TopRuleAppCost.INSTANCE</code> indicates that the rule shall not be applied at
    /// all (it is discarded by the strategy).
    @Override
    public <GOAL extends ProofGoal<@NonNull GOAL>> RuleAppCost computeCost(@NonNull RuleApp app,
            @NonNull PosInOccurrence pio,
            @NonNull GOAL goal,
            @NonNull MutableState mState) {
        var time = System.nanoTime();
        try {
            return costComputationF.computeCost(app, pio, goal, mState);
        } finally {
            PERF_COMPUTE.addAndGet(System.nanoTime() - time);
        }
    }

    /// Re-Evaluate a <code>RuleApp</code>. This method is called immediately before a rule is
    /// really
    /// applied
    ///
    /// @param app the rule application
    /// @param pio the position in occurrence
    /// @param goal the goal
    /// @return true iff the rule should be applied, false otherwise
    @Override
    public final boolean isApprovedApp(RuleApp app,
            PosInOccurrence pio, Goal goal) {
        var time = System.nanoTime();
        try {
            return !(approvalF.computeCost(app, pio, goal,
                new MutableState()) == TopRuleAppCost.INSTANCE);
        } finally {
            PERF_APPROVE.addAndGet(System.nanoTime() - time);
        }
    }

    @Override
    public RuleAppCost instantiateApp(RuleApp app,
            PosInOccurrence pio, Goal goal,
            MutableState mState) {
        var time = System.nanoTime();
        try {
            return instantiationF.computeCost(app, pio, goal, mState);
        } finally {
            PERF_INSTANTIATE.addAndGet(System.nanoTime() - time);
        }
    }

    @Override
    public boolean isStopAtFirstNonCloseableGoal() {
        return strategyProperties.getProperty(StrategyProperties.STOPMODE_OPTIONS_KEY)
                .equals(StrategyProperties.STOPMODE_NONCLOSE);
    }

    private RuleSetDispatchFeature getCostComputationDispatcher() {
        return costComputationDispatcher;
    }

    private RuleSetDispatchFeature getInstantiationDispatcher() {
        return instantiationDispatcher;
    }

    private RuleSetDispatchFeature setupCostComputationF() {
        final var d = new RuleSetDispatchFeature();

        bindRuleSet(d, "semantics_blasting", inftyConst());

        bindRuleSet(d, "rustIntegerSemantics",
            ifZero(sequentContainsNoPrograms(), longConst(-5000), ifZero(
                leq(CountBranchFeature.INSTANCE, longConst(1)), longConst(-5000), inftyConst())));

        bindRuleSet(d, "simplify_literals",
            // ifZero ( ConstraintStrengthenFeatureUC.create(proof),
            // longConst ( 0 ),
            longConst(-8000));

        bindRuleSet(d, "nonDuplicateAppCheckEq", EqNonDuplicateAppFeature.INSTANCE);

        setupUserTaclets(d);

        // chrisg: The following rule, if active, must be applied delta rules.
        if (autoInductionEnabled()) {
            bindRuleSet(d, "auto_induction", -6500); // chrisg
        } else {
            bindRuleSet(d, "auto_induction", inftyConst()); // chrisg
        }

        // chrisg: The following rule is a beta rule that, if active, must have
        // a higher priority than other beta rules.
        if (autoInductionLemmaEnabled()) {
            bindRuleSet(d, "auto_induction_lemma", -300);
        } else {
            bindRuleSet(d, "auto_induction_lemma", inftyConst());
        }

        if (strategyProperties.contains(StrategyProperties.AUTO_INDUCTION_ON)
                || strategyProperties.contains(StrategyProperties.AUTO_INDUCTION_LEMMA_ON)) {
            bindRuleSet(d, "induction_var", 0);
        } else if (!autoInductionEnabled() && !autoInductionLemmaEnabled()) {
            bindRuleSet(d, "induction_var", inftyConst());
        } else {
            bindRuleSet(d, "induction_var", ifZero(
                applyTF(instOf("uSub"), IsInductionVariable.INSTANCE), longConst(0), inftyConst()));
        }

        return d;
    }

    private RuleSetDispatchFeature setupApprovalDispatcher() {
        var d = new RuleSetDispatchFeature();
        // TODO
        return d;
    }

    private void setupUserTaclets(RuleSetDispatchFeature d) {
        for (int i = 1; i <= StrategyProperties.USER_TACLETS_NUM; ++i) {
            final String userTacletsProbs =
                strategyProperties.getProperty(StrategyProperties.userTacletsOptionsKey(i));
            if (StrategyProperties.USER_TACLETS_LOW.equals(userTacletsProbs)) {
                bindRuleSet(d, "userTaclets" + i, 10000);
            } else if (StrategyProperties.USER_TACLETS_HIGH.equals(userTacletsProbs)) {
                bindRuleSet(d, "userTaclets" + i, -50);
            } else {
                bindRuleSet(d, "userTaclets" + i, inftyConst());
            }
        }
    }

    private boolean autoInductionEnabled() { // chrisg
        // Negated!
        return !StrategyProperties.AUTO_INDUCTION_OFF.equals(
            strategyProperties.getProperty(StrategyProperties.AUTO_INDUCTION_OPTIONS_KEY));
    }

    private boolean autoInductionLemmaEnabled() { // chrisg
        final String prop =
            strategyProperties.getProperty(StrategyProperties.AUTO_INDUCTION_OPTIONS_KEY);
        return prop.equals(StrategyProperties.AUTO_INDUCTION_LEMMA_ON)
                || prop.equals(StrategyProperties.AUTO_INDUCTION_RESTRICTED);
    }

    private RuleSetDispatchFeature setupInstantiationF() {
        enableInstantiate();

        final RuleSetDispatchFeature d = new RuleSetDispatchFeature();

        disableInstantiate();
        return d;
    }

    private Feature setupGlobalF(@NonNull Feature dispatcher) {
        return dispatcher;
    }

    private Feature setupApprovalF() {
        return NonDuplicateAppFeature.INSTANCE;
    }

    @Override
    public Set<RuleSet> getResponsibilities(StrategyAspect aspect) {
        var set = new HashSet<RuleSet>();
        set.addAll(getDispatcher(aspect).ruleSets());
        return set;
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
    public boolean isResponsibleFor(BuiltInRule rule) {
        return false;
    }
}
