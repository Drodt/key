/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ldt;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.BinaryExpression;
import org.key_project.rusty.ast.expr.LiteralExpression;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.sort.*;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public class TupleLDT extends LDT {
    private static final Name NAME = new Name("Tuple");

    private final ParametricSortDecl[] tupleSorts = new ParametricSortDecl[12];
    private final ParametricFunctionDecl[] tupleFunctions = new ParametricFunctionDecl[12];
    private final ParametricFunctionDecl[] fieldFunctions = new ParametricFunctionDecl[12];

    public TupleLDT(Services services) {
        super(NAME);

        var paraSorts = services.getNamespaces().parametricSorts();
        var paraFns = services.getNamespaces().parametricFunctions();

        for (int i = 0; i < 12; i++) {
            tupleSorts[i] = paraSorts.lookup("Tuple" + (i + 1));

            tupleFunctions[i] = paraFns.lookup("tpl" + (i + 1));

            fieldFunctions[i] = paraFns.lookup("Tuple$" + i);
        }
    }

    public @NonNull ParametricSortDecl getTupleSort(int i) {
        return tupleSorts[i];
    }

    public @NonNull ParametricFunctionDecl getTupleFunction(int i) {
        return tupleFunctions[i];
    }

    public @NonNull ParametricFunctionDecl getFieldFunctions(int i) {
        return fieldFunctions[i];
    }

    @Override
    public @Nullable Term translateLiteral(LiteralExpression lit, Services services) {
        return null;
    }

    @Override
    public @Nullable Function getFunctionFor(BinaryExpression.Operator op, Services services) {
        return null;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term[] subs, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term sub, Services services) {
        return false;
    }

    @Override
    public boolean isResponsible(BinaryExpression.Operator op, Term left, Term right,
            Services services) {
        return false;
    }

    @Override
    public @NonNull Name name() {
        return NAME;
    }
}
