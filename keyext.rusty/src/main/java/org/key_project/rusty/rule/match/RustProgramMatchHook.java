/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match;

import org.key_project.prover.rules.matcher.compiler.ProgramMatchHook;
import org.key_project.prover.rules.matcher.compiler.ProgramMatchPlan;
import org.key_project.prover.rules.matcher.vm.MatchProgram;
import org.key_project.prover.rules.matcher.vm.instruction.VMInstruction;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.RustyBlock;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchVMInstructionSet.matchProgram;

/// Java-DL implementation of the [ProgramMatchHook] SPI (service provider interface): it
/// matches the
/// `RustyBlock` program of a modality. The compiled side is the single-source dispatch's plan
/// ([RustProgramMatchPlanBuilder#buildProgramPlan]) and nothing else; it calls no AST
/// `match` method; a program the dispatch does not describe yields no hook, and the taclet
/// fails to load with the framework's clear error.
public final class RustProgramMatchHook implements ProgramMatchHook {
    private final RustyProgramElement prog;
    private final MatchProgram compiled;

    private RustProgramMatchHook(RustyProgramElement prog,
            MatchProgram compiled) {
        this.prog = prog;
        this.compiled = compiled;
    }

    /// @param prog the modality's program pattern
    public static @Nullable RustProgramMatchHook of(RustyProgramElement prog) {
        final ProgramMatchPlan plan = RustProgramMatchPlanBuilder.buildProgramPlan(prog);
        if (plan == null || plan.listSV() != null) {
            return null;
        }
        // the plan matches the program element; the modality skeleton hands over the RUstyBlock
        final MatchProgram ps = plan.compile();
        final MatchProgram compiled =
            (block, mc, services) -> ps.match(((RustyBlock) block).program(), mc, services);
        return new RustProgramMatchHook(prog, compiled);
    }

    @Override
    public VMInstruction programInstruction() {
        return matchProgram(prog);
    }

    @Override
    public MatchProgram compileProgram() {
        return compiled;
    }
}
