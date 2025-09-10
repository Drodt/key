/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.init;


import org.key_project.logic.Name;
import org.key_project.prover.engine.GoalChooserFactory;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.proof.io.RuleSourceFactory;
import org.key_project.rusty.proof.mgt.AxiomJustification;
import org.key_project.rusty.proof.mgt.ComplexRuleJustificationBySpec;
import org.key_project.rusty.proof.mgt.RuleJustification;
import org.key_project.rusty.prover.impl.DefaultGoalChooserFactory;
import org.key_project.rusty.rule.BuiltInRule;
import org.key_project.rusty.rule.Rule;
import org.key_project.rusty.rule.Taclet;
import org.key_project.rusty.rule.UseOperationContractRule;
import org.key_project.rusty.strategy.ModularRustyDLStrategyFactory;
import org.key_project.rusty.strategy.StrategyFactory;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

public class RustProfile implements Profile {
    public static final String NAME = "Rust Profile";

    private static RustProfile defaultInstance;

    public static final StrategyFactory DEFAULT = new ModularRustyDLStrategyFactory();

    // maybe move these fields to abstract parent AbstractProfile
    private final RuleCollection standardRules;

    private GoalChooserFactory<@NonNull Proof, @NonNull Goal> prototype;

    protected RustProfile(String standardRuleFilename) {
        standardRules = new RuleCollection(
            RuleSourceFactory.fromDefaultLocation(standardRuleFilename), initBuiltInRules());
        this.prototype = getDefaultGoalChooserBuilder();
    }

    public RustProfile() {
        this("standardRustRules.key");
    }

    public static RustProfile getDefaultInstance() {
        if (defaultInstance == null) {
            defaultInstance = new RustProfile();
        }
        return defaultInstance;
    }

    @Override
    public RuleCollection getStandardRules() {
        return standardRules;
    }

    @Override
    public String name() {
        return NAME;
    }

    protected ImmutableList<BuiltInRule> initBuiltInRules() {
        return ImmutableSLList.<BuiltInRule>nil().prepend(UseOperationContractRule.INSTANCE);
    }

    @Override
    public RuleJustification getJustification(Rule r) {
        if (r == UseOperationContractRule.INSTANCE)
            return new ComplexRuleJustificationBySpec();
        if (r instanceof Taclet t)
            return t.getRuleJustification();
        else
            return AxiomJustification.INSTANCE;
    }

    @Override
    public StrategyFactory getDefaultStrategyFactory() {
        return DEFAULT;
    }

    protected ImmutableSet<StrategyFactory> getStrategyFactories() {
        return ImmutableSet.singleton(DEFAULT);
    }

    @Override
    public boolean supportsStrategyFactory(Name strategy) {
        return getStrategyFactory(strategy) != null;
    }

    @Override
    public StrategyFactory getStrategyFactory(Name n) {
        for (StrategyFactory sf : getStrategyFactories()) {
            if (sf.name().equals(n)) {
                return sf;
            }
        }
        return null;
    }

    /// returns the default builder for a goal chooser
    ///
    /// @return this implementation returns a new instance of [DefaultGoalChooserFactory]
    @Override
    public GoalChooserFactory<Proof, @NonNull Goal> getDefaultGoalChooserBuilder() {
        return new DefaultGoalChooserFactory();
    }

    /// returns a copy of the selected goal chooser builder
    @Override
    public GoalChooserFactory<@NonNull Proof, @NonNull Goal> getSelectedGoalChooserBuilder() {
        return prototype.copy();
    }
}
