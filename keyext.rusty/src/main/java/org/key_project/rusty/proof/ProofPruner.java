/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import javax.swing.*;

import org.key_project.rusty.proof.init.InitConfig;
import org.key_project.rusty.rule.NoPosTacletApp;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

/// This class is responsible for pruning a proof tree at a certain cutting point. It has been
/// introduced to encapsulate the methods that are needed for pruning. Since the class has
/// influence on the internal state of the proof it should not be moved to a new file, in order
/// to restrict the access to it.
public class ProofPruner {
    private final Proof proof;
    private Node firstLeaf = null;

    ProofPruner(Proof proof) {
        this.proof = proof;
    }

    /// prunes the proof at the given node
    ///
    /// @param cuttingPoint the node where to prune
    /// @return the subtrees whose common root was the given `cuttingPoint`
    public ImmutableList<Node> prune(final Node cuttingPoint) {

        // there is only one leaf containing an open goal that is interesting for pruning the
        // subtree of <code>node</code>, namely the first leave that is found by a breadth
        // first search.
        // The other leaves containing open goals are only important for removing the open goals
        // from the open goal list.
        // To that end, those leaves are stored in residualLeaves. For increasing the
        // performance,
        // a tree structure has been chosen, because it offers the operation
        // <code>contains</code> in O(log n).
        final Set<Node> residualLeaves = new TreeSet<>(Comparator.comparingInt(Node::getSerialNr));

        final InitConfig initConfig = proof.getInitConfig();

        // First, make a breadth first search, in order to find the leaf
        // with the shortest distance to the cutting point and to remove
        // the rule applications from the proof management system.
        // Furthermore, store the residual leaves.
        proof.breadthFirstSearch(cuttingPoint, (proof, visitedNode) -> {
            if (visitedNode.leaf()) {
                // pruning in closed branches
                if (firstLeaf == null) {
                    firstLeaf = visitedNode;
                } else {
                    residualLeaves.add(visitedNode);
                }
            }

            if (initConfig != null && visitedNode.parent() != null) {
                proof.mgt().ruleUnApplied(visitedNode.parent().getAppliedRuleApp());
                for (final NoPosTacletApp app : visitedNode.parent()
                        .getLocalIntroducedRules()) {
                    initConfig.getJustifInfo().removeJustificationFor(app.taclet());
                }
            }
        });

        // first leaf is closed -> add as goal and reopen
        final Goal firstGoal =
            firstLeaf.isClosed() ? proof.getClosedGoal(firstLeaf) : proof.getOpenGoal(firstLeaf);
        assert firstGoal != null;
        if (firstLeaf.isClosed()) {
            proof.reOpenGoal(firstGoal);
        }
        // Go from the first leaf that has been found to the cutting point. For each node on the
        // path,
        // remove the local rules from firstGoal that have been added by the considered node.
        proof.traverseFromChildToParent(firstLeaf, cuttingPoint, (proof, visitedNode) -> {
            for (final NoPosTacletApp app : visitedNode.getLocalIntroducedRules()) {
                firstGoal.ruleAppIndex().removeNoPosTacletApp(app);
                proof.getInitConfig().getJustifInfo().removeJustificationFor(app.taclet());
            }

            firstGoal.pruneToParent();

            // final List<StrategyInfoUndoMethod> undoMethods =
            // visitedNode.getStrategyInfoUndoMethods();
            // for (StrategyInfoUndoMethod undoMethod : undoMethods) {
            // firstGoal.undoStrategyInfoAdd(undoMethod);
            // }
        });


        // do some cleaning and refreshing: Clearing indices, caches....
        refreshGoal(firstGoal, cuttingPoint);

        // cut the subtree, it is not needed anymore.
        ImmutableList<Node> subtrees = cut(cuttingPoint);


        // remove the goals of the residual leaves.
        proof.removeOpenGoals(residualLeaves);
        proof.removeClosedGoals(residualLeaves);

        /*
         * this ensures that the open goals are in interactive mode and thus all rules are
         * available in the just pruned goal (see GitLab #1480)
         */
        proof.setRuleAppIndexToInteractiveMode();

        return subtrees;
    }

    private void refreshGoal(Goal goal, Node node) {
        goal.getRuleAppManager().clearCache();
        goal.ruleAppIndex().clearIndexes();
        goal.getNode().setAppliedRuleApp(null);
        node.clearNameCache();

        // delete NodeInfo, but preserve potentially existing branch label
        String branchLabel = node.getNodeInfo().getBranchLabel();
        node.clearNodeInfo();
        if (branchLabel != null) {
            node.getNodeInfo().setBranchLabel(branchLabel);
        }
    }

    private ImmutableList<Node> cut(Node node) {
        ImmutableList<Node> children = ImmutableSLList.nil();
        Iterator<Node> it = node.childrenIterator();

        while (it.hasNext()) {
            children = children.append(it.next());

        }
        for (Node child : children) {
            node.remove(child);
        }
        return children;
    }
}
