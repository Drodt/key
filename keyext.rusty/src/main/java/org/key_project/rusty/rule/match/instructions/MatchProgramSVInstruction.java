/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.prover.rules.instantiation.IllegalInstantiationException;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

import static org.key_project.rusty.Services.convertToLogicElement;

public class MatchProgramSVInstruction extends MatchSchemaVariableInstruction {

    public MatchProgramSVInstruction(ProgramSV sv) {
        super(sv);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public MatchResultInfo match(
            RustyProgramElement instantiationCandidate,
            MatchResultInfo matchCond,
            LogicServices services) {
        final ProgramSVSort svSort = (ProgramSVSort) op.sort();

        // TODO: will need execution context when we add functions (in the Rust programs)
        if (svSort.canStandFor(instantiationCandidate, (Services) services)) {
            return addInstantiation(instantiationCandidate, matchCond, (Services) services);
        }

        return null;
    }

    /**
     * tries to add the pair <tt>(this,pe)</tt> to the match conditions. If possible the resulting
     * match conditions are returned, otherwise <tt>null</tt>. Such an addition can fail, e.g. if
     * already a pair <tt>(this,x)</tt> exists where <tt>x!=pe</tt>
     */
    private MatchResultInfo addInstantiation(RustyProgramElement pe, MatchResultInfo matchCond,
            Services services) {

        final SVInstantiations instantiations =
            (SVInstantiations) matchCond.getInstantiations();
        final Object inMap = instantiations.getInstantiation(op);

        if (inMap == null) {
            try {
                return matchCond.setInstantiations(instantiations.add(op, pe, services));
            } catch (IllegalInstantiationException e) {

            }
        } else {
            Object peForCompare = pe;
            if (inMap instanceof Term) {
                try {
                    peForCompare = convertToLogicElement(pe, services);
                } catch (RuntimeException re) {
                    return null;
                }
            }
            if (inMap.equals(peForCompare)) {
                return matchCond;
            }
        }
        return null;
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        MatchResultInfo result = null;
        if (actualElement instanceof RustyProgramElement programElement) {
            result = match(programElement, mc, services);
        } else if (actualElement instanceof Term term) {
            final ProgramSVSort svSort = (ProgramSVSort) op.sort();
            if (svSort.canStandFor(term)) {
                return addInstantiation(term, mc, services);
            }
        }
        return result;
    }
}
