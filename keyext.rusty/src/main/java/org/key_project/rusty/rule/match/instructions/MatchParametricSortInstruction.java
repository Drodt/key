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
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.getMatchInstructionForGenericArgument;

/// Matches a parametric sort instance
public final class MatchParametricSortInstruction implements MatchInstruction {
    private final ParametricSortDecl base;
    private final MatchInstruction[] argMatchers;

    MatchParametricSortInstruction(ParametricSortInstance psi) {
        this.base = psi.getBase();
        this.argMatchers = new MatchInstruction[psi.getArgs().size()];
        int i = 0;
        for (GenericArgument arg : psi.getArgs()) {
            argMatchers[i++] = getMatchInstructionForGenericArgument(arg);
        }
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        if (!(actualElement instanceof SortArg(Sort sort))
                || !(sort instanceof ParametricSortInstance source)
                || source.getBase() != base) {
            return null;
        }
        MatchResultInfo r = mc;
        int i = 0;
        // same base implies same arity (asserted in ParametricSortInstance.get)
        for (GenericArgument sourceArg : source.getArgs()) {
            r = argMatchers[i++].match(sourceArg, r, services);
            if (r == null) {
                return null;
            }
        }
        return r;
    }
}
