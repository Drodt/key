/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Term;
import org.key_project.prover.indexing.RuleAppIndexMode;
import org.key_project.prover.proof.ProofObject;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.prover.strategy.Strategy;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.proof.calculus.RustySequentKit;
import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.proof.init.Profile;
import org.key_project.rusty.proof.mgt.ProofCorrectnessMgt;
import org.key_project.rusty.proof.mgt.ProofEnvironment;
import org.key_project.rusty.settings.ProofSettings;
import org.key_project.rusty.strategy.StrategyProperties;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public class Proof implements ProofObject<Goal>, Named {
    /// name of the proof
    private final Name name;

    /// The time when the [Proof] instance was created.
    final long creationTime = System.currentTimeMillis();

    /// the root of the proof
    private @Nullable Node root;

    /// list with prooftree listeners of this proof attention: firing events makes use of array
    /// list's random access nature
    private final List<ProofTreeListener> listenerList = new LinkedList<>();

    /// list of rule app listeners
    private final List<RuleAppListener> ruleAppListenerList =
        Collections.synchronizedList(new ArrayList<>(10));

    /// list with the open goals of the proof
    private ImmutableList<Goal> openGoals = ImmutableList.nil();

    /// list with the closed goals of the proof, needed to make pruning in closed branches possible.
    /// If the list needs too much memory, pruning can be disabled via the command line option
    /// "--no-pruning-closed". In this case the list will not be filled.
    private ImmutableList<Goal> closedGoals = ImmutableList.nil();

    /// Mode specifying the rule app index to use. The field is read by the prover threads, hence,
    /// volatile.
    private volatile RuleAppIndexMode ruleAppIndexMode = RuleAppIndexMode.INTERACTIVE;

    /// the number of open scopes of the rule app index mode
    private final AtomicInteger openRuleAppIndexModeScopes = new AtomicInteger();

    /// the logic configuration for this proof, i.e., logic signature, rules etc.
    private InitConfig initConfig;

    /// declarations &c, read from a problem file or otherwise
    private String problemHeader = "";

    /// the environment of the proof with specs and java model
    private ProofCorrectnessMgt localMgt;

    private long autoModeTime = 0;

    private @Nullable Strategy<@NonNull Goal> activeStrategy;

    /// Whether closing the proof should emit a [ProofEvent].
    private boolean mutedProofCloseEvents = false;

    /// Set once an [EssentialProofListener] failed while handling an event: the step that
    /// fired it may have left the proof or a goal inconsistent, so the running search is stopped
    /// (the provers poll this), no new search may be started, but the proof object stays intact so
    /// the user can still attempt to save it. Volatile: written from the (possibly worker) firing
    /// thread, read by the prover loop(s) and the auto-mode control.
    private volatile boolean erroneous = false;

    /// the proof environment (optional)
    private @Nullable ProofEnvironment env;

    /// constructs a new empty proof with name
    private Proof(Name name, InitConfig initConfig) {
        this.name = name;
        assert initConfig != null : "Tried to create proof without valid services.";
        this.initConfig = initConfig;

        if (initConfig.getSettings() == null) {
            // if no settings have been assigned yet, take default settings
            initConfig.setSettings(new ProofSettings(ProofSettings.DEFAULT_SETTINGS));
        }

        localMgt = new ProofCorrectnessMgt(this);

        final Services services = this.initConfig.getServices();
        services.setProof(this);
    }

    /// constructs a new empty proof with name
    public Proof(String name, InitConfig initConfig) {
        this(new Name(name), initConfig);
    }

    public Proof(String name, Sequent problem, TacletIndex tacletIndex,
            BuiltInRuleIndex builtInRules,
            InitConfig initConfig) {
        this(new Name(name), initConfig);

        final var rootNode = new Node(this, problem);
        final var firstGoal =
            new Goal(rootNode, tacletIndex, new BuiltInRuleAppIndex(builtInRules),
                initConfig.getServices());
        openGoals = openGoals.prepend(firstGoal);
        setRoot(rootNode);
    }

    public Proof(String name, Term problem, String header, InitConfig initConfig) {
        this(name,
            RustySequentKit
                    .createSuccSequent(ImmutableList.singleton(new SequentFormula(problem))),
            initConfig.createTacletIndex(),
            initConfig.createBuiltInRuleIndex(),
            initConfig);
        problemHeader = header;
    }

    public Proof(Name name, Sequent problem, String header, InitConfig initConfig) {
        this(name.toString(), problem, initConfig.createTacletIndex(),
            initConfig.createBuiltInRuleIndex(), initConfig);
        problemHeader = header;
    }

    public Services getServices() {
        return initConfig.getServices();
    }

    public Node getRoot() {
        return root;
    }

    public void setRoot(Node root) {
        this.root = root;
    }

    public String header() {
        return problemHeader;
    }

    public ImmutableList<Goal> getOpenGoals() {
        return openGoals;
    }

    public void setOpenGoals(ImmutableList<Goal> openGoals) {
        this.openGoals = openGoals;
    }

    public ImmutableList<Goal> getClosedGoals() {
        return closedGoals;
    }

    public void setClosedGoals(ImmutableList<Goal> closedGoals) {
        this.closedGoals = closedGoals;
    }

    public InitConfig getInitConfig() {
        return initConfig;
    }

    public void setInitConfig(InitConfig initConfig) {
        this.initConfig = initConfig;
    }

    @Override
    public @NonNull Name name() {
        return name;
    }


    public Node root() {
        return root;
    }

    /// Returns the list of open goals.
    ///
    /// @return list with the open goals
    @Override
    public @NonNull ImmutableList<@NonNull Goal> openGoals() {
        return openGoals;
    }

    /// adds a list with new goals to the list of open goals
    ///
    /// @param goals the Iterable<Goal> to be prepended
    @Override
    public void add(@NonNull Iterable<@NonNull Goal> goals) {
        ImmutableList<Goal> addGoals;
        if (goals instanceof ImmutableList<Goal> asList) {
            addGoals = asList;
        } else {
            addGoals = ImmutableList.fromList(goals);
        }
        add(addGoals);
    }

    /// adds a list with new goals to the list of open goals
    ///
    /// @param goals the IList<Goal> to be prepended
    public void add(ImmutableList<Goal> goals) {
        ImmutableList<Goal> newOpenGoals = openGoals.prepend(goals);
        if (openGoals != newOpenGoals) {
            openGoals = newOpenGoals;
        }
    }

    /// removes the given goal and adds the new goals in list
    ///
    /// @param oldGoal the old goal that has to be removed from list
    /// @param newGoals the Iterable<Goal> with the new goals that were result of a rule application
    /// on goal
    @Override
    public void replace(Goal oldGoal, @NonNull Iterable<@NonNull Goal> newGoals) {
        openGoals = openGoals.removeAll(oldGoal);

        if (!closed()) {
            add(newGoals);
        }
    }

    /// Close the given goals and all goals in the subtree below it.
    ///
    /// @param goalToClose the goal to close.
    public void closeGoal(Goal goalToClose) {
        Node closedSubtree = goalToClose.getNode().close();

        boolean b = false;
        Iterator<Node> it = closedSubtree.leavesIterator();
        Goal goal;

        // close all goals below the given goalToClose
        while (it.hasNext()) {
            goal = getOpenGoal(it.next());
            if (goal != null) {
                b = true;
                // if (!GeneralSettings.noPruningClosed) {
                closedGoals = closedGoals.prepend(goal);
                // }
                remove(goal);
            }
        }

        if (b) {
            // For the moment it is necessary to fire the message ALWAYS
            // in order to detect branch closing.
            // fireProofGoalsAdded(ImmutableSLList.nil());
        }
    }

    /// returns the goal that belongs to the given node or null if the node is an inner one
    ///
    /// @return the goal that belongs to the given node or null if the node is an inner one
    public Goal getOpenGoal(Node node) {
        for (final Goal result : openGoals) {
            if (result.getNode() == node) {
                return result;
            }
        }
        return null;
    }

    /// removes the given goal from the list of open goals. Take care removing the last goal will
    /// fire the proofClosed event
    ///
    /// @param goal the Goal to be removed
    private void remove(Goal goal) {
        ImmutableList<Goal> newOpenGoals = openGoals.removeAll(goal);
        if (newOpenGoals != openGoals) {
            openGoals = newOpenGoals;
        }
    }

    /// returns true if the root node is marked as closed and all goals have been removed
    public boolean closed() {
        return root.isClosed() && openGoals.isEmpty();
    }

    public ProofCorrectnessMgt mgt() {
        return localMgt;
    }

    /// Retrieves a bunch of statistics to the proof tree. This implementation traverses the proof
    /// tree only once. Statistics are not cached; don't call this method too often.
    public Statistics getStatistics() {
        return new Statistics(this);
    }

    /// retrieves number of nodes
    public int countNodes() {
        return root.countNodes();
    }

    /// toString
    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("Proof -- ");
        if (!name.toString().isEmpty()) {
            result.append(name);
        } else {
            result.append("unnamed");
        }
        result.append("\nProoftree:\n");
        if (countNodes() < 50) {
            result.append(root.toString());
        } else {
            result.append("<too large to include>");
        }
        return result.toString();
    }

    public void dispose() {
        // TODO
        if (getServices() != null) {
            getServices().getSpecificationRepository().removeProof(this);
        }
    }

    /// returns a collection of the namespaces valid for this proof
    public NamespaceSet getNamespaces() {
        return getServices().getNamespaces();
    }

    /// sets the variable, function, sort, heuristics namespaces
    public void setNamespaces(NamespaceSet ns) {
        getServices().setNamespaces(ns);
        if (!root.leaf()) {
            throw new IllegalStateException("Proof: ProgVars set too late");
        }

        Goal fstGoal = openGoals().head();
        fstGoal.makeLocalNamespacesFrom(ns);
    }

    public long getAutoModeTime() {
        return autoModeTime;
    }

    public void addAutoModeTime(long time) {
        autoModeTime += time;
    }

    public ProofSettings getSettings() {
        return initConfig.getSettings();
    }

    /// adds a listener to the proof
    ///
    /// @param listener the ProofTreeListener to be added
    public synchronized void addProofTreeListener(ProofTreeListener listener) {
        synchronized (listenerList) {
            listenerList.add(listener);
        }
    }

    /// removes a listener from the proof
    ///
    /// @param listener the ProofTreeListener to be removed
    public synchronized void removeProofTreeListener(ProofTreeListener listener) {
        synchronized (listenerList) {
            listenerList.remove(listener);
        }
    }

    public void addRuleAppListener(RuleAppListener p) {
        if (p == null) {
            return;
        }
        synchronized (ruleAppListenerList) {
            ruleAppListenerList.add(p);
        }
    }

    public void removeRuleAppListener(RuleAppListener p) {
        synchronized (ruleAppListenerList) {
            ruleAppListenerList.remove(p);
        }
    }

    public Strategy<Goal> getActiveStrategy() {
        if (activeStrategy == null) {
            initStrategy();
        }
        return activeStrategy;
    }

    public void setActiveStrategy(Strategy<@NonNull Goal> activeStrategy) {
        this.activeStrategy = activeStrategy;
        getSettings().getStrategySettings().setStrategy(activeStrategy.name());
        updateStrategyOnGoals();

        // This could be seen as a hack; it's however important that OSS is
        // refreshed after strategy has been set, otherwise nothing will happen.
        // OneStepSimplifier.refreshOSS(root.proof());
    }

    /// initialises the strategies
    private void initStrategy() {
        StrategyProperties activeStrategyProperties =
            initConfig.getSettings().getStrategySettings().getActiveStrategyProperties();

        final Profile profile = getServices().getProfile();

        final Name strategy = initConfig.getSettings().getStrategySettings().getStrategy();
        if (profile.supportsStrategyFactory(strategy)) {
            setActiveStrategy(
                profile.getStrategyFactory(strategy).create(this, activeStrategyProperties));
        } else {
            setActiveStrategy(
                profile.getDefaultStrategyFactory().create(this, activeStrategyProperties));
        }
    }

    private void updateStrategyOnGoals() {
        Strategy<@NonNull Goal> ourStrategy = getActiveStrategy();

        for (Goal goal : openGoals()) {
            goal.setGoalStrategy(ourStrategy);
        }
    }

    /// return the list of open and enabled goals
    ///
    /// @return list of open and enabled goals, never null
    /// @author mulbrich
    public ImmutableList<Goal> openEnabledGoals() {
        return filterEnabledGoals(openGoals);
    }

    /// filter those goals from a list which are enabled
    ///
    /// @param goals non-null list of goals
    /// @return sublist such that every goal in the list is enabled
    /// @author mulbrich
    /// @see Goal#isAutomatic()
    private ImmutableList<Goal> filterEnabledGoals(ImmutableList<Goal> goals) {
        ImmutableList<Goal> enabledGoals = ImmutableList.nil();
        for (Goal g : goals) {
            if (g.isAutomatic()) {
                enabledGoals = enabledGoals.prepend(g);
            }
        }
        return enabledGoals;
    }

    public void setEnv(ProofEnvironment env) {
        this.env = env;
    }

    /// Currently the rule app index can either operate in interactive mode (and contain
    /// applications
    /// of all existing taclets) or in automatic mode (and only contain a restricted set of taclets
    /// that can possibly be applied automated). This distinction could be replaced with a more
    /// general way to control the contents of the rule app index
    public void setRuleAppIndexToAutoMode() {
        for (final Goal g : openGoals) {
            g.ruleAppIndex().autoModeStarted();
        }
    }

    public void setRuleAppIndexToInteractiveMode() {
        for (final Goal g : openGoals) {
            g.ruleAppIndex().autoModeStopped();
        }
    }

    /// returns the list of goals of the subtree starting with node.
    ///
    /// @param node the Node where to start from
    /// @return the list of goals of the subtree starting with node
    public ImmutableList<Goal> getSubtreeGoals(Node node) {
        return getGoalsBelow(node, openGoals);
    }

    /// Returns a list of all goals from the provided list that are associated to goals below
    /// <code>node</code>
    ///
    /// @param node the root of the subtree
    /// @param fromGoals the list of goals from which to select
    /// @return the goals below node that are contained in <code>fromGoals</code>
    private static ImmutableList<Goal> getGoalsBelow(Node node, ImmutableList<Goal> fromGoals) {
        ImmutableList<Goal> result = ImmutableList.nil();
        List<Node> leaves = node.getLeaves();
        for (final Goal goal : fromGoals) {
            // if list contains node, remove it to make the list faster later
            if (leaves.remove(goal.getNode())) {
                result = result.prepend(goal);
            }
        }
        return result;
    }

    /// Performs an undo operation on the given goal. This is equivalent to a pruning of the parent
    /// node of the goal (if this parent node exists).
    ///
    /// @param goal the Goal where the last rule application gets undone
    public synchronized void pruneProof(Goal goal) {
        if (goal.getNode().parent() != null) {
            pruneProof(goal.getNode().parent());
        }
    }

    /// Prunes the subtree beneath the node <code>cuttingPoint</code>, i.e. the node
    /// <code>cuttingPoint</code> remains as the last node on the branch. As a result, an open goal
    /// is associated with this node.
    ///
    /// @param cuttingPoint node below which to prune
    /// @return the subtrees that have been pruned.
    public synchronized ImmutableList<Node> pruneProof(Node cuttingPoint) {
        return pruneProof(cuttingPoint, true);
    }

    public synchronized ImmutableList<Node> pruneProof(Node cuttingPoint, boolean fireChanges) {
        assert cuttingPoint.proof() == this;
        if (getOpenGoal(cuttingPoint) != null) {
            return null;
        }
        // abort pruning if the node is closed and pruning in closed branches is disabled
        // if (cuttingPoint.isClosed() && GeneralSettings.noPruningClosed) {
        // return null;
        // }

        ProofPruner pruner = new ProofPruner(this);
        if (fireChanges) {
            fireProofIsBeingPruned(cuttingPoint);
        }
        ImmutableList<Node> result = pruner.prune(cuttingPoint);
        if (fireChanges) {
            fireProofGoalsChanged();
            fireProofPruned(cuttingPoint);
        }
        return result;
    }

    /// Makes a downwards directed breadth first search on the proof tree, starting with node
    /// <code>startNode</code>. The visited notes are reported to the object <code>visitor</code>.
    /// The first reported node is <code>startNode</code>.
    public void breadthFirstSearch(Node startNode, ProofVisitor visitor) {
        ArrayDeque<Node> queue = new ArrayDeque<>();
        queue.add(startNode);
        while (!queue.isEmpty()) {
            Node currentNode = queue.poll();
            Iterator<Node> it = currentNode.childrenIterator();
            while (it.hasNext()) {
                queue.add(it.next());
            }
            visitor.visit(this, currentNode);
        }
    }

    /// Get the closed goal belonging to the given node if it exists.
    ///
    /// @param node the Node where a corresponding closed goal is searched
    /// @return the closed goal that belongs to the given node or null if the node is an inner one
    /// or
    /// an open goal
    public @Nullable Goal getClosedGoal(Node node) {
        for (final Goal result : closedGoals) {
            if (result.getNode() == node) {
                return result;
            }
        }
        return null;
    }

    /// Opens a previously closed node (the one corresponding to p_goal) and all its closed parents.
    ///
    /// This is, for instance, needed for the `MergeRule`: In a situation where a merge node
    /// and its associated partners have been closed and the merge node is then pruned away, the
    /// partners have to be reopened again. Otherwise, we have a soundness issue.
    ///
    /// This will automatically add the goal to the list of open goals.
    ///
    /// @param goal The goal to be opened again.
    public void reOpenGoal(Goal goal) {
        add(goal);
        goal.getNode().reopen();
        closedGoals = closedGoals.removeAll(goal);
        fireProofStructureChanged();
    }

    /// adds a new goal to the list of goals
    ///
    /// @param goal the Goal to be added
    private void add(Goal goal) {
        ImmutableList<Goal> newOpenGoals = openGoals.prepend(goal);
        if (openGoals != newOpenGoals) {
            openGoals = newOpenGoals;
            fireProofGoalsAdded(goal);
        }
    }

    public void traverseFromChildToParent(Node child, Node parent, ProofVisitor visitor) {
        do {
            visitor.visit(this, child);
            child = child.parent();
        } while (child != parent);
    }

    void removeOpenGoals(Collection<Node> toBeRemoved) {
        ImmutableList<Goal> newGoalList = ImmutableList.nil();
        for (Goal openGoal : openGoals()) {
            if (!toBeRemoved.contains(openGoal.getNode())) {
                newGoalList = newGoalList.append(openGoal);
            }
        }
        openGoals = newGoalList;
    }

    /// Removes the given collection of Nodes from the closedGoals. Nodes in the given collection
    /// which are not member of closedGoals are ignored. This method does not reopen the goals!
    /// This has to be done via the method reOpenGoal() if desired.
    ///
    /// @param toBeRemoved the goals to remove
    void removeClosedGoals(Collection<Node> toBeRemoved) {
        ImmutableList<Goal> newGoalList = ImmutableList.nil();
        for (Goal closedGoal : closedGoals) {
            if (!toBeRemoved.contains(closedGoal.getNode())) {
                newGoalList = newGoalList.prepend(closedGoal);
            }
        }
        closedGoals = newGoalList;
    }


    /// fires the event that the proof is being pruned at the given node
    protected void fireProofIsBeingPruned(Node below) {
        ProofTreeEvent e = new ProofTreeEvent(this, below);
        synchronized (listenerList) {
            for (ProofTreeListener listener : listenerList) {
                listener.proofIsBeingPruned(e);
            }
        }
    }

    /// fires the event that the proof has been pruned at the given node
    protected void fireProofPruned(Node below) {
        ProofTreeEvent e = new ProofTreeEvent(this, below);
        synchronized (listenerList) {
            for (ProofTreeListener listener : listenerList) {
                listener.proofPruned(e);
            }
        }
    }

    /// fires the event that the proof has been restructured
    public void fireProofGoalsChanged() {
        ProofTreeEvent e = new ProofTreeEvent(this, openGoals());
        synchronized (listenerList) {
            for (ProofTreeListener listener : listenerList) {
                listener.proofGoalsChanged(e);
            }
        }
    }


    /// fires the event that a rule has been applied
    protected void fireRuleApplied(ProofEvent p_e) {
        synchronized (ruleAppListenerList) {
            for (RuleAppListener ral : ruleAppListenerList) {
                ral.ruleApplied(p_e);
            }
        }
    }

    /// fires the event that new goals have been added to the list of goals
    protected void fireProofGoalsAdded(ImmutableList<Goal> goals) {
        ProofTreeEvent e = new ProofTreeEvent(this, goals);
        synchronized (listenerList) {
            for (ProofTreeListener listener : listenerList) {
                listener.proofGoalsAdded(e);
            }
        }
    }

    /// fires the event that new goals have been added to the list of goals
    protected void fireProofGoalsAdded(Goal goal) {
        fireProofGoalsAdded(ImmutableList.singleton(goal));
    }

    /// fires the event that the proof has been restructured
    public void fireProofStructureChanged() {
        ProofTreeEvent e = new ProofTreeEvent(this);
        synchronized (listenerList) {
            for (ProofTreeListener listener : listenerList) {
                listener.proofStructureChanged(e);
            }
        }
    }

    public int countBranches() {
        return root.countBranches();
    }

    /// get the list of goals of the subtree starting with node which are enabled.
    ///
    /// @param node the Node where to start from
    /// @return the list of enabled goals of the subtree starting with node
    public ImmutableList<Goal> getSubtreeEnabledGoals(Node node) {
        return filterEnabledGoals(getSubtreeGoals(node));
    }

    public RuleAppIndexMode getRuleAppIndexMode() {
        return ruleAppIndexMode;
    }

    /// Sets the mode of the rule app indices of the proof.
    /// It returns a closeable that restores the previous mode, hence, this method should
    /// only be called using the pattern `try(var mode = setRuleAppIndexMode()) ...`
    /// Closing the last open scope resets **always** to the interactive mode.
    /// Precaution to enable always all rules in the GUI, no matter in which order
    /// threads close the scope. In general it should never happen that two provers using a
    /// different
    /// rule application index run at the same time.
    ///
    /// @param mode the mode of the rule app indices
    /// @return the mode's scope
    public AutoCloseable setRuleAppIndexMode(RuleAppIndexMode mode) {
        final RuleAppIndexMode previous = ruleAppIndexMode;
        openRuleAppIndexModeScopes.incrementAndGet();
        ruleAppIndexMode = mode;
        return () -> ruleAppIndexMode = openRuleAppIndexModeScopes.decrementAndGet() == 0
                ? RuleAppIndexMode.INTERACTIVE
                : previous;
    }

    /// Notifies every listener of an event, isolating the proof from a listener that throws. The
    /// caller must hold the monitor of `listeners`.
    ///
    /// A failing _non-essential_ listener (a pure observer) is logged and unregistered, and
    /// the remaining listeners are still notified: its brokenness cannot corrupt the proof, so the
    /// search continues unaffected.
    ///
    /// A failing [EssentialProofListener] is different: it is part of the proving machinery,
    /// so its failure means the step that just fired this event may already have left the proof or
    /// a
    /// goal inconsistent. Continuing to prove would build on a broken state. Such a failure instead
    /// [marks the proof erroneous][#markErroneous] -- which cooperatively stops the
    /// running search and blocks any new one -- while leaving the proof intact so it can still be
    /// saved. The essential listener is _not_ unregistered (dropping it would merely hide the
    /// problem), and no further listeners are notified for this event.
    ///
    /// @param listeners the listeners to notify (a throwing non-essential listener is pruned)
    /// @param notify the notification to deliver to each listener
    /// @param <L> the listener type
    /// @return the pruned (broken, unregistered) listeners; empty if none. Callers that notified a
    /// snapshot list must drop these from the live registration list themselves.
    private <L> List<L> notifyListeners(List<L> listeners, Consumer<L> notify) {
        List<L> broken = null;
        for (L listener : listeners) {
            try {
                notify.accept(listener);
            } catch (Exception e) {
                if (listener instanceof EssentialProofListener) {
                    markErroneous(listener.getClass().getName(), e);
                    // The proof is now flagged; the prover will stop at its next check and no new
                    // search can start. Do not touch the remaining listeners for this event.
                    break;
                }
                // LOGGER.error("proof listener {} threw while handling an event and will be "
                // + "unregistered to keep the proof consistent", listener.getClass().getName(),
                // e);
                if (broken == null) {
                    broken = new ArrayList<>();
                }
                broken.add(listener);
            }
        }
        if (broken != null) {
            listeners.removeAll(broken);
            return broken;
        }
        return List.of();
    }

    /**
     * @return whether this proof has been flagged as erroneous because an
     *         {@link EssentialProofListener} failed (see {@link #notifyListeners}). An erroneous
     *         proof stops its running search and refuses to start a new one, but may still be
     *         saved.
     */
    @Override
    public boolean isErroneous() {
        return erroneous;
    }

    /// Flags this proof as [erroneous][#isErroneous()] after an essential-listener
    /// failure. Idempotent; only the first failure is logged.
    ///
    /// @param listenerName the failing listener's class name (for the log)
    /// @param cause the throwable it raised
    private void markErroneous(String listenerName, Throwable cause) {
        if (!erroneous) {
            erroneous = true;
            // LOGGER.error("essential proof listener {} failed; the proof search is stopped and the
            // "
            // + "proof is marked erroneous (no further search possible). The proof can still be "
            // + "saved for a later reload attempt.", listenerName, cause);
        }
    }

    /// Detaches every registered [ProofTreeListener] and [RuleAppListener] that is not
    /// tagged [EssentialProofListener], and returns a handle that re-attaches them when
    /// closed.
    ///
    /// This is the foundation for safe automatic (and, later, multithreaded) proof search: pure
    /// observers — caching, slicing, origin labels, GUI tree models, … — are
    /// suspended for the duration of a run so that nothing unrelated to proving fires on a worker
    /// thread or mutates shared state concurrently. Essential bookkeeping listeners keep running.
    /// [ProofDisposedListener]s are untouched, as disposal is not part of a run.
    ///
    /// Intended use is a try-with-resources scope around the prover's run:
    ///
    /// <pre>
    /// try (var ignored = proof.suspendNonEssentialListeners()) {
    /// prover.start(proof, goals, ...);
    /// } // observers re-attached here; the caller then refreshes them from the final state
    /// </pre>
    ///
    /// Suspended listeners are re-attached on [ListenerSuspension#close()]; their relative
    /// firing order is not guaranteed to be preserved, which is sound because proof listeners are
    /// mutually independent.
    ///
    /// @return a handle whose [ListenerSuspension#close()] restores the suspended listeners;
    /// restoration is idempotent
    public ListenerSuspension suspendNonEssentialListeners() {
        return new ListenerSuspension();
    }

    /**
     * Handle returned by {@link Proof#suspendNonEssentialListeners()}. Closing it (idempotently)
     * re-attaches the listeners that were suspended.
     */
    public final class ListenerSuspension implements AutoCloseable {
        private final List<ProofTreeListener> suspendedTreeListeners = new ArrayList<>();
        private final List<RuleAppListener> suspendedRuleAppListeners = new ArrayList<>();
        /**
         * Distinct non-essential per-goal listeners (e.g. the GUI proof-tree model's goal listener,
         * which is attached to every open goal). Unlike the proof-level listener lists, these are
         * registered per goal and would otherwise fire on the worker threads during a parallel run
         * -- and touch EDT-only state. They are detached for the run's duration and re-attached on
         * {@link #close()}, to the <em>current</em> open goals (the goal set changes during the
         * run).
         */
        private final Set<GoalListener> suspendedGoalListeners =
            Collections.newSetFromMap(new IdentityHashMap<>());
        private boolean restored = false;
        /**
         * Whether the proof was already closed when the listeners were suspended. Used to decide on
         * {@link #close()} whether a {@code proofClosed} event was missed by the suspended
         * listeners.
         */
        private final boolean closedAtSuspension = closed();

        private ListenerSuspension() {
            synchronized (listenerList) {
                for (ProofTreeListener l : listenerList) {
                    if (!(l instanceof EssentialProofListener)) {
                        suspendedTreeListeners.add(l);
                    }
                }
                listenerList.removeAll(suspendedTreeListeners);
            }
            synchronized (ruleAppListenerList) {
                for (RuleAppListener l : ruleAppListenerList) {
                    if (!(l instanceof EssentialProofListener)) {
                        suspendedRuleAppListeners.add(l);
                    }
                }
                ruleAppListenerList.removeAll(suspendedRuleAppListeners);
            }
            // Detach non-essential goal listeners from every open goal. (Construction runs before
            // any
            // worker starts, so the open-goal set and the goals' listener lists are stable here.)
            for (Goal g : openGoals()) {
                for (GoalListener l : g.getGoalListeners()) {
                    if (!(l instanceof EssentialProofListener)) {
                        suspendedGoalListeners.add(l);
                    }
                }
            }
            for (Goal g : openGoals()) {
                for (GoalListener l : suspendedGoalListeners) {
                    g.removeGoalListener(l);
                }
            }
        }

        /** Re-attaches the suspended listeners. Idempotent: a second call does nothing. */
        @Override
        public void close() {
            if (restored) {
                return;
            }
            restored = true;
            synchronized (listenerList) {
                listenerList.addAll(suspendedTreeListeners);
            }
            synchronized (ruleAppListenerList) {
                ruleAppListenerList.addAll(suspendedRuleAppListeners);
            }
            // Re-attach to the goals that are open now (the run may have closed some and created
            // others); remove-then-add keeps it idempotent and avoids duplicates. The view itself
            // is
            // refreshed from the final state by the caller (the GUI's autoModeStopped handler
            // rebuilds
            // the modified subtrees on the EDT).
            for (Goal g : openGoals()) {
                for (GoalListener l : suspendedGoalListeners) {
                    g.removeGoalListener(l);
                    g.addGoalListener(l);
                }
            }
            // If the proof closed while its non-essential listeners were suspended, the proofClosed
            // event reached only the essential listeners; the suspended ones (e.g. the GUI's
            // "proof closed" notification, the task and goal views) missed it. Re-deliver it to
            // them now. This runs after the parallel run has joined all workers, so it is a single
            // delivery on one thread -- the same context in which proofClosed fires for the
            // single-threaded prover.
            if (!closedAtSuspension && !mutedProofCloseEvents && closed()) {
                ProofTreeEvent event = new ProofTreeEvent(Proof.this);
                // go through notifyListeners for the same per-listener exception isolation every
                // other proofClosed dispatch uses; a throwing GUI listener must not escape here and
                // abort the parallel run's winddown
                List<ProofTreeListener> broken =
                    notifyListeners(suspendedTreeListeners, l -> l.proofClosed(event));
                if (!broken.isEmpty()) {
                    // notifyListeners pruned them from the suspension snapshot, which is about to
                    // be discarded -- but they were already re-attached above, so unregister them
                    // from the live list as well.
                    synchronized (listenerList) {
                        listenerList.removeAll(broken);
                    }
                }
            }
        }
    }

    public void setMutedProofCloseEvents(boolean mutedProofCloseEvents) {
        this.mutedProofCloseEvents = mutedProofCloseEvents;
    }
}
