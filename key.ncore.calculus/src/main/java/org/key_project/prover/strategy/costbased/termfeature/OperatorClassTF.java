/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.prover.strategy.costbased.termfeature;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.prover.strategy.costbased.MutableState;

/// Term feature for checking whether the top operator of a term has an instance of a certain class
public class OperatorClassTF extends BinaryTermFeature {

    private final Class<? extends Operator> opClass;

    private OperatorClassTF(Class<? extends Operator> op) {
        this.opClass = op;
    }

    public static TermFeature create(Class<? extends Operator> op) {
        return new OperatorClassTF(op);
    }

    @Override
    protected boolean filter(Term term, MutableState mState, LogicServices services) {
        return opClass.isInstance(term.op());
    }
}
