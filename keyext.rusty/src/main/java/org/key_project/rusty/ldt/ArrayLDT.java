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

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ArrayLDT extends LDT {
    public static final Name NAME = new Name("Array");

    private final ParametricFunctionDecl len;
    private final ParametricFunctionDecl repeat;
    private final ParametricFunctionDecl outside;
    private final ParametricFunctionDecl get;
    private final ParametricFunctionDecl set;

    public ArrayLDT(Services services) {
        super(NAME, services);

        len = addParametricFunction(services, "len");
        repeat = addParametricFunction(services, "arr_repeat");
        outside = addParametricFunction(services, "arr_outside");
        get = addParametricFunction(services, "arr_get");
        set = addParametricFunction(services, "arr_set");
    }

    public ParametricFunctionDecl getLen() {
        return len;
    }

    public ParametricFunctionDecl getRepeat() {
        return repeat;
    }

    public ParametricFunctionDecl getOutside() {
        return outside;
    }

    public ParametricFunctionDecl getGet() {
        return get;
    }

    public ParametricFunctionDecl getSet() {
        return set;
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
