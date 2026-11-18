/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import java.util.List;

import org.key_project.logic.Term;
import org.key_project.rusty.strategy.quantifierHeuristics.Trigger;

/// The triggers that trigger selection found in one literal of a clause.
///
/// A covering trigger binds every universal variable of the clause on its own. An element binds
/// only some of them and becomes a trigger only together with elements of other literals, as a
/// covering multi-trigger. A literal may yield both, either, or neither.
///
/// @param literal the literal, negation stripped, as listed in the clause's
/// [ClauseAnalysis]
/// @param covering the triggers of the literal that bind every universal variable of the clause
/// @param elements the triggers of the literal that bind only some of them
public record LiteralTriggers(Term literal, List<Trigger> covering, List<Trigger> elements) {
    /// Whether the literal yielded neither a covering trigger nor an element.
    public boolean isEmpty() {
        return covering.isEmpty() && elements.isEmpty();
    }
}
