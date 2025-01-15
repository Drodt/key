/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.stmt;

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.VariableDeclaration;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.pat.Pattern;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.ast.visitor.Visitor;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Represents a `let` statement in a Rust abstract syntax tree (AST).
 * A `let` statement declares a variable by binding it to a pattern, optionally specifying its type,
 * and optionally providing an initializer expression.
 *
 * <p>
 * This class is immutable and provides methods to access its components,
 * including the pattern, type, and initializer expression.
 * </p>
 *
 *
 * <h3>Thread Safety</h3>
 * <p>
 * This class is immutable and therefore thread-safe.
 * </p>
 */
public class LetStatement implements Statement, VariableDeclaration {
    /**
     * The pattern to which the value is bound.
     */
    private final Pattern pat;

    /**
     * The type of the variable being declared (nullable).
     */
    private final RustType type;

    /**
     * The initializer expression (nullable).
     */
    private final Expr init;

    private int hashCode = -1;

    /**
     * Constructs a new `LetStatement`.
     *
     * @param pat The pattern to which the value is bound. Must not be {@code null}.
     * @param type The type of the variable being declared. May be {@code null}.
     * @param init The initializer expression for the variable. May be {@code null}.
     */
    public LetStatement(Pattern pat, @Nullable RustType type, @Nullable Expr init) {
        this.pat = pat;
        this.type = type;
        this.init = init;
    }


    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return switch (n) {
        case 0 -> pat;
        case 1 -> type;
        case 2 -> Objects.requireNonNull(init);
        default -> throw new IndexOutOfBoundsException("LetStatement has three children");
        };
    }

    @Override
    public int getChildCount() {
        return 3;
    }

    public RustType type() {
        return type;
    }

    public Pattern getPattern() {
        return pat;
    }

    @Override
    public String toString() {
        return "let " + pat + ": " + type + " = " + init;
    }

    @Override
    public void visit(Visitor v) {
        throw new RuntimeException("TODO @ DD");
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            return hashCode;
        }
        final int hash = computeHashCode();
        this.hashCode = hash;
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        final LetStatement that = (LetStatement) obj;
        return pat.equals(that.pat) && Objects.equals(type, that.type)
                && Objects.equals(init, that.init);
    }
}
