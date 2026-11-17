/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy;

import org.key_project.prover.strategy.Strategy;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.proof.Proof;
import org.key_project.rusty.settings.ProofSettings;

/// Generic interface for evaluating the cost of a RuleApp with regard to a specific strategy
public interface RustStrategy extends Strategy<Goal> {
    /// Updates the [RustStrategy] for the given [Proof] by setting the
    /// [RustStrategy]'s
    /// [StrategyProperties] to the given ones.
    ///
    /// @param proof The [Proof] the strategy of which should be updated.
    /// @param p The new [StrategyProperties]
    static void updateStrategySettings(Proof proof, StrategyProperties p) {
        final org.key_project.prover.strategy.Strategy<Goal> strategy =
            proof.getActiveStrategy();
        ProofSettings.DEFAULT_SETTINGS.getStrategySettings().setStrategy(strategy.name());
        ProofSettings.DEFAULT_SETTINGS.getStrategySettings().setActiveStrategyProperties(p);

        proof.getSettings().getStrategySettings().setStrategy(strategy.name());
        proof.getSettings().getStrategySettings().setActiveStrategyProperties(p);

        proof.setActiveStrategy(strategy);
    }
}
