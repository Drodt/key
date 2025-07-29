/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

public interface ParamSortArg extends SyntaxElement {
    default ParamSortArg instantiateParamArg(SVInstantiations svInst, Services services) {
        if (this instanceof SortArg sa) {
            if (sa.sort() instanceof GenericSort gs) {
                return new SortArg(svInst.getGenericSortInstantiations().getRealSort(gs));
            } else if (sa.sort() instanceof ParametricSortInstance psi) {
                ImmutableList<ParamSortArg> args = ImmutableSLList.nil();

                for (int i = psi.getArgs().size() - 1; i >= 0; i--) {
                    args = args.prepend(psi.getArgs().get(i).instantiateParamArg(svInst, services));
                }

                return new SortArg(ParametricSortInstance.get(psi.getBase(), args));
            } else {
                return sa;
            }
        } else if (this instanceof TermArg ta) {
            if (ta.term().op() instanceof SchemaVariable sv) {
                Term inst = svInst.getTermInstantiation(sv, services);
                return new TermArg(inst);
            } else {
                return ta;
            }
        } else {
            throw new IllegalArgumentException("Unsupported argument type: " + getClass());
        }
    }
}
