/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.prover.impl;

import org.key_project.rusty.proof.Goal;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// Helper class for managing a list of goals on which rules are applied. The class provides methods
/// for removing a goal, and for updating the internal data structures after a rule has been
/// applied.
public class DepthFirstGoalChooser extends DefaultGoalChooser {
    public @Nullable Goal getNextGoal() {
        Goal result;

        if (allGoalsSatisfiable) {
            if (nextGoals.isEmpty()) {
                nextGoals = selectedList;
            }

            if (nextGoals.isEmpty()) {
                result = null;
            } else {
                do {
                    result = nextGoals.head();
                    nextGoals = nextGoals.tail();
                } while (result != null && !result.isAutomatic());
            }
        } else {
            ++nextGoalCounter;
            do {
                result = selectedList.isEmpty() ? null : selectedList.head();
                if (result != null && !result.isAutomatic()) {
                    selectedList = selectedList.tail();
                }
            } while (result != null && !result.isAutomatic());
        }
        return result;
    }

    protected ImmutableList<Goal> insertNewGoals(ImmutableList<Goal> newGoals,
            ImmutableList<Goal> prevGoalList) {

        for (final Goal g : newGoals) {
            if (proof.openGoals().contains(g)) {
                prevGoalList = prevGoalList.prepend(g);
            }
        }
        return prevGoalList;
    }

    @Override
    protected void updateGoalListHelp(Object node, ImmutableList<Goal> newGoals) {
        ImmutableList<Goal> prevGoalList = ImmutableSLList.nil();
        boolean newGoalsInserted = false;

        nextGoals = ImmutableSLList.nil();

        // Remove "node" and goals contained within "newGoals"
        while (!selectedList.isEmpty()) {
            final @NonNull Goal goal = selectedList.head();
            selectedList = selectedList.tail();

            if (node == goal.getNode() || newGoals.contains(goal)) {
                // continue taclet apps at the next goal in list
                nextGoals = selectedList;

                if (!newGoalsInserted) {
                    prevGoalList = insertNewGoals(newGoals, prevGoalList);
                    newGoalsInserted = true;
                }
            } else {
                prevGoalList = prevGoalList.append(goal);
            }
        }

        while (!prevGoalList.isEmpty()) {
            selectedList = selectedList.append(prevGoalList.head());
            prevGoalList = prevGoalList.tail();
        }
    }
}
