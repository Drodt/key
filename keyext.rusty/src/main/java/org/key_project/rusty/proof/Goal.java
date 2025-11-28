/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.key_project.logic.op.Function;
import org.key_project.prover.indexing.FormulaTagManager;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleAbortException;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.prover.strategy.RuleApplicationManager;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.logic.op.ProgramVariable;
import org.key_project.rusty.proof.proofevent.NodeChangeJournal;
import org.key_project.rusty.proof.proofevent.RuleAppInfo;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.rusty.rule.Taclet;
import org.key_project.rusty.rule.TacletApp;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.rusty.strategy.QueueRuleApplicationManager;
import org.key_project.rusty.strategy.Strategy;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public final class Goal implements ProofGoal<@NonNull Goal> {
    private Node node;
    /// The namespaces local to this goal. This may evolve over time.
    private NamespaceSet localNamespaces;
    /// list of all applied rule applications at this branch
    private ImmutableList<RuleApp> appliedRuleApps = ImmutableSLList.nil();

    private final RuleAppIndex ruleAppIndex;

    /// the strategy object that determines automated application of rules
    private @Nullable Strategy<@NonNull Goal> goalStrategy = null;
    /// This is the object which keeps book about all applicable rules.
    private @Nullable RuleApplicationManager<Goal> ruleAppManager;
    /**
     * goal listeners
     */
    private List<GoalListener> listeners = new ArrayList<>(10);
    /// this object manages the tags for all formulas of the sequent
    private FormulaTagManager tagManager;

    /// creates a new goal referencing the given node
    public Goal(Node n, TacletIndex tacletIndex, BuiltInRuleAppIndex builtInRuleAppIndex,
            Services services) {
        this.node = n;
        this.ruleAppIndex = new RuleAppIndex(tacletIndex, builtInRuleAppIndex, this, services);
        appliedRuleApps = ImmutableSLList.nil();
        localNamespaces =
            node.proof().getServices().getNamespaces().copyWithParent().copyWithParent();
        tagManager = new FormulaTagManager(this);
        setRuleAppManager(new QueueRuleApplicationManager());
    }

    /// copy constructor
    private Goal(Node node, RuleAppIndex ruleAppIndex, ImmutableList<RuleApp> appliedRuleApps,
            @Nullable FormulaTagManager tagManager, RuleApplicationManager<Goal> ruleAppManager,
            NamespaceSet localNamespace) {
        this.node = node;
        this.ruleAppIndex = ruleAppIndex.copy(this);
        this.appliedRuleApps = appliedRuleApps;
        this.localNamespaces = localNamespace;
        this.tagManager = tagManager == null ? new FormulaTagManager(this) : tagManager;
        setRuleAppManager(ruleAppManager);
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        if (this.node.sequent() != node.sequent()) {
            this.node = node;
            tagManager = new FormulaTagManager(this);
        } else {
            this.node = node;
        }
    }

    public void setRuleAppManager(@Nullable RuleApplicationManager<Goal> manager) {
        if (ruleAppManager != null) {
            ruleAppIndex.setNewRuleListener(null);
            ruleAppManager.setGoal(null);
        }

        ruleAppManager = manager;

        if (ruleAppManager != null) {
            ruleAppIndex.setNewRuleListener(ruleAppManager);
            ruleAppManager.setGoal(this);
        }
    }

    /// returns the namespaces for this goal.
    ///
    /// @return an up-to-date non-null namespaces-set.
    public NamespaceSet getLocalNamespaces() {
        return localNamespaces;
    }

    public RuleAppIndex ruleAppIndex() {
        return ruleAppIndex;
    }

    /// puts a RuleApp to the list of the applied rule apps at this goal and stores it in the node
    /// of
    /// the goal
    ///
    /// @param app the applied rule app
    public void addAppliedRuleApp(RuleApp app) {
        // Last app first makes inserting and searching faster
        appliedRuleApps = appliedRuleApps.prepend(app);
        getNode().setAppliedRuleApp(app);
    }

    public Sequent sequent() {
        return getNode().sequent();
    }

    @Override
    public @Nullable ImmutableList<@NonNull Goal> apply(
            @NonNull RuleApp ruleApp) {
        final Proof proof = proof();

        final NodeChangeJournal journal = new NodeChangeJournal(proof, this);
        addGoalListener(journal);
        final Node n = node;

        /*
         * wrap the services object into an overlay such that any addition to local symbols is
         * caught.
         */
        final ImmutableList<Goal> goalList;
        ruleApp.checkApplicability();
        ruleApp.registerSkolemConstants(localNamespaces.functions());
        addAppliedRuleApp(ruleApp);
        try {
            goalList = ruleApp.rule().<Goal>getExecutor().apply(this, ruleApp);
        } catch (RuleAbortException rae) {
            removeLastAppliedRuleApp();
            getNode().setAppliedRuleApp(null);
            return null;
        }

        proof.getServices().saveNameRecorder(n);

        if (goalList.isEmpty()) {
            proof.closeGoal(this);
        } else {
            proof.replace(this, goalList);
            if (ruleApp instanceof TacletApp tacletApp && tacletApp.taclet().closeGoal()) {
                // the first new goal is the one to be closed
                proof.closeGoal(goalList.head());
            }
        }

        adaptNamespacesNewGoals(goalList);
        final RuleAppInfo ruleAppInfo = journal.getRuleAppInfo(ruleApp);
        proof.fireRuleApplied(new ProofEvent(proof, ruleAppInfo, goalList));
        return goalList;
    }

    @Override
    public RuleApplicationManager<@NonNull Goal> getRuleAppManager() {
        return ruleAppManager;
    }

    /// creates n new nodes as children of the referenced node and new n goals that have references
    /// to these new nodes.
    ///
    /// @param n number of goals to create
    /// @return the list of new created goals.
    public ImmutableList<Goal> split(int n) {
        ImmutableList<Goal> goalList = ImmutableSLList.nil();

        final Node parent = node; // has to be stored because the node
        // of this goal will be replaced

        if (n == 1) {
            Node newNode = new Node(parent.proof(), parent.sequent(), parent);

            parent.add(newNode);
            this.setNode(newNode);
            goalList = goalList.prepend(this);
        } else if (n > 1) { // this would also work for n ==1 but the above avoids unnecessary
            // creation of arrays
            Node[] newNode = new Node[n];

            for (int i = 0; i < n; i++) {
                // create new node and add to tree
                newNode[i] = new Node(parent.proof(), parent.sequent(), parent);
            }

            parent.addAll(newNode);

            this.setNode(newNode[0]);
            goalList = goalList.prepend(this);

            for (int i = 1; i < n; i++) {
                goalList = goalList.prepend(clone(newNode[i]));
            }
        }

        return goalList;
    }

    /// PRECONDITION: appliedRuleApps.size () > 0
    public void removeLastAppliedRuleApp() {
        appliedRuleApps = appliedRuleApps.tail();
        // node ().setAppliedRuleApp ( null );
    }

    /// clones the goal (with copy of tacletindex and ruleAppIndex).
    ///
    /// The local symbols are reused. This is taken care of later.
    ///
    /// @param node the new Node to which the goal is attached
    /// @return Object the clone
    public Goal clone(Node node) {
        Goal clone;
        if (node.sequent() != this.node.sequent()) {
            clone = new Goal(node, ruleAppIndex, appliedRuleApps, null, ruleAppManager.copy(),
                localNamespaces);
        } else {
            clone = new Goal(node, ruleAppIndex, appliedRuleApps, getFormulaTagManager().copy(),
                ruleAppManager.copy(),
                localNamespaces);
        }
        clone.listeners = (List<GoalListener>) ((ArrayList<GoalListener>) listeners).clone();
        // clone.automatic = this.automatic;
        return clone;
    }

    public @NonNull Proof proof() {
        return node.proof();
    }

    /// sets the sequent of the node
    ///
    /// @param sci SequentChangeInfo containing the sequent to be set and describing the applied
    /// changes to the sequent of the node currently pointed to by this goal
    public void setSequent(SequentChangeInfo sci) {
        assert sci.getOriginalSequent() == getNode().sequent();
        if (!sci.hasChanged()) {
            assert sci.sequent().equals(sci.getOriginalSequent());
            return;
        }
        getNode().setSequent(sci.sequent());
        // getNode().getNodeInfo().setSequentChangeInfo(sci);
        fireSequentChanged(sci);
    }

    /**
     * informs all goal listeners about a change of the sequent to reduce unnecessary object
     * creation the necessary information is passed to the listener as parameters and not through an
     * event object.
     */
    private void fireSequentChanged(SequentChangeInfo sci) {
        getFormulaTagManager().sequentChanged(sci, getTime());
        ruleAppIndex.sequentChanged(sci);
        for (GoalListener listener : listeners) {
            listener.sequentChanged(this, sci);
        }
    }

    public void setBranchLabel(String name) {
        node.getNodeInfo().setBranchLabel(name);
    }

    /// puts the NoPosTacletApp to the set of TacletApps at the node of the goal and to the current
    /// RuleAppIndex.
    ///
    /// @param app the TacletApp
    public void addNoPosTacletApp(NoPosTacletApp app) {
        getNode().addNoPosTacletApp(app);
        ruleAppIndex.addNoPosTacletApp(app);
    }

    /// creates a new TacletApp and puts it to the set of TacletApps at the node of the goal and to
    /// the current RuleAppIndex.
    ///
    /// @param rule the Taclet of the TacletApp to create
    /// @param insts the given instantiations of the TacletApp to be created
    public void addTaclet(Taclet rule, SVInstantiations insts, boolean isAxiom) {
        NoPosTacletApp tacletApp =
            NoPosTacletApp.createFixedNoPosTacletApp(rule, insts, proof().getServices());
        if (tacletApp != null) {
            addNoPosTacletApp(tacletApp);
            /*
             * if (proof().getInitConfig() != null) { // do not break everything
             * // because of ProofMgt
             * proof().getInitConfig().registerRuleIntroducedAtNode(tacletApp,
             * node.parent() != null ? node.parent() : node, isAxiom);
             * }
             */
        }
    }

    public TacletIndex indexOfTaclets() {
        return ruleAppIndex.tacletIndex();
    }

    public Services getOverlayServices() {
        return proof().getServices().getOverlay(getLocalNamespaces());
    }

    /*
     * when the new goals are created during splitting, their namespaces cannot be fixed yet as new
     * symbols may still be added.
     *
     * Now, remember the freshly created symbols in the nodes and set fresh local namespaces.
     *
     * The
     */
    private void adaptNamespacesNewGoals(final ImmutableList<Goal> goalList) {
        Collection<ProgramVariable> newProgVars = localNamespaces.programVariables().elements();
        Collection<Function> newFunctions = localNamespaces.functions().elements();

        localNamespaces.flushToParent();

        boolean first = true;
        for (Goal goal : goalList) {
            goal.getNode().addLocalProgVars(newProgVars);
            goal.getNode().addLocalFunctions(newFunctions);

            if (first) {
                first = false;
            } else {
                goal.localNamespaces = localNamespaces.getParent().copy().copyWithParent();
            }

        }
    }

    @Override
    public String toString() {
        return node.sequent().toString();
    }

    public void addProgramVariable(ProgramVariable pv) {
        localNamespaces.programVariables().addSafely(pv);
    }

    /// Update the local namespaces from a namespace set.
    ///
    /// The parameter is copied and stored locally.
    ///
    /// @param ns a non-null set of namesspaces which applies to this goal.
    public void makeLocalNamespacesFrom(NamespaceSet ns) {
        this.localNamespaces = ns.copyWithParent().copyWithParent();
    }

    /// adds a formula to the antecedent or succedent of a sequent. Either at its front or back and
    /// informs the rule application index about this change
    ///
    /// @param sf the SequentFormula to be added
    /// @param inAntec boolean true(false) if SequentFormula has to be added to antecedent
    /// (succedent)
    /// @param first boolean true if at the front, if false then sf is added at the back
    public void addFormula(SequentFormula sf, boolean inAntec, boolean first) {
        setSequent(sequent().addFormula(sf, inAntec, first));
    }

    /// replaces a formula at the given position and informs the rule application index about this
    /// change
    ///
    /// @param sf the SequentFormula replacing the old one
    /// @param p the PosInOccurrence encoding the position
    public void changeFormula(SequentFormula sf, PosInOccurrence p) {
        setSequent(sequent().changeFormula(sf, p));
    }

    @Override
    public long getTime() {
        return appliedRuleApps.size();
    }

    public ImmutableList<RuleApp> appliedRuleApps() {
        return appliedRuleApps;
    }

    public void setGoalStrategy(Strategy<@NonNull Goal> p_goalStrategy) {
        goalStrategy = p_goalStrategy;
        ruleAppManager.clearCache();
    }

    public Strategy<@NonNull Goal> getGoalStrategy() {
        if (goalStrategy == null) {
            goalStrategy = proof().getActiveStrategy();
        }
        return goalStrategy;
    }

    public FormulaTagManager getFormulaTagManager() {
        return tagManager;
    }

    public boolean isAutomatic() {
        // TODO
        return true;
    }

    /// adds the listener l to the list of goal listeners. Attention: A listener added to this goal
    /// will be taken over when splitting into subgoals.
    ///
    /// @param l the GoalListener to be added
    public void addGoalListener(GoalListener l) {
        listeners.add(l);
    }

    /// removes the listener l from the list of goal listeners. Attention: The listener is just
    /// removed from 'this' goal not from the other goals. (All goals can be accessed via proof
    /// openGoals())
    ///
    /// @param l the GoalListener to be removed
    public void removeGoalListener(GoalListener l) {
        listeners.remove(l);
    }
}
