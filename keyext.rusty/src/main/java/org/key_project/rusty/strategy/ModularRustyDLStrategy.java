/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.key_project.logic.Name;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.Rule;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.ArithTermFeatures;
import org.key_project.prover.strategy.ComponentStrategy;
import org.key_project.prover.strategy.ComponentStrategy.StrategyAspect;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.AutomatedRuleFeature;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.MatchedAssumesFeature;
import org.key_project.prover.strategy.costbased.feature.RuleSetDispatchFeature;
import org.key_project.prover.strategy.costbased.termProjection.FocusProjection;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.rule.BuiltInRule;
import org.key_project.rusty.strategy.feature.NonDuplicateAppFeature;

import org.jspecify.annotations.NonNull;

public class ModularRustyDLStrategy extends RustAbstractFeatureStrategy {
    public static final Name NAME = new Name("Modular RustyDL Strategy");

    private final List<ComponentStrategy<Goal>> strategies = new ArrayList<>();
    private final StrategyProperties strategyProperties;
    private final Feature reduceCostTillMaxF;
    private final Feature reduceInstTillMaxF;
    private final ArithTermFeatures tf;
    private final RuleSetDispatchFeature conflictCostDispatcher;
    private final Feature totalCost;
    /// the feature evaluated by [#instantiateApp]; built once, see constructor
    private final Feature totalInstCost;

    // map rulesets to the strategies that participate in their cost computations, instantiation or
    // approval decisions
    private final Map<RuleSet, List<ComponentStrategy<Goal>>> costResponsibilityMap =
        new LinkedHashMap<>();
    private final Map<RuleSet, List<ComponentStrategy<Goal>>> instantiationResponsibilityMap =
        new LinkedHashMap<>();
    private final Map<RuleSet, List<ComponentStrategy<Goal>>> approvalResponsibilityMap =
        new LinkedHashMap<>();

    // map rules to the strategies that participate in their cost computations, instantiation or
    // approval decisions
    private final Map<Rule, LinkedHashSet<ComponentStrategy<Goal>>> costRuleToStrategyMap =
        new LinkedHashMap<>();
    private final Map<Rule, LinkedHashSet<ComponentStrategy<Goal>>> instantiationRuleToStrategyMap =
        new LinkedHashMap<>();
    private final Map<Rule, LinkedHashSet<ComponentStrategy<Goal>>> approvalRuleToStrategyMap =
        new LinkedHashMap<>();
    private final Map<Name, ComponentStrategy<Goal>> nameToStrategyMap = new HashMap<>();

    public ModularRustyDLStrategy(Proof proof, List<ComponentStrategy<Goal>> componentStrategies,
            StrategyProperties properties) {
        super(proof);
        strategies.addAll(componentStrategies);
        this.strategyProperties = (StrategyProperties) properties.clone();
        this.tf = new ArithTermFeatures(getServices().getLDTs().getIntLDT(), Equality.EQUALS);

        initializeResponsibilityMap(StrategyAspect.Cost, costResponsibilityMap);
        initializeResponsibilityMap(StrategyAspect.Instantiation, instantiationResponsibilityMap);
        initializeResponsibilityMap(StrategyAspect.Approval, approvalResponsibilityMap);

        // if more than one strategy is responsible for a _ruleset_ we need to determine how to
        // resolve the
        // competing computations
        conflictCostDispatcher = resolveConflicts();

        final Feature ifMatchedF = ifZero(MatchedAssumesFeature.create(), longConst(+1));
        reduceCostTillMaxF = new ReduceTillMaxFeature(Feature::computeCost,
            (rule) -> getResponsibleStrategies(rule, costResponsibilityMap, costRuleToStrategyMap));

        reduceInstTillMaxF = new ReduceTillMaxFeature(ComponentStrategy::instantiateApp,
            (rule) -> getResponsibleStrategies(rule, instantiationResponsibilityMap,
                instantiationRuleToStrategyMap));

        // the feature for the cost computation
        totalCost =
            add(AutomatedRuleFeature.getInstance(), ifMatchedF, NonDuplicateAppFeature.INSTANCE,
                reduceCostTillMaxF, conflictCostDispatcher);

        // The feature for instantiateApp, built once instead of on every call.
        // Note that no conflict dispatcher takes part in this sum: resolveConflicts()
        // *moves* the conflicting bindings out of the component dispatchers (see
        // RuleSetDispatchFeature#remove), so any dispatcher built after the one above
        // would be empty and contribute constant zero anyway.
        enableInstantiate();
        totalInstCost =
            add(AutomatedRuleFeature.getInstance(), ifMatchedF, NonDuplicateAppFeature.INSTANCE,
                reduceInstTillMaxF);
        disableInstantiate();
    }

    /// Caches the information which strategies are responsible for which ruleset
    ///
    /// @param aspect the StrategyAspect for which the cache is created
    /// @param responsibilityMap the map used as cache for the specified aspect
    private void initializeResponsibilityMap(StrategyAspect aspect,
            Map<RuleSet, List<ComponentStrategy<Goal>>> responsibilityMap) {
        for (ComponentStrategy<Goal> strategy : strategies) {
            nameToStrategyMap.put(strategy.name(), strategy);
            var res = strategy.getResponsibilities(aspect);
            for (var rs : res) {
                responsibilityMap.computeIfAbsent(rs, k -> new ArrayList<>()).add(strategy);
            }
        }
    }

    /// The [RuleSetDispatchFeature]s that contribute to a rule's COST (the conflict-resolution
    /// dispatcher plus each component strategy's cost dispatcher). Exposed for [CostReuse]'s
    /// feature-locality classification, which must NOT reach this via reflection from the strategy
    /// object (that would traverse the live proof graph the strategy references).
    public List<RuleSetDispatchFeature> costRuleSetDispatchers() {
        final List<RuleSetDispatchFeature> result = new ArrayList<>();
        result.add(conflictCostDispatcher);
        for (ComponentStrategy s : strategies) {
            final RuleSetDispatchFeature d = s.getDispatcher(StrategyAspect.Cost);
            if (d != null) {
                result.add(d);
            }
        }
        return result;
    }

    private record StratAndDispatcher(ComponentStrategy strategy,
            RuleSetDispatchFeature dispatcher) {
    }

    /// checks for conflicts and resolves known ones (in case an unknown conflict is encountered the
    /// method fails
    /// with a runtime exception
    ///
    /// @return the feature implementing the conflict resolution
    private RuleSetDispatchFeature resolveConflicts() {
        var dis = new RuleSetDispatchFeature();
        var dispatchers =
            strategies.stream()
                    .map(s -> new StratAndDispatcher(s, s.getDispatcher(StrategyAspect.Cost)))
                    .toList();
        var map = new HashMap<RuleSet, List<Name>>();
        for (var d : dispatchers) {
            var s = d.strategy;
            for (var rs : d.dispatcher.ruleSets()) {
                var lst = map.computeIfAbsent(rs, r -> new ArrayList<>());
                lst.add(s.name());
            }
        }
        for (var e : map.entrySet()) {
            if (e.getValue().size() > 1) {
                resolveConflict(dis, e.getKey());
            }
        }
        return dis;
    }

    private void resolveConflict(RuleSetDispatchFeature d, RuleSet rs) {
        switch (rs.name().toString()) {
            case "order_terms" -> {
                var folStrat = nameToStrategyMap.get(RFOLStrategy.NAME);
                var intStrat = nameToStrategyMap.get(RustIntegerStrategy.NAME);
                bindRuleSet(d, "order_terms",
                    ifZero(applyTF("commEqLeft", tf.intF),
                        intStrat.getDispatcher(StrategyAspect.Cost).remove(rs),
                        folStrat.getDispatcher(StrategyAspect.Cost).remove(rs)));
            }
            case "apply_equations" -> {
                var folStrat = nameToStrategyMap.get(RFOLStrategy.NAME);
                var intStrat = nameToStrategyMap.get(RustIntegerStrategy.NAME);
                bindRuleSet(d, "apply_equations",
                    ifZero(applyTF(FocusProjection.create(0), tf.intF),
                        intStrat.getDispatcher(StrategyAspect.Cost).remove(rs),
                        folStrat.getDispatcher(StrategyAspect.Cost).remove(rs)));
            }
            case "apply_equations_andOr" -> {
                var folStrat = nameToStrategyMap.get(RFOLStrategy.NAME);
                var intStrat = nameToStrategyMap.get(RustIntegerStrategy.NAME);
                if (quantifierInstantiatedEnabled()) {
                    bindRuleSet(d, "apply_equations_andOr",
                        ifZero(applyTF(FocusProjection.create(0), tf.intF),
                            intStrat.getDispatcher(StrategyAspect.Cost).remove(rs),
                            folStrat.getDispatcher(StrategyAspect.Cost).remove(rs)));
                } else {
                    bindRuleSet(d, "apply_equations_andOr", inftyConst());
                }
            }
            default -> throw new IllegalArgumentException("No resolution defined for " + rs);
        }
    }

    @Override
    public RuleAppCost instantiateApp(RuleApp app, PosInOccurrence pio, Goal goal,
            MutableState mState) {
        return totalInstCost.computeCost(app, pio, goal, mState);
    }

    @Override
    public boolean isStopAtFirstNonCloseableGoal() {
        return strategyProperties.getProperty(StrategyProperties.STOPMODE_OPTIONS_KEY)
                .equals(StrategyProperties.STOPMODE_NONCLOSE);
    }

    @Override
    public boolean isApprovedApp(RuleApp app, PosInOccurrence pio, Goal goal) {
        boolean isApproved =
            NonDuplicateAppFeature.INSTANCE.computeCost(app, pio, goal,
                new MutableState()) != TopRuleAppCost.INSTANCE;
        if (!isApproved) {
            return false;
        }
        return reduceTillMax(isApproved, false, Boolean::logicalAnd,
            s -> s.isApprovedApp(app, pio, goal), getResponsibleStrategies(app.rule(),
                approvalResponsibilityMap, approvalRuleToStrategyMap));
    }

    @Override
    public @NonNull Name name() {
        return NAME;
    }

    private <R> R reduceTillMax(R init, R max, BiFunction<R, R, R> accumulator,
            Function<ComponentStrategy<Goal>, R> mapper,
            LinkedHashSet<ComponentStrategy<Goal>> strats) {
        for (ComponentStrategy<Goal> strategy : strats) {
            init = accumulator.apply(init, mapper.apply(strategy));
            if (init == max) {
                break;
            }
        }
        return init;
    }

    private LinkedHashSet<ComponentStrategy<Goal>> getResponsibleStrategies(Rule rule,
            Map<RuleSet, List<ComponentStrategy<Goal>>> responsibilityMap,
            Map<Rule, LinkedHashSet<ComponentStrategy<Goal>>> ruleToStrategyMap) {
        LinkedHashSet<ComponentStrategy<Goal>> strats = ruleToStrategyMap.get(rule);
        if (strats == null) {
            strats = new LinkedHashSet<>();
            if (rule instanceof BuiltInRule bir) {
                for (var cs : strategies) {
                    if (cs.isResponsibleFor(bir)) {
                        strats.add(cs);
                    }
                }
            } else {
                var ruleSets = rule.ruleSets();
                while (ruleSets.hasNext()) {
                    var rs = ruleSets.next();
                    List<ComponentStrategy<Goal>> s = responsibilityMap.get(rs);
                    if (s != null)
                        strats.addAll(s);
                }
            }
            ruleToStrategyMap.put(rule, strats);
        }
        return strats;
    }

    @Override
    public RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
        return totalCost.computeCost(app, pos, goal, mState);
    }

    @FunctionalInterface
    private interface StrategyCostFunction {
        RuleAppCost compute(ComponentStrategy<Goal> strategy, RuleApp app,
                PosInOccurrence pos, Goal goal, MutableState mState);
    }

    private class ReduceTillMaxFeature implements Feature {
        private final StrategyCostFunction mapper;
        private final Function<Rule, LinkedHashSet<ComponentStrategy<Goal>>> ruleToStrategy;

        ReduceTillMaxFeature(StrategyCostFunction mapper,
                Function<Rule, LinkedHashSet<ComponentStrategy<Goal>>> ruleToStrategy) {
            this.mapper = mapper;
            this.ruleToStrategy = ruleToStrategy;
        }

        @Override
        public RuleAppCost computeCost(RuleApp app,
                PosInOccurrence pos, ProofGoal<?> goal, MutableState mState) {
            return reduceTillMax(NumberRuleAppCost.getZeroCost(), TopRuleAppCost.INSTANCE,
                RuleAppCost::add, s -> mapper.compute(s, app, pos, (Goal) goal, mState),
                ruleToStrategy.apply(app.rule()));
        }
    }

    private boolean quantifierInstantiatedEnabled() {
        return !StrategyProperties.QUANTIFIERS_NONE
                .equals(strategyProperties.getProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY));
    }
}
