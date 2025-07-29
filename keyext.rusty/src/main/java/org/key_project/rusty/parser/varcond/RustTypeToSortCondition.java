/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.varcond;

import org.key_project.logic.LogicServices;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.OperatorSV;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.VariableCondition;
import org.key_project.prover.rules.instantiation.MatchResultInfo;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.rusty.rule.inst.GenericSortCondition;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.rusty.rule.inst.SortException;

import org.jspecify.annotations.Nullable;

/// Variable condition that enforces a given generic sort to be instantiated with the sort of a
/// program expression a schema variable is instantiated with
public final class RustTypeToSortCondition implements VariableCondition {
    private final OperatorSV exprOrTypeSV;
    private final GenericSort sort;

    public RustTypeToSortCondition(OperatorSV exprOrTypeSV, GenericSort sort) {
        this.exprOrTypeSV = exprOrTypeSV;
        this.sort = sort;

        if (!checkSortedSV(exprOrTypeSV)) {
            throw new RuntimeException("Expected a program schemavariable for expressions");
        }
    }

    public static boolean checkSortedSV(final OperatorSV exprOrTypeSV) {
        final Sort svSort = exprOrTypeSV.sort();
        return svSort == ProgramSVSort.EXPRESSION || svSort == ProgramSVSort.SIMPLE_EXPRESSION
                || svSort == ProgramSVSort.NON_SIMPLE_EXPRESSION || svSort == ProgramSVSort.TYPE
                || exprOrTypeSV.arity() == 0;
    }

    @Override
    @Nullable
    public MatchResultInfo check(SchemaVariable var, SyntaxElement svSubst,
            MatchResultInfo matchCond, LogicServices lServices) {
        if (var != exprOrTypeSV) {
            return matchCond;
        }

        final var inst = (SVInstantiations) matchCond.getInstantiations();
        Services services = (Services) lServices;
        Sort type;
        if (svSubst instanceof Term t) {
            type = t.sort();
        } else if (svSubst instanceof RustType rt) {
            type = services.getRustInfo().getKeYRustyType(rt.type()).getSort();
        } else {
            final var expr = (Expr) svSubst;
            type = services.getRustInfo().getKeYRustyType(expr.type(services)).getSort();
        }
        try {
            return matchCond.setInstantiations(
                inst.add(GenericSortCondition.createIdentityCondition(sort, type), lServices));
        } catch (SortException e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "\\hasSort(" + exprOrTypeSV + ", "
            + sort + ")";
    }
}
