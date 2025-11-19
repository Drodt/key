/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.prover.impl;

import org.key_project.prover.engine.GoalChooser;
import org.key_project.prover.engine.GoalChooserFactory;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;

import org.jspecify.annotations.NonNull;

public class DepthFirstGoalChooserFactory
        implements GoalChooserFactory<@NonNull Proof, @NonNull Goal> {
    public static final String NAME = "Depth First Goal Chooser";

    public DepthFirstGoalChooserFactory() {}

    public @NonNull GoalChooser<@NonNull Proof, Goal> create() {
        return new DepthFirstGoalChooser();
    }

    public @NonNull GoalChooserFactory<@NonNull Proof, @NonNull Goal> copy() {
        return new DepthFirstGoalChooserFactory();
    }

    public @NonNull String name() {
        return NAME;
    }
}
