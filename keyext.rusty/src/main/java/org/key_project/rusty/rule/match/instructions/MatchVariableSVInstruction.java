/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.match.instructions;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.logic.op.sv.VariableSV;

import org.jspecify.annotations.Nullable;


public class MatchVariableSVInstruction
        extends MatchSchemaVariableInstruction {
    protected MatchVariableSVInstruction(VariableSV op) {
        super(op);
    }

    @Override
    public @Nullable MatchResultInfo match(SyntaxElement actualElement, MatchResultInfo mc,
            LogicServices services) {
        final Term actualTerm = (Term) actualElement;
        if (actualTerm.op() instanceof QuantifiableVariable qv) {
            final Term foundMapping = mc.getInstantiations().getInstantiation(op);
            if (foundMapping == null) {
                return addInstantiation(actualTerm, mc, services);
            } else if (foundMapping.op() == qv) {
                return mc;
            }
        }
        return null;
    }
}
