/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.feature;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.RuleSet;
import org.key_project.prover.rules.Taclet;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.prover.strategy.costbased.feature.SumFeature;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

/// Feature for relating rule sets with feature terms. Given a taclet application, this feature will
/// iterate over the rule sets that the taclet belongs to, and for each rule set the corresponding
/// feature term (if existing) is evaluated. The result of the feature is the sum of the results of
/// the different rule set features.
public class RuleSetDispatchFeature implements Feature {
    private static final Feature[] NO_FEATURES = new Feature[0];

    private final Map<RuleSet, Feature> rulesetToFeature = new LinkedHashMap<>();

    /// For each taclet the features bound to the rule sets the taclet belongs to, in rule
    /// set order. The bindings in [#rulesetToFeature] only change during strategy
    /// setup, while `computeCost` is called for every rule application candidate —
    /// caching avoids one map lookup per rule set of the taclet on every call. Invalidated
    /// by all mutating methods; concurrent because proofs may run in parallel.
    private final Map<Taclet, Feature[]> featuresByTaclet = new ConcurrentHashMap<>();

    @Override
    public <Goal extends ProofGoal<@NonNull Goal>> RuleAppCost computeCost(RuleApp app,
            PosInOccurrence pos, Goal goal,
            MutableState mState) {
        if (!(app instanceof TacletApp tapp)) {
            return NumberRuleAppCost.getZeroCost();
        }

        RuleAppCost res = NumberRuleAppCost.getZeroCost();
        for (Feature partialF : featuresFor(tapp.taclet())) {
            res = res.add(partialF.computeCost(app, pos, goal, mState));
            if (res instanceof TopRuleAppCost) {
                break;
            }
        }
        return res;
    }

    private Feature[] featuresFor(Taclet taclet) {
        Feature[] features = featuresByTaclet.get(taclet);
        if (features == null) {
            List<Feature> matching = new ArrayList<>();
            ImmutableList<RuleSet> ruleSets = taclet.getRuleSets();
            while (!ruleSets.isEmpty()) {
                final Feature partialF = rulesetToFeature.get(ruleSets.head());
                if (partialF != null) {
                    matching.add(partialF);
                }
                ruleSets = ruleSets.tail();
            }
            features = matching.isEmpty() ? NO_FEATURES : matching.toArray(new Feature[0]);
            featuresByTaclet.put(taclet, features);
        }
        return features;
    }

    /// Bind feature <code>f</code> to the rule set <code>ruleSet</code>. If this method is called
    /// more than once for the same rule set, the given features are added to each other.
    public void add(RuleSet ruleSet, Feature f) {
        Feature combinedF = rulesetToFeature.get(ruleSet);
        if (combinedF == null) {
            combinedF = f;
        } else {
            combinedF = SumFeature.createSum(combinedF, f);
        }

        rulesetToFeature.put(ruleSet, combinedF);
        featuresByTaclet.clear();
    }

    /// Remove all features that have been related to <code>ruleSet</code>.
    public void clear(RuleSet ruleSet) {
        rulesetToFeature.remove(ruleSet);
        featuresByTaclet.clear();
    }

    /// Returns the used [Feature] for the given [RuleSet].
    ///
    /// @param ruleSet The [RuleSet] to get its [Feature].
    /// @return The [Feature] used for the given [RuleSet] or `null` if not
    /// available.
    public Feature get(RuleSet ruleSet) {
        return rulesetToFeature.get(ruleSet);
    }

    public Set<RuleSet> ruleSets() {
        return rulesetToFeature.keySet();
    }

    /// Returns the used [Feature] for the given [RuleSet] and removes it.
    ///
    /// @param ruleSet The [RuleSet] to get its [Feature].
    /// @return The [Feature] used for the given [RuleSet] or `null` if not
    /// available.
    public Feature remove(RuleSet ruleSet) {
        featuresByTaclet.remove(ruleSet);
        return rulesetToFeature.remove(ruleSet);
    }
}
