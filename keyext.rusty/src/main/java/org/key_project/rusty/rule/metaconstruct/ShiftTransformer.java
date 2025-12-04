/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.rule.metaconstruct;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.Shift;
import org.key_project.rusty.logic.op.AbstractTermTransformer;
import org.key_project.rusty.rule.inst.SVInstantiations;


public class ShiftTransformer extends AbstractTermTransformer {

    public ShiftTransformer() {
        super(new Name("#logicShift"), 2);
    }

    @Override
    public Term transform(Term term, SVInstantiations svInst, Services services) {
        final Term target =
            svInst.getTermInstantiation((SchemaVariable) term.sub(1).op(), services);
        final Shift shift = new Shift(getDistance(term, services), services.getTermBuilder());

        return shift.apply(target);
    }

    public int getDistance(Term term, Services services) {
        final Term shiftDistance = term.sub(0);
        // we know there could be an overflow, but we want to see that real world example ...
        final int distance = Integer.parseInt(convertToDecimalString(shiftDistance, services));
        return distance;
    }
}
