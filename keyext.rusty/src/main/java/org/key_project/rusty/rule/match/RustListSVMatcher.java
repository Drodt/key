/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import java.util.List;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.prover.rules.matcher.compiler.ListSVMatcher;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

/// Java-DL implementation of the [ListSVMatcher] SPI (service provider interface): a source
/// child may join a list schema
/// variable's run if it is admissible for the schema variable's sort
/// (`ProgramSVSort.canStandFor`, judged in the execution context recorded with the
/// instantiations), and the collected run is stored as an `ImmutableList` of program
/// elements; if the schema variable is already bound, the match instead succeeds without change
/// exactly when the existing list equals the run. (The interpreter back-end matches list schema
/// variables with `ProgramSV.matchListSV`; both must yield identical results.)
public final class RustListSVMatcher implements ListSVMatcher {
    /// stateless; a single shared instance suffices.
    public static final RustListSVMatcher INSTANCE = new RustListSVMatcher();

    /// the instantiation of a list schema variable that matched no source children
    private static final ImmutableList<RustyProgramElement> EMPTY_LIST_INSTANTIATION =
        ImmutableList.of();

    private RustListSVMatcher() {}

    @Override
    public boolean isAdmissible(SchemaVariable listSV, SyntaxElement element, MatchResultInfo mc,
            LogicServices services) {
        return ((ProgramSVSort) ((ProgramSV) listSV).sort()).canStandFor(
            (RustyProgramElement) element,
            (Services) services);
    }

    @Override
    public @Nullable MatchResultInfo bindRun(SchemaVariable listSV,
            List<? extends SyntaxElement> run, MatchResultInfo mc, LogicServices services) {
        final ImmutableList<RustyProgramElement> list = run.isEmpty() ? EMPTY_LIST_INSTANTIATION
                : ImmutableList.of(run.toArray(new RustyProgramElement[0]));
        final MatchConditions matchCond = (MatchConditions) mc;
        SVInstantiations insts = matchCond.getInstantiations();
        final ImmutableList<RustyProgramElement> pl = insts.getInstantiation(listSV);
        if (pl != null) {
            return pl.equals(list) ? matchCond : null;
        }
        insts = insts.add(listSV, list, RustyProgramElement.class, services);
        return insts == null ? null : matchCond.setInstantiations(insts);
    }
}
