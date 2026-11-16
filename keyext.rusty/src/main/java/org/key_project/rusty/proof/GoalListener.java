/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.proof;

import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.util.collection.ImmutableList;

/// interface to be implemented by a goal listener
public interface GoalListener {

    /// informs the listener about a change that occured to the sequent of goal
    void sequentChanged(Goal source, SequentChangeInfo sci);

    /// Informs the listener that the given goal `source` has been replaced by the goals
    /// `newGoals` (note that `source` may be an element of
    /// `newGoals`). The nodes of `newGoals` are children of the node
    /// `parent`
    void goalReplaced(Goal source, Node parent, ImmutableList<Goal> newGoals);

    /// Informs the listener that the automatic state [Goal#isAutomatic()] has changed.
    ///
    /// @param source The changed [Goal].
    /// @param oldAutomatic The old state.
    /// @param newAutomatic The new state.
    void automaticStateChanged(Goal source, boolean oldAutomatic, boolean newAutomatic);
}
