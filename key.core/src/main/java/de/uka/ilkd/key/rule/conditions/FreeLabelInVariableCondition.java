/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.conditions;

import de.uka.ilkd.key.java.Label;
import de.uka.ilkd.key.java.ProgramElement;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.visitor.FreeLabelFinder;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;


public final class FreeLabelInVariableCondition extends VariableConditionAdapter {

    private final SchemaVariable label;
    private final SchemaVariable statement;
    private final boolean negated;
    private final FreeLabelFinder freeLabelFinder = new FreeLabelFinder();

    public FreeLabelInVariableCondition(SchemaVariable label, SchemaVariable statement,
            boolean negated) {
        this.label = label;
        this.statement = statement;
        this.negated = negated;
    }


    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations instMap,
            Services services) {
        Label prgLabel = null;
        ProgramElement program = null;

        if (var == label) {
            prgLabel = (Label) instCandidate;
            program = (ProgramElement) instMap.getInstantiation(statement);
        } else if (var == statement) {
            prgLabel = (Label) instMap.getInstantiation(label);
            program = (ProgramElement) instCandidate;
        }

        if (program == null || prgLabel == null) {
            // not yet complete or not responsible
            return true;
        }

        final boolean freeIn = freeLabelFinder.findLabel(prgLabel, program);
        return negated != freeIn;
    }


    @Override
    public String toString() {
        return (negated ? "\\not" : "") + "\\freeLabelIn (" + label.name() + "," + statement.name()
            + ")";
    }
}
