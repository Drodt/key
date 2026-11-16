/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.rusty.rule.inst.GenericSortCondition;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.rusty.rule.inst.SortException;

/// Matches a generic sort of the pattern against the concrete sort found in the source (the sort
/// of a parametric function's [SortArg]) by
/// recording an identity [GenericSortCondition] in the instantiations: the generic sort must
/// be (or already have been) instantiated with exactly that concrete sort.
public class MatchGenericSortInstruction implements MatchInstruction {
    private final GenericSort genericSortOfOp;

    public MatchGenericSortInstruction(GenericSort sort) {
        this.genericSortOfOp = sort;
    }

    /// Matches this instruction's generic sort against the given concrete sort by adding the
    /// corresponding identity condition. If that is consistent with the instantiations so far the
    /// resulting match conditions are returned, otherwise `null`.
    ///
    /// @param dependingSortToMatch the concrete [Sort] found in the source
    /// @param matchConditions the [MatchResultInfo] accumulated so far
    /// @return the resulting [MatchResultInfo], or `null` on failure
    private MatchResultInfo matchSorts(Sort dependingSortToMatch, MatchResultInfo matchConditions,
            LogicServices services) {
        // the source sort may itself be generic: proving a taclet correct matches schematic
        // patterns against schematic terms
        final GenericSortCondition c =
            GenericSortCondition.createIdentityCondition(genericSortOfOp, dependingSortToMatch);
        try {
            final SVInstantiations instantiations =
                (SVInstantiations) matchConditions.getInstantiations();
            return matchConditions.setInstantiations(instantiations.add(c, services));
        } catch (SortException e) {
            return null;
        }
    }

    /// {@inheritDoc}
    @Override
    public MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        if (actualElement instanceof SortArg(Sort sort)) {
            return matchSorts(sort, mc, services);
        }
        return null;
    }
}
