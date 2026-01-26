/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.prover.impl;

import org.key_project.prover.engine.GoalChooser;
import org.key_project.prover.engine.GoalChooserFactory;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;

import org.jspecify.annotations.NonNull;

/// creates the default goal chooser used in KeY
public class DefaultGoalChooserFactory
        implements GoalChooserFactory<@NonNull Proof, @NonNull Goal> {
    public static final String NAME = "Simple Goal Chooser";

    public DefaultGoalChooserFactory() {}

    public @NonNull GoalChooser<@NonNull Proof, @NonNull Goal> create() {
        return new DefaultGoalChooser();
    }

    public @NonNull String name() {
        return NAME;
    }

    public GoalChooserFactory<@NonNull Proof, @NonNull Goal> copy() {
        return new DefaultGoalChooserFactory();
    }
}
