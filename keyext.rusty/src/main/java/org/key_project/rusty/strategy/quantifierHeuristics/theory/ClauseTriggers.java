/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.Term;

/// What trigger selection found for one clause: the triggers of each literal, and whether
/// elements of several literals combined into a covering multi-trigger.
///
/// A clause is covered if some literal has a covering trigger or a covering multi-trigger was
/// built. Every covering trigger binds the formula's first variable, so the formula is
/// instantiated exactly if some clause is covered; a formula with no covered clause is the case
/// [TriggerSupport#fallbackTriggers] exists for.
///
/// @param clause the clause as trigger selection read it
/// @param literals the triggers found per literal, in the order of `clause.literals()`
/// @param multiCovered whether a covering multi-trigger was built from the literals' elements
public record ClauseTriggers(ClauseAnalysis clause, List<LiteralTriggers> literals,
        boolean multiCovered) {
    /// Whether some literal has a covering trigger or a covering multi-trigger was built.
    public boolean covered() {
        if (multiCovered) {
            return true;
        }
        for (final LiteralTriggers literal : literals) {
            if (!literal.covering().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /// The literals that yielded neither a covering trigger nor an element.
    public List<Term> literalsWithoutTrigger() {
        final List<Term> result = new ArrayList<>();
        for (final LiteralTriggers literal : literals) {
            if (literal.isEmpty()) {
                result.add(literal.literal());
            }
        }
        return result;
    }
}
