/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.prover.impl;

import org.key_project.prover.engine.TaskFinishedInfo;
import org.key_project.rusty.proof.Proof;

/// A concrete implementation of the [TaskFinishedInfo] interface. This class holds
/// additional information about a task that has been completed, including the source,
/// result, proof, execution time, the number of applied rules, and closed goals.
///
/// This implementation is used to track task completion details about the proof search.
///
///
/// The task result may either be an exception (e.g., a [Throwable]) or
/// specific information about the proof search (strategy execution, proof macros or similar).
///
public class DefaultTaskFinishedInfo implements TaskFinishedInfo {
    /// The task that has finished. This can be one of several possible sources:
    ///
    /// - [ApplyStrategy]
    /// - `KeYMediator` (when pruning)
    /// - [AbstractProblemLoader] (when loading a proof)
    private final Object source;

    /// The result of the task, which can be either a Throwable or ApplyStrategyInfo.
    private final Object result;

    /// The proof the task worked on.
    private final Proof proof;

    /// Time taken to complete the task, in milliseconds.
    private final long timeInMillis;

    /// Number of rules applied during the task.
    private final int appliedRules;

    /// Number of goals closed during the task.
    private final int closedGoals;


    /// Constructs a new [DefaultTaskFinishedInfo] object with the provided details.
    ///
    /// @param source The source object that initiated the task.
    /// @param result The result of the task execution.
    /// @param proof The proof worked on by the task.
    /// @param time The time taken for the task to complete, in milliseconds.
    /// @param appliedRules The number of rules applied during the task.
    /// @param closedGoals The number of goals closed during the task.
    public DefaultTaskFinishedInfo(Object source, Object result, Proof proof, long time,
            int appliedRules, int closedGoals) {
        this.source = source;
        this.result = result;
        this.proof = proof;
        this.timeInMillis = time;
        this.appliedRules = appliedRules;
        this.closedGoals = closedGoals;
    }

    /// {@inheritDoc}
    @Override
    public long getTime() {
        return timeInMillis;
    }

    /// {@inheritDoc}
    @Override
    public Object getResult() {
        return result;
    }

    /// {@inheritDoc}
    @Override
    public Object getSource() {
        return source;
    }

    /// {@inheritDoc}
    @Override
    public int getAppliedRules() {
        return appliedRules;
    }

    /// {@inheritDoc}
    @Override
    public int getClosedGoals() {
        return closedGoals;
    }

    /// {@inheritDoc}
    @Override
    public Proof getProof() {
        return proof;
    }

    /// Returns a string representation of the task's status. This message is typically used
    /// for displaying in a status bar.
    ///
    /// The message includes details about the number of rules applied, the time taken,
    /// the number of goals closed, and the number of remaining open goals.
    ///
    ///
    /// @return A status message summarizing the task's execution details.
    @Override
    public String toString() {
        // if (proof.isDisposed()) {
        // return "Proof disposed";
        // }
        if (appliedRules != 0) {
            StringBuilder message = new StringBuilder();
            String timeString = (timeInMillis / 1000) + "." + ((timeInMillis % 1000) / 100);

            message.append("Strategy: Applied ").append(appliedRules).append(" rule");
            if (appliedRules != 1) {
                message.append("s");
            }
            message.append(" (").append(timeString).append(" sec), ");
            message.append(" closed ").append(closedGoals).append(" goal");
            if (closedGoals != 1) {
                message.append("s");
            }
            message.append(", ").append(proof.openGoals().size());
            message.append(" remaining");
            return message.toString();
        } else {
            return "No rules applied";
        }
    }
}
