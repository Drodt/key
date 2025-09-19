/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof.proofevent;

import java.util.Iterator;

import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.GoalListener;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.proof.Proof;
import org.key_project.util.collection.*;

import org.jspecify.annotations.NonNull;

/// Collect changes applied to a node during a given period of time
public class NodeChangeJournal implements GoalListener {
    private final Proof proof;

    /// The original node
    private final Node node;

    /// This is a map storing the leaves that are currently below the original node, and all changes
    /// applied to each of them
    private ImmutableMap<@NonNull Node, NodeChangesHolder> changes =
        DefaultImmutableMap.nilMap();

    /**
     * @param p_goal the original goal/node
     */
    public NodeChangeJournal(Proof p_proof, Goal p_goal) {
        proof = p_proof;
        node = p_goal.getNode();
        putChangeObj(node, new NodeChangesHolder());
    }


    /**
     * Create an RuleAppInfo object containing all changes stored within this object; remove all
     * listeners
     */
    public RuleAppInfo getRuleAppInfo(RuleApp p_ruleApp) {
        ImmutableList<NodeReplacement> nrs = ImmutableSLList.nil();

        for (final ImmutableMapEntry<@NonNull Node, NodeChangesHolder> entry : changes) {
            final Node newNode = entry.key();
            final Goal newGoal = proof.getOpenGoal(newNode);

            if (newGoal != null) {
                final NodeChangesHolder nc = entry.value();

                nrs = nrs.prepend(new NodeReplacement(newNode, node, nc.scis));

                newGoal.removeGoalListener(this);
            }
        }

        return new RuleAppInfo(p_ruleApp, node, nrs);
    }


    // GoalListener methods

    /**
     * informs the listener about a change that occured to the sequent of goal
     */
    @Override
    public void sequentChanged(Goal source, SequentChangeInfo sci) {
        NodeChangesHolder nc = getChangeObj(source.getNode());

        if (nc != null) {
            nc.addSCI(sci);
        }
    }


    /// Informs the listener that the given goal <code>source</code> has been replaced by the goals
    /// <code>newGoals</code> (note that <code>source</code> may be an element of
    /// <code>newGoals</code>). The nodes of <code>newGoals</code> are children of the node
    /// <code>parent</code>
    @Override
    public void goalReplaced(Goal source, Node parent, ImmutableList<Goal> newGoals) {
        NodeChangesHolder nc = removeChangeObj(parent);

        if (nc != null) {
            Iterator<Goal> it = newGoals.iterator();
            if (it.hasNext()) {
                while (true) {
                    putChangeObj(it.next().getNode(), nc);
                    if (!it.hasNext()) {
                        break;
                    }
                    nc = (NodeChangesHolder) nc.clone();
                }
            }
        }
    }


    private void putChangeObj(Node p_node, NodeChangesHolder p_obj) {
        changes = changes.put(p_node, p_obj);
    }

    private NodeChangesHolder getChangeObj(Node p_node) {
        return changes.get(p_node);
    }

    private NodeChangesHolder removeChangeObj(Node p_node) {
        final NodeChangesHolder res = changes.get(p_node);
        changes = changes.remove(p_node);
        return res;
    }

    @Override
    public void automaticStateChanged(Goal source, boolean oldAutomatic, boolean newAutomatic) {
        // Nothing to do
    }
}
