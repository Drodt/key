/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.quantifierHeuristics;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.util.collection.ImmutableSet;

public interface Trigger {
    /// @param targetTerm
    /// @param services
    /// @return all substitution that found from the targeTerm by matching this trigger to
    /// targeTerm.
    ImmutableSet<Substitution> getSubstitutionsFromTerms(
            ImmutableSet<Term> targetTerm, Services services);

    Term getTriggerTerm();
}
