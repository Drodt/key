/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.conditions;


import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.op.ProgramSV;
import de.uka.ilkd.key.logic.op.ProgramVariable;
import de.uka.ilkd.key.rule.VariableConditionAdapter;
import de.uka.ilkd.key.rule.inst.SVInstantiations;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;

/**
 * Ensures the given ProgramElement denotes a local variable
 */
public final class LocalVariableCondition extends VariableConditionAdapter {

    private final SchemaVariable var;
    private final boolean neg;

    public LocalVariableCondition(SchemaVariable var, boolean neg) {
        this.var = var;
        this.neg = neg;
        if (!(var instanceof ProgramSV)) {
            throw new IllegalArgumentException("Illegal schema variable");
        }
    }


    @Override
    public boolean check(SchemaVariable var, SyntaxElement candidate, SVInstantiations svInst,
            Services services) {

        if (var != this.var) {
            return true;
        }
        final boolean isLocalVar =
            ((candidate instanceof ProgramVariable) && !((ProgramVariable) candidate).isMember());
        return neg != isLocalVar;
    }


    @Override
    public String toString() {
        return "\\isLocalVariable (" + var + ")";
    }
}
