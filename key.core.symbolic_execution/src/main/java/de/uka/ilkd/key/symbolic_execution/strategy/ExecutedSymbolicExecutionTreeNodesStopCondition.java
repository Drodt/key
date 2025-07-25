/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.symbolic_execution.strategy;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.prover.impl.ApplyStrategy;
import de.uka.ilkd.key.settings.StrategySettings;
import de.uka.ilkd.key.symbolic_execution.util.SymbolicExecutionUtil;

import org.key_project.prover.engine.SingleRuleApplicationInfo;
import org.key_project.prover.engine.StopCondition;
import org.key_project.prover.rules.RuleApp;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * <p>
 * This {@link StopCondition} stops the auto mode ({@link ApplyStrategy}) if a given number
 * ({@link #getMaximalNumberOfSetNodesToExecutePerGoal()}) of maximal executed symbolic execution
 * tree nodes is reached in a goal.
 * </p>
 * <p>
 * If a {@link Node} in KeY's proof tree is also a node in a symbolic execution tree is computed via
 * {@link SymbolicExecutionUtil#isSymbolicExecutionTreeNode}.
 * </p>
 * <p>
 * The auto mode is stopped exactly in the open goal {@link Node} which will become the next
 * symbolic execution tree node.
 * </p>
 *
 * @author Martin Hentschel
 */
public class ExecutedSymbolicExecutionTreeNodesStopCondition implements StopCondition<Goal> {
    /**
     * The default maximal number of steps to simulate a complete program execution.
     */
    public static final int MAXIMAL_NUMBER_OF_SET_NODES_TO_EXECUTE_PER_GOAL_IN_COMPLETE_RUN = 1000;

    /**
     * The default maximal number of steps to do exactly one step in each goal.
     */
    public static final int MAXIMAL_NUMBER_OF_SET_NODES_TO_EXECUTE_PER_GOAL_FOR_ONE_STEP = 1;

    /**
     * The maximal number of allowed symbolic execution tree nodes per goal. The auto mode will stop
     * exactly in the open goal proof node which becomes the next symbolic execution tree node.
     */
    private int maximalNumberOfSetNodesToExecutePerGoal;

    /**
     * Maps a {@link Goal} to the number of executed symbolic execution tree nodes.
     */
    private final Map<Goal, Integer> executedNumberOfSetNodesPerGoal =
        new LinkedHashMap<>();

    /**
     * Stores for each {@link Node} which is a symbolic execution tree node the computed result of
     * {@link StopCondition#isGoalAllowed(org.key_project.prover.proof.ProofGoal, int, long, long, int)}
     * to make sure that it is only
     * computed once and that the number of executed set statements is not increased multiple times
     * for the same {@link Node}.
     */
    private final Map<Node, Boolean> goalAllowedResultPerSetNode =
        new LinkedHashMap<>();

    /**
     * Constructor to stop after one executed symbolic execution tree node.
     */
    public ExecutedSymbolicExecutionTreeNodesStopCondition() {
        this(1);
    }

    /**
     * Constructor to stop after the given number of symbolic execution tree nodes.
     *
     * @param maximalNumberOfSetNodesToExecutePerGoal The maximal number of allowed symbolic
     *        execution tree nodes per goal.
     */
    public ExecutedSymbolicExecutionTreeNodesStopCondition(
            int maximalNumberOfSetNodesToExecutePerGoal) {
        this.maximalNumberOfSetNodesToExecutePerGoal = maximalNumberOfSetNodesToExecutePerGoal;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getMaximalWork(int maxApplications, long timeout) {
        executedNumberOfSetNodesPerGoal.clear(); // Reset number of already detected symbolic
                                                 // execution tree nodes for all goals.
        goalAllowedResultPerSetNode.clear(); // Remove no longer needed references.
        return 0; // Return unknown because there is no relation between applied rules and executed
                  // symbolic execution tree nodes.
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isGoalAllowed(Goal goal, int maxApplications, long timeout, long startTime,
            int countApplied) {
        if (goal != null) {
            Node node = goal.node();
            // Check if goal is allowed
            RuleApp ruleApp = goal.getRuleAppManager().peekNext();
            if (SymbolicExecutionUtil.isSymbolicExecutionTreeNode(node, ruleApp)) {
                // Check if the result for the current node was already computed.
                Boolean value = goalAllowedResultPerSetNode.get(node);
                if (value == null) {
                    // Get the number of executed set nodes on the current goal
                    Integer executedNumberOfSetNodes = executedNumberOfSetNodesPerGoal.get(goal);
                    if (executedNumberOfSetNodes == null) {
                        executedNumberOfSetNodes = 0;
                    }
                    // Check if limit of set nodes of the current goal is exceeded
                    if (executedNumberOfSetNodes
                            + 1 > maximalNumberOfSetNodesToExecutePerGoal) {
                        handleNodeLimitExceeded(node);
                        return false; // Limit of set nodes of this goal exceeded
                    } else {
                        // Increase number of set nodes on this goal and allow rule application
                        executedNumberOfSetNodes =
                            executedNumberOfSetNodes.intValue() + 1;
                        executedNumberOfSetNodesPerGoal.put(goal, executedNumberOfSetNodes);
                        handleNodeLimitNotExceeded(maxApplications, timeout, goal.proof(),
                            startTime,
                            countApplied, goal, node, ruleApp, executedNumberOfSetNodes);
                        return true;
                    }
                } else {
                    // Reuse already computed result.
                    return value;
                }
            } else {
                return true;
            }
        } else {
            return true; // Allowed, because ApplyStrategy will handle the null case
        }
    }

    /**
     * Handles the state that the node limit is exceeded.
     *
     * @param node The {@link Node} of the current {@link Goal}.
     */
    protected void handleNodeLimitExceeded(Node node) {
        goalAllowedResultPerSetNode.put(node, Boolean.FALSE);
    }

    /**
     * Handles the state that the node limit is not exceeded.
     *
     * @param maxApplications The defined maximal number of rules to apply. Can be different to
     *        {@link StrategySettings#getMaxSteps()} in side proofs.
     * @param timeout The defined timeout in ms or {@code -1} if disabled. Can be different to
     *        {@link StrategySettings#getTimeout()} in side proofs.
     * @param proof The current {@link Proof}.
     * @param startTime The timestamp when the apply strategy has started, computed via
     *        {@link System#nanoTime()}
     * @param countApplied The number of already applied rules.
     * @param goal The current {@link Goal} on which the next rule will be applied.
     * @param node The {@link Node} of the current {@link Goal}.
     * @param ruleApp The current {@link RuleApp}.
     * @param executedNumberOfSetNodes The executed number of SET nodes.
     */
    protected void handleNodeLimitNotExceeded(int maxApplications, long timeout, Proof proof,
            long startTime, int countApplied, Goal goal, Node node,
            RuleApp ruleApp,
            Integer executedNumberOfSetNodes) {
        goalAllowedResultPerSetNode.put(node, Boolean.TRUE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getGoalNotAllowedMessage(Goal goal, int maxApplications, long timeout,
            long startTime, int countApplied) {
        if (maximalNumberOfSetNodesToExecutePerGoal > 1) {
            return "Maximal limit of " + maximalNumberOfSetNodesToExecutePerGoal
                + " symbolic execution tree nodes reached.";
        } else {
            return "Maximal limit of one symbolic execution tree node reached.";
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldStop(int maxApplications, long timeout, long startTime,
            int countApplied, SingleRuleApplicationInfo singleRuleApplicationInfo) {
        // Check if a rule was applied
        if (singleRuleApplicationInfo != null) {
            // Get the node on which a rule was applied.
            Goal goal = singleRuleApplicationInfo.getGoal();
            Node goalNode = goal.node();
            assert goalNode.childrenCount() == 0; // Make sure that this is the current goal node
            Node updatedNode = goalNode.parent();
            // Check if multiple branches where created.
            if (updatedNode.childrenCount() >= 2) {
                // If a number of executed set nodes is available for the goal it must be used for
                // all other new created goals.
                Integer executedValue = executedNumberOfSetNodesPerGoal.get(goal);
                if (executedValue != null) {
                    // Reuse number of set nodes for new created goals
                    Iterator<Node> childIter = updatedNode.childrenIterator();
                    while (childIter.hasNext()) {
                        Node next = childIter.next();
                        Goal nextGoal = next.proof().getOpenGoal(next);
                        // Check if the current goal is a new one
                        if (nextGoal != goal) {
                            // New goal found, use the number of set nodes for it.
                            executedNumberOfSetNodesPerGoal.put(nextGoal, executedValue);
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull String getStopMessage(int maxApplications, long timeout, long startTime,
            int countApplied, @Nullable SingleRuleApplicationInfo singleRuleApplicationInfo) {
        return "";
    }

    /**
     * Returns the maximal number of executed symbolic execution tree nodes per goal per auto mode
     * run.
     *
     * @return The maximal number of executed symbolic execution tree nodes per goal per auto mode
     *         run.
     */
    public int getMaximalNumberOfSetNodesToExecutePerGoal() {
        return maximalNumberOfSetNodesToExecutePerGoal;
    }

    /**
     * Sets the maximal number of executed symbolic execution tree nodes per goal per auto mode run.
     *
     * @param maximalNumberOfSetNodesToExecute The maximal number of executed symbolic execution
     *        tree nodes per per goal auto mode run.
     */
    public void setMaximalNumberOfSetNodesToExecutePerGoal(int maximalNumberOfSetNodesToExecute) {
        this.maximalNumberOfSetNodesToExecutePerGoal = maximalNumberOfSetNodesToExecute;
    }

    /**
     * Checks if at least one symbolic execution tree node was executed.
     *
     * @return {@code true} at least one symbolic execution tree node was executed, {@code false} no
     *         symbolic execution tree node was executed.
     */
    public boolean wasSetNodeExecuted() {
        return !executedNumberOfSetNodesPerGoal.isEmpty();
    }

    /**
     * Returns the number of executed symbolic execution tree nodes per {@link Goal}.
     *
     * @return The number of executed symbolic execution tree nodes per {@link Goal}.
     */
    public Map<Goal, Integer> getExectuedSetNodesPerGoal() {
        return executedNumberOfSetNodesPerGoal;
    }
}
