/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
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

import org.jspecify.annotations.Nullable;

public class MatchGenericSortInstruction implements MatchInstruction {
    private final GenericSort genericSortOfOp;

    public MatchGenericSortInstruction(GenericSort sort) {
        this.genericSortOfOp = sort;
    }

    /// matches the depending sort of this instructions sort depending function against the given
    /// sort. If a match is possible the resulting match conditions are returned otherwise
    /// `null` is returned.
    ///
    /// @param dependingSortToMatch the depending [Sort] of the concrete function to be matched
    /// @param matchConditions the [MatchResultInfo] accumulated so far
    /// @return <code>null</code> if failed the resulting match conditions otherwise the resulting
    /// [MatchResultInfo]
    private MatchResultInfo matchSorts(Sort dependingSortToMatch, MatchResultInfo matchConditions,
            LogicServices services) {
        // This restriction has been dropped for free generic sorts to prove taclets correct
        // assert !(s2 instanceof GenericSort)
        // : "Sort s2 is not allowed to be of type generic.";
        MatchResultInfo result;
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

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        if (actualElement instanceof SortArg sa) {
            return matchSorts(sa.sort(), mc, services);
        }
        return null;
    }
}
