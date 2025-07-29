/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.rule.VariableConditionAdapter;
import org.key_project.rusty.rule.inst.SVInstantiations;

public class DifferentInstantiationCondition extends VariableConditionAdapter {
    private final SchemaVariable var1, var2;

    public DifferentInstantiationCondition(SchemaVariable var1, SchemaVariable var2) {
        this.var1 = var1;
        this.var2 = var2;
    }

    @Override
    public boolean check(SchemaVariable var, SyntaxElement instCandidate, SVInstantiations svInst,
            Services services) {
        if (var == var1) {
            final Object inst2 = svInst.getInstantiation(var2);
            return inst2 == null || !inst2.equals(instCandidate);
        } else if (var == var2) {
            final Object inst1 = svInst.getInstantiation(var1);
            return inst1 == null || !inst1.equals(instCandidate);
        } else {
            return true;
        }
    }
}
