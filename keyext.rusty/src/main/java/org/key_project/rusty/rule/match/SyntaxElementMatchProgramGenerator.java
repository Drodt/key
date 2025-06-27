/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match;

import java.util.ArrayList;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.rules.matcher.vm.VMProgramInterpreter;
import org.key_project.prover.rules.matcher.vm.instruction.MatchInstruction;
import org.key_project.prover.rules.matcher.vm.instruction.VMInstruction;
import org.key_project.rusty.logic.SVPlace;
import org.key_project.rusty.logic.op.*;
import org.key_project.rusty.logic.op.sv.ModalOperatorSV;
import org.key_project.rusty.logic.sort.GenericSort;

import static org.key_project.rusty.rule.match.instructions.RustyDLMatchInstructionSet.*;

/**
 * This class generates a matching program for a given syntax element that can be
 * interpreted by the virtual machine's interpreter
 *
 * @see VMProgramInterpreter
 */
public class SyntaxElementMatchProgramGenerator {
    /**
     * creates a matcher for the given pattern
     *
     * @param pattern the {@link Term} specifying the pattern
     * @return the specialized matcher for the given pattern
     */
    public static VMInstruction[] createProgram(Term pattern) {
        ArrayList<VMInstruction> program = new ArrayList<>();
        createProgram(pattern, program);
        return program.toArray(new VMInstruction[0]);
    }

    /**
     * creates a matching program for the given pattern. It appends the necessary match instruction
     * to the given list of instructions
     *
     * @param pattern the {@link Term} used as pattern for which to create a matcher
     * @param program the list of {@link MatchInstruction} to which the instructions for matching
     *        {@code pattern} are added.
     */
    private static void createProgram(Term pattern, ArrayList<VMInstruction> program) {
        final Operator op = pattern.op();

        final var boundVars = pattern.boundVars();

        if (op instanceof SchemaVariable sv) {
            program.add(getMatchInstructionForSV(sv));
            program.add(gotoNextSiblingInstruction());
        } else {
            program.add(getCheckNodeKindInstruction(Term.class));
            program.add(gotoNextInstruction());
            if (op instanceof SortDependingFunction sortDependingFunction) {
                program.add(getCheckNodeKindInstruction(SortDependingFunction.class));
                program.add(getSimilarSortDependingFunctionInstruction(sortDependingFunction));
                program.add(gotoNextInstruction());
                if (sortDependingFunction.getSortDependingOn() instanceof GenericSort gs) {
                    program.add(getMatchGenericSortInstruction(gs));
                } else {
                    program.add(getMatchIdentityInstruction(sortDependingFunction.getChild(0)));
                }
                program.add(gotoNextInstruction());
            } else if (op instanceof ElementaryUpdate elUp) {
                program.add(getCheckNodeKindInstruction(ElementaryUpdate.class));
                program.add(gotoNextInstruction());
                if (elUp.lhs() instanceof SchemaVariable sv) {
                    program.add(getMatchInstructionForSV(sv));
                    program.add(gotoNextSiblingInstruction());
                } else if (elUp.lhs() instanceof ProgramVariable pv) {
                    program.add(getMatchIdentityInstruction(pv));
                    program.add(gotoNextInstruction());
                }
            } else if (op instanceof RModality mod) {
                program.add(getCheckNodeKindInstruction(RModality.class));
                program.add(gotoNextInstruction());
                if (mod.kind() instanceof ModalOperatorSV modKindSV) {
                    program.add(matchModalOperatorSV(modKindSV));
                } else {
                    program.add(getMatchIdentityInstruction(mod.kind()));
                }
                program.add(gotoNextInstruction());
                program.add(matchProgram(mod.programBlock().program()));
                program.add(gotoNextSiblingInstruction());
            } else if (op instanceof MutRef mr && mr.getPlace() instanceof SVPlace sv) {
                program.add(matchPlaceSV(sv));
                program.add(gotoNextSiblingInstruction());
            } else {
                program.add(getMatchIdentityInstruction(op));
                program.add(gotoNextInstruction());
            }
        }

        if (!boundVars.isEmpty()) {
            for (int i = 0; i < boundVars.size(); i++) {
                program.add(matchAndBindVariable(boundVars.get(i)));
                program.add(gotoNextSiblingInstruction());
            }
        }

        for (int i = 0; i < pattern.arity(); i++) {
            createProgram(pattern.sub(i), program);
        }
    }
}
