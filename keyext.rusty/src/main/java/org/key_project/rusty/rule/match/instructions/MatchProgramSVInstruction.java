/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Term;
import org.key_project.prover.rules.instantiation.IllegalInstantiationException;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.logic.op.sv.ProgramSV;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/// Matches a (single, non-list) program schema variable: the candidate (a program element, or a
/// term when the schema variable stands inside an expression) must be admissible for the schema
/// variable's [ProgramSVSort] (`canStandFor`), and must agree with any instantiation
/// the schema variable already has (a program element is converted to its logic term for that
/// comparison where needed).
public class MatchProgramSVInstruction extends MatchSchemaVariableInstruction {
    private static final Logger LOGGER = LoggerFactory.getLogger(MatchProgramSVInstruction.class);

    public MatchProgramSVInstruction(ProgramSV sv) {
        super(sv);
    }

    /// tries to add the pair `(this,pe)` to the match conditions. If possible the resulting
    /// match conditions are returned, otherwise `null`. Such an addition can fail, e.g. if
    /// already a pair `(this,x)` exists where `x!=pe`
    protected MatchResultInfo addInstantiation(RustyProgramElement pe, MatchResultInfo matchCond,
            LogicServices services) {

        final SVInstantiations instantiations = (SVInstantiations) matchCond.getInstantiations();
        final Object inMap = instantiations.getInstantiation(op);

        if (inMap == null) {
            try {
                return matchCond.setInstantiations(instantiations.add(op, pe, services));
            } catch (IllegalInstantiationException e) {
                LOGGER.debug("Exception thrown by class Taclet at setInstantiations");
            }
        } else {
            Object peForCompare = pe;
            if (inMap instanceof Term) {
                try {
                    peForCompare = Services.convertToLogicElement(pe, (Services) services);
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

    /// {@inheritDoc}
    @Override
    public MatchResultInfo match(RustyProgramElement instantiationCandidate,
            MatchResultInfo matchCond,
            LogicServices services) {
        final ProgramSVSort svSort = (ProgramSVSort) op.sort();

        final SVInstantiations instantiations = (SVInstantiations) matchCond.getInstantiations();
        if (!svSort.canStandFor(instantiationCandidate, (Services) services)) {
            return null;
        }
        // A schema variable that is already bound matches a program element again only if the
        // element IS its binding: the bound program element itself, or -- for a term binding --
        // the term's operator (so a bound term re-matches only the program variable it consists
        // of). This is the same rule as in ProgramSV.match, keeping this instruction and the AST
        // matcher in agreement.
        final Object instant = instantiations.getInstantiation(op);
        if (instant == null || instant.equals(instantiationCandidate)
                || (instant instanceof Term t && t.op().equals(instantiationCandidate))) {
            return addInstantiation(instantiationCandidate, matchCond, services);
        }
        return null;
    }

    /// A term candidate matches when the schema variable's sort can stand for the term (a program
    /// schema variable may occur at an expression position and then binds a term).
    @Override
    protected MatchResultInfo match(Term instantiationCandidate, MatchResultInfo mc,
            LogicServices services) {
        final ProgramSVSort svSort = (ProgramSVSort) op.sort();
        if (svSort.canStandFor(instantiationCandidate)) {
            return addInstantiation(instantiationCandidate, mc, services);
        }
        return null;
    }
}
