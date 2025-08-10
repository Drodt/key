/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.op.sv.OperatorSV;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.rusty.logic.sort.TermArg;
import org.key_project.rusty.rule.inst.GenericSortCondition;
import org.key_project.rusty.rule.inst.SVInstantiations;

import org.jspecify.annotations.Nullable;

public class RustTypeToParametricSortCondition implements VariableCondition {
    private final OperatorSV exprOrTypeSV;
    private final ParametricSortInstance psi;

    public RustTypeToParametricSortCondition(OperatorSV exprOrTypeSV, ParametricSortInstance psi) {
        this.exprOrTypeSV = exprOrTypeSV;
        this.psi = psi;
    }

    @Override
    public String toString() {
        return "\\hasSort(" + exprOrTypeSV + ", "
            + psi + ")";
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != exprOrTypeSV) {
            return matchCond;
        }

        var inst = (SVInstantiations) matchCond.getInstantiations();
        Services services = (Services) lServices;

        Sort sort;
        if (svSubst instanceof Term t) {
            sort = t.sort();
        } else if (svSubst instanceof RustType rt) {
            sort = services.getRustInfo().getKeYRustyType(rt.type()).getSort();
        } else {
            final var expr = (Expr) svSubst;
            sort = services.getRustInfo().getKeYRustyType(expr.type(services)).getSort();
        }

        if (!(sort instanceof ParametricSortInstance psi) || psi.getBase() != this.psi.getBase()) {
            return null;
        }

        inst = checkRecursive(psi, this.psi, inst, services);

        if (inst == null) {
            return null;
        }

        return matchCond.setInstantiations(inst);
    }

    private SVInstantiations checkRecursive(ParametricSortInstance instantiatedPsi,
            ParametricSortInstance genericPsi, SVInstantiations inst, Services services) {
        for (int i = 0; i < instantiatedPsi.getArgs().size(); i++) {
            if (inst == null)
                return null;
            var iArg = instantiatedPsi.getArgs().get(i);
            var arg = genericPsi.getArgs().get(i);
            if (arg instanceof SortArg sa) {
                var isa = (SortArg) iArg;
                if (sa.sort() instanceof GenericSort gs) {
                    inst = inst.add(GenericSortCondition.createIdentityCondition(gs, isa.sort()),
                        services);
                } else if (sa.sort() instanceof ParametricSortInstance gPsi1) {
                    if (!(isa.sort() instanceof ParametricSortInstance iPsi1)) {
                        return null;
                    }
                    inst = checkRecursive(iPsi1, gPsi1, inst, services);
                }
            } else if (arg instanceof TermArg ta) {
                var ita = (TermArg) iArg;
                if (ta.term().op() instanceof SchemaVariable sv) {
                    inst = inst.add(sv, ita.term(), services);
                }
            }
        }
        return inst;
    }
}
