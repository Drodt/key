/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.ty;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.ArrayType;
import org.key_project.rusty.ast.abstraction.IntArrayLen;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.expr.IntegerLiteralExpression;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;

public class ArrayRustType implements RustType {
    private final RustType elemTy;
    private final Expr len;
    private final Type ty;

    public ArrayRustType(RustType elemTy, Expr len, Services services) {
        this.elemTy = elemTy;
        this.len = len;
        int length = switch (len) {
            case IntegerLiteralExpression le -> le.getValue().intValue();
            default -> throw new IllegalStateException("Unexpected value: " + len);
        };
        ty = ArrayType.getInstance(elemTy.type(), new IntArrayLen(length), services);
    }

    public RustType getElemTy() {
        return elemTy;
    }

    public Expr getLen() {
        return len;
    }

    @Override
    public Type type() {
        return ty;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnArrayRustType(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
            case 0 -> elemTy;
            case 1 -> len;
            default -> throw new IndexOutOfBoundsException();
        };
    }

    @Override
    public int getChildCount() {
        return 2;
    }
}
