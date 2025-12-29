/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.strategy.NewRuleListener;
import org.key_project.rusty.Services;
import org.key_project.rusty.rule.IBuiltInRuleApp;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

public class RuleAppIndex {
    private final Goal goal;

    private final TacletIndex tacletIndex;

    /// Two <code>TacletAppIndex</code> objects, one of which only contains rules that have to be
    /// applied interactively, and the other one for rules that can also be applied automatic. This
    /// is used as an optimization, as only the latter index has to be kept up to date while
    /// applying
    /// rules automated
    private final TacletAppIndex interactiveTacletAppIndex;
    private final TacletAppIndex automatedTacletAppIndex;

    private final BuiltInRuleAppIndex builtInRuleAppIndex;

    private NewRuleListener ruleListener = null;

    /// The current mode of the index: For <code>autoMode==true</code>, the index
    /// <code>interactiveTacletAppIndex</code> is not updated
    private boolean autoMode;

    private final NewRuleListener newRuleListener = new NewRuleListener() {
        public void ruleAdded(RuleApp taclet, PosInOccurrence pos) {
            informNewRuleListener(taclet, pos);
        }

        @Override
        public void rulesAdded(ImmutableList<? extends RuleApp> rules,
                PosInOccurrence pos) {
            informNewRuleListener(rules, pos);
        }
    };

    public RuleAppIndex(TacletIndex tacletIndex, BuiltInRuleAppIndex builtInRuleAppIndex, Goal goal,
            Services services) {
        this.goal = goal;
        this.tacletIndex = tacletIndex;
        this.builtInRuleAppIndex = builtInRuleAppIndex;
        interactiveTacletAppIndex = new TacletAppIndex(tacletIndex, goal, services);
        automatedTacletAppIndex = new TacletAppIndex(tacletIndex, goal, services);

        setNewRuleListeners();
    }

    private RuleAppIndex(TacletIndex tacletIndex, BuiltInRuleAppIndex builtInRuleAppIndex,
            Goal goal, TacletAppIndex interactiveTacletAppIndex,
            TacletAppIndex automatedTacletAppIndex) {
        this.goal = goal;
        this.tacletIndex = tacletIndex;
        this.interactiveTacletAppIndex = interactiveTacletAppIndex;
        this.automatedTacletAppIndex = automatedTacletAppIndex;
        this.builtInRuleAppIndex = builtInRuleAppIndex;

        setNewRuleListeners();
    }

    /// returns the set of rule applications for the given heuristics at the given position of the
    /// given sequent.
    /// //@param filter the TacletFiler filtering the taclets of interest
    ///
    /// @param pos the PosInOccurrence to focus
    /// @param services the Services object encapsulating information about the Rust datastructures
    /// like (static)types etc.
    public ImmutableList<TacletApp> getTacletAppAt(PosInOccurrence pos,
            Services services) {
        ImmutableList<TacletApp> result = ImmutableSLList.nil();
        result = result.prepend(interactiveTacletAppIndex.getTacletAppAt(pos, services));
        return result;
    }

    /// returns a new RuleAppIndex with a copied TacletIndex. Attention: the listener lists are not
    /// copied
    public RuleAppIndex copy(Goal goal) {
        TacletIndex copiedTacletIndex = tacletIndex.copy();
        return new RuleAppIndex(copiedTacletIndex, builtInRuleAppIndex().copy(), goal,
            interactiveTacletAppIndex.copyWith(copiedTacletIndex, goal),
            automatedTacletAppIndex.copyWith(copiedTacletIndex, goal));
    }


    /// adds a new Taclet with instantiation information to the Taclet Index of this TacletAppIndex.
    ///
    /// @param tacletApp the NoPosTacletApp describing a partial instantiated Taclet to add
    public void addNoPosTacletApp(NoPosTacletApp tacletApp) {
        tacletIndex.add(tacletApp);

        interactiveTacletAppIndex.addedNoPosTacletApp(tacletApp);
    }

    /**
     * called if a formula has been replaced
     *
     * @param sci SequentChangeInfo describing the change of the sequent
     */
    public void sequentChanged(SequentChangeInfo sci) {
        if (!autoMode) {
            interactiveTacletAppIndex.sequentChanged(sci);
        }
        automatedTacletAppIndex.sequentChanged(sci);
        builtInRuleAppIndex.sequentChanged(goal, sci, newRuleListener);
    }

    public TacletIndex tacletIndex() {
        return tacletIndex;
    }

    /// returns the rule applications at the given PosInOccurrence and at all Positions below this.
    /// The method calls getTacletAppAt for all the Positions below.
    ///
    /// @param pos the position where to start from
    /// @param services the Services object encapsulating information about the java datastructures
    /// like (static)types etc.
    /// @return the possible rule applications
    public ImmutableList<TacletApp> getTacletAppAtAndBelow(PosInOccurrence pos,
            Services services) {
        ImmutableList<TacletApp> result = ImmutableSLList.nil();
        result =
            result.prepend(interactiveTacletAppIndex.getTacletAppAtAndBelow(pos, services));
        return result;
    }

    /// returns the built-in rule application index for this ruleAppIndex.
    public BuiltInRuleAppIndex builtInRuleAppIndex() {
        return builtInRuleAppIndex;
    }

    /// returns a list of built-in rule applications applicable for the given goal, user defined
    /// constraint and position
    public ImmutableList<IBuiltInRuleApp> getBuiltInRules(Goal g, PosInOccurrence pos) {
        return builtInRuleAppIndex().getBuiltInRule(g, pos);
    }

    public void scanBuiltInRules(Goal goal) {
        builtInRuleAppIndex().scanApplicableRules(goal, newRuleListener);
    }

    /// Report all rule applications that are supposed to be applied automatically, and that are
    /// currently stored by the index
    ///
    /// @param l the NewRuleListener
    /// @param services the Services
    public void reportAutomatedRuleApps(NewRuleListener l, Services services) {
        automatedTacletAppIndex.reportRuleApps(l, services);
        builtInRuleAppIndex.reportRuleApps(l, goal);
    }

    /// Ensures that all caches are fully up-to-date
    public void fillCache() {
        if (!autoMode) {
            interactiveTacletAppIndex.fillCache();
        }
        automatedTacletAppIndex.fillCache();
    }

    /// Currently the rule app index can either operate in interactive mode (and contain
    /// applications
    /// of all existing taclets) or in automatic mode (and only contain a restricted set of taclets
    /// that can possibly be applied automated). This distinction could be replaced with a more
    /// general way to control the contents of the rule app index
    public void autoModeStarted() {
        autoMode = true;
    }

    public void autoModeStopped() {
        autoMode = false;
    }

    /// informs all observers, if a formula has been added, changed or removed
    private void informNewRuleListener(RuleApp p_app,
            PosInOccurrence p_pos) {
        if (ruleListener != null) {
            ruleListener.ruleAdded(p_app, p_pos);
        }
    }

    /// informs all observers, if a formula has been added, changed or removed
    private void informNewRuleListener(ImmutableList<? extends RuleApp> p_apps,
            PosInOccurrence p_pos) {
        if (ruleListener != null) {
            ruleListener.rulesAdded(p_apps, p_pos);
        }
    }

    /**
     * adds a change listener to the index
     *
     * @param l the AppIndexListener to add
     */
    public void setNewRuleListener(@Nullable NewRuleListener l) {
        ruleListener = l;
    }

    private void setNewRuleListeners() {
        interactiveTacletAppIndex.setNewRuleListener(newRuleListener);
        automatedTacletAppIndex.setNewRuleListener(newRuleListener);
        builtInRuleAppIndex.setNewRuleListener(newRuleListener);
    }
}
