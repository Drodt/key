/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.definition;

import org.key_project.rusty.strategy.StrategyProperties;

/// Instances of this factory are used to create default [StrategyProperties] used by a
/// [Strategy] defined via its [StrategySettingsDefinition].
///
/// @author Martin Hentschel
public interface IDefaultStrategyPropertiesFactory {
    /// The default implementation.
    IDefaultStrategyPropertiesFactory DEFAULT_FACTORY =
        StrategyProperties::new;

    /// Creates new default [StrategyProperties].
    ///
    /// @return The new default [StrategyProperties].
    StrategyProperties createDefaultStrategyProperties();
}
