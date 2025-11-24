/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.termfeature;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Term;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.termfeature.BinaryTermFeature;
import org.key_project.prover.strategy.costbased.termfeature.TermFeature;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.logic.op.IfThenElse;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.logic.op.Quantifier;

public class AtomTermFeature extends BinaryTermFeature {
    public static final TermFeature INSTANCE = new AtomTermFeature();

    private AtomTermFeature() {}

    @Override
    protected boolean filter(Term term, MutableState mState, LogicServices services) {
        final var op = term.op();
        return !(op instanceof Junctor || op == Equality.EQV || op instanceof IfThenElse
                || op instanceof Quantifier);
    }
}
