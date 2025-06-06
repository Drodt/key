/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.proof;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.rule.IBuiltInRuleApp;
import de.uka.ilkd.key.rule.NoPosTacletApp;
import de.uka.ilkd.key.rule.TacletApp;

import org.key_project.prover.proof.rulefilter.AnyRuleSetTacletFilter;
import org.key_project.prover.proof.rulefilter.NotRuleFilter;
import org.key_project.prover.proof.rulefilter.TacletFilter;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.strategy.NewRuleListener;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

/**
 * manages the possible application of rules (RuleApps)
 */
public final class RuleAppIndex {

    private final Goal goal;

    private final TacletIndex tacletIndex;

    /**
     * Two <code>TacletAppIndex</code> objects, one of which only contains rules that have to be
     * applied interactively, and the other one for rules that can also be applied automatic. This
     * is used as an optimization, as only the latter index has to be kept up to date while applying
     * rules automated
     */
    private final TacletAppIndex interactiveTacletAppIndex;
    private final TacletAppIndex automatedTacletAppIndex;

    private final BuiltInRuleAppIndex builtInRuleAppIndex;

    private NewRuleListener ruleListener = null;

    /**
     * The current mode of the index: For <code>autoMode==true</code>, the index
     * <code>interactiveTacletAppIndex</code> is not updated
     */
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

    public RuleAppIndex(TacletIndex p_tacletIndex, BuiltInRuleAppIndex p_builtInRuleAppIndex,
            Goal goal,
            Services services) {
        tacletIndex = p_tacletIndex;
        automatedTacletAppIndex = new TacletAppIndex(tacletIndex, goal, services);
        interactiveTacletAppIndex = new TacletAppIndex(tacletIndex, goal, services);
        this.goal = goal;
        builtInRuleAppIndex = p_builtInRuleAppIndex;
        // default to false to keep compatibility with old code
        autoMode = false;

        automatedTacletAppIndex.setRuleFilter(AnyRuleSetTacletFilter.INSTANCE);
        interactiveTacletAppIndex.setRuleFilter(new NotRuleFilter(AnyRuleSetTacletFilter.INSTANCE));

        setNewRuleListeners();
    }

    private RuleAppIndex(TacletIndex tacletIndex, TacletAppIndex interactiveTacletAppIndex,
            TacletAppIndex automatedTacletAppIndex, BuiltInRuleAppIndex builtInRuleAppIndex,
            Goal goal, boolean autoMode) {
        this.tacletIndex = tacletIndex;
        this.interactiveTacletAppIndex = interactiveTacletAppIndex;
        this.automatedTacletAppIndex = automatedTacletAppIndex;
        this.builtInRuleAppIndex = builtInRuleAppIndex;
        this.autoMode = autoMode;
        this.goal = goal;

        setNewRuleListeners();
    }

    private void setNewRuleListeners() {
        interactiveTacletAppIndex.setNewRuleListener(newRuleListener);
        automatedTacletAppIndex.setNewRuleListener(newRuleListener);
        builtInRuleAppIndex.setNewRuleListener(newRuleListener);
    }

    /**
     * Currently the rule app index can either operate in interactive mode (and contain applications
     * of all existing taclets) or in automatic mode (and only contain a restricted set of taclets
     * that can possibly be applied automated). This distinction could be replaced with a more
     * general way to control the contents of the rule app index
     */
    public void autoModeStarted() {
        autoMode = true;
    }

    public void autoModeStopped() {
        autoMode = false;
    }

    /**
     * returns the Taclet index for this ruleAppIndex.
     */
    public TacletIndex tacletIndex() {
        return tacletIndex;
    }

    /**
     * returns the built-in rule application index for this ruleAppIndex.
     */
    public BuiltInRuleAppIndex builtInRuleAppIndex() {
        return builtInRuleAppIndex;
    }


    /**
     * adds a change listener to the index
     *
     * @param l the AppIndexListener to add
     */
    public void setNewRuleListener(@Nullable NewRuleListener l) {
        ruleListener = l;
    }

    /**
     * returns the set of rule applications for the given heuristics at the given position of the
     * given sequent.
     *
     * @param filter the TacletFiler filtering the taclets of interest
     * @param pos the PosInOccurrence to focus
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     */
    public ImmutableList<TacletApp> getTacletAppAt(TacletFilter filter,
            PosInOccurrence pos,
            Services services) {
        ImmutableList<TacletApp> result = ImmutableSLList.nil();
        if (!autoMode) {
            result =
                result.prepend(interactiveTacletAppIndex.getTacletAppAt(pos, filter, services));
        }
        result = result.prepend(automatedTacletAppIndex.getTacletAppAt(pos, filter, services));
        return result;
    }


    /**
     * returns the rule applications at the given PosInOccurrence and at all Positions below this.
     * The method calls getTacletAppAt for all the Positions below.
     *
     * @param filter the TacletFiler filtering the taclets of interest
     * @param pos the position where to start from
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     * @return the possible rule applications
     */
    public ImmutableList<TacletApp> getTacletAppAtAndBelow(TacletFilter filter,
            PosInOccurrence pos,
            Services services) {
        ImmutableList<TacletApp> result = ImmutableSLList.nil();
        if (!autoMode) {
            result = result.prepend(
                interactiveTacletAppIndex.getTacletAppAtAndBelow(pos, filter, services));
        }
        result =
            result.prepend(automatedTacletAppIndex.getTacletAppAtAndBelow(pos, filter, services));
        return result;
    }


    /**
     * collects all FindTacletInstantiations for the given heuristics and position
     *
     * @param filter the TacletFiler filtering the taclets of interest
     * @param pos the PosInOccurrence to focus
     * @return list of all possible instantiations
     */
    public ImmutableList<NoPosTacletApp> getFindTaclet(TacletFilter filter,
            PosInOccurrence pos) {
        ImmutableList<NoPosTacletApp> result = ImmutableSLList.nil();
        if (!autoMode) {
            result = result.prepend(interactiveTacletAppIndex.getFindTaclet(pos, filter));
        }
        result = result.prepend(automatedTacletAppIndex.getFindTaclet(pos, filter));
        return result;
    }

    /**
     * collects all NoFindTacletInstantiations for the given heuristics
     *
     * @param filter the TacletFiler filtering the taclets of interest
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     * @return list of all possible instantiations
     */
    public ImmutableList<NoPosTacletApp> getNoFindTaclet(TacletFilter filter, Services services) {
        ImmutableList<NoPosTacletApp> result = ImmutableSLList.nil();
        if (!autoMode) {
            result = interactiveTacletAppIndex.getNoFindTaclet(filter, services);
        }
        result = result.prepend(automatedTacletAppIndex.getNoFindTaclet(filter, services));
        return result;
    }


    /**
     * collects all RewriteTacletInstantiations for the given heuristics in a subterm of the
     * constraintformula described by a PosInOccurrence
     *
     * @param filter the TacletFiler filtering the taclets of interest
     * @param pos the PosInOccurrence to focus
     * @return list of all possible instantiations
     */
    public ImmutableList<NoPosTacletApp> getRewriteTaclet(TacletFilter filter,
            PosInOccurrence pos) {
        ImmutableList<NoPosTacletApp> result = ImmutableSLList.nil();
        if (!autoMode) {
            result =
                result.prepend(interactiveTacletAppIndex.getRewriteTaclet(pos, filter));
        }
        result = result.prepend(automatedTacletAppIndex.getRewriteTaclet(pos, filter));

        return result;
    }


    /**
     * returns a list of built-in rule applications applicable for the given goal, user defined
     * constraint and position
     */
    public ImmutableList<IBuiltInRuleApp> getBuiltInRules(Goal g, PosInOccurrence pos) {

        return builtInRuleAppIndex().getBuiltInRule(g, pos);
    }


    /**
     * adds a new Taclet with instantiation information to the Taclet Index of this TacletAppIndex.
     *
     * @param tacletApps the NoPosTacletApp describing a partial instantiated Taclet to add
     */
    public void addNoPosTacletApp(Iterable<NoPosTacletApp> tacletApps) {
        tacletIndex.addTaclets(tacletApps);

        if (autoMode) {
            interactiveTacletAppIndex.clearIndexes();
        }

        interactiveTacletAppIndex.addedNoPosTacletApps(tacletApps);
        automatedTacletAppIndex.addedNoPosTacletApps(tacletApps);
    }

    /**
     * adds a new Taclet with instantiation information to the Taclet Index of this TacletAppIndex.
     *
     * @param tacletApp the NoPosTacletApp describing a partial instantiated Taclet to add
     */
    public void addNoPosTacletApp(NoPosTacletApp tacletApp) {
        tacletIndex.add(tacletApp);

        if (autoMode) {
            interactiveTacletAppIndex.clearIndexes();
        }

        interactiveTacletAppIndex.addedNoPosTacletApp(tacletApp);
        automatedTacletAppIndex.addedNoPosTacletApp(tacletApp);
    }

    /**
     * remove a Taclet with instantiation information from the Taclet Index of this TacletAppIndex.
     *
     * @param tacletApp the NoPosTacletApp to remove
     */
    public void removeNoPosTacletApp(NoPosTacletApp tacletApp) {
        tacletIndex.remove(tacletApp);

        if (autoMode) {
            interactiveTacletAppIndex.clearIndexes();
        }

        interactiveTacletAppIndex.removedNoPosTacletApp(tacletApp);
        automatedTacletAppIndex.removedNoPosTacletApp(tacletApp);
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

    /**
     * Empties all caches
     */
    public void clearAndDetachCache() {
        // Currently this only applies to the taclet index
        interactiveTacletAppIndex.clearAndDetachCache();
        automatedTacletAppIndex.clearAndDetachCache();
    }

    /**
     * Empties all caches
     */
    public void clearIndexes() {
        // Currently this only applies to the taclet index
        interactiveTacletAppIndex.clearIndexes();
        automatedTacletAppIndex.clearIndexes();
    }

    /**
     * Ensures that all caches are fully up-to-date
     */
    public void fillCache() {
        if (!autoMode) {
            interactiveTacletAppIndex.fillCache();
        }
        automatedTacletAppIndex.fillCache();
    }

    /**
     * Report all rule applications that are supposed to be applied automatically, and that are
     * currently stored by the index
     *
     * @param l the NewRuleListener
     * @param services the Services
     */
    public void reportAutomatedRuleApps(NewRuleListener l, Services services) {
        automatedTacletAppIndex.reportRuleApps(l, services);
        builtInRuleAppIndex.reportRuleApps(l, goal);
    }

    /**
     * Report builtin rules to all registered NewRuleListener instances.
     *
     * @param p_goal the Goal which to scan
     */
    public void scanBuiltInRules(Goal p_goal) {
        builtInRuleAppIndex().scanApplicableRules(p_goal, newRuleListener);
    }

    /**
     * informs all observers, if a formula has been added, changed or removed
     */
    private void informNewRuleListener(RuleApp p_app,
            PosInOccurrence p_pos) {
        if (ruleListener != null) {
            ruleListener.ruleAdded(p_app, p_pos);
        }
    }

    /**
     * informs all observers, if a formula has been added, changed or removed
     */
    private void informNewRuleListener(ImmutableList<? extends RuleApp> p_apps,
            PosInOccurrence p_pos) {
        if (ruleListener != null) {
            ruleListener.rulesAdded(p_apps, p_pos);
        }
    }


    /**
     * returns a new RuleAppIndex with a copied TacletIndex. Attention: the listener lists are not
     * copied
     */
    public RuleAppIndex copy(Goal goal) {
        TacletIndex copiedTacletIndex = tacletIndex.copy();
        TacletAppIndex copiedInteractiveTacletAppIndex =
            interactiveTacletAppIndex.copyWith(copiedTacletIndex, goal);
        TacletAppIndex copiedAutomatedTacletAppIndex =
            automatedTacletAppIndex.copyWith(copiedTacletIndex, goal);
        return new RuleAppIndex(copiedTacletIndex, copiedInteractiveTacletAppIndex,
            copiedAutomatedTacletAppIndex, builtInRuleAppIndex().copy(), goal, autoMode);
    }


    public String toString() {
        return "RuleAppIndex with indexing, getting Taclets from" + " TacletAppIndex "
            + interactiveTacletAppIndex + " and " + automatedTacletAppIndex;
    }
}
