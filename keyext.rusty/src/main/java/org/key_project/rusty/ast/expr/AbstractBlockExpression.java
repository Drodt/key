/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import java.util.ArrayList;
import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ElseBranch;
import org.key_project.rusty.ast.ProgramPrefixUtil;
import org.key_project.rusty.ast.abstraction.TupleType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.stmt.Statement;
import org.key_project.rusty.logic.PosInProgram;
import org.key_project.rusty.logic.PossibleProgramPrefix;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.checkerframework.checker.initialization.qual.UnknownInitialization;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public abstract class AbstractBlockExpression
        implements Expr, PossibleProgramPrefix, ThenBranch, ElseBranch {

    protected final ImmutableList<Statement> statements;
    protected final @Nullable Expr value;
    private final int prefixLength;

    private int hashCode = -1;

    protected AbstractBlockExpression(ImmutableList<Statement> statements, @Nullable Expr value) {
        this.statements = statements;
        this.value = value;
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        this.prefixLength = info.length();
    }

    protected AbstractBlockExpression(ExtList children) {
        this.statements = ImmutableList.of(children.collect(Statement.class));
        this.value = children.get(Expr.class);
        ProgramPrefixUtil.ProgramPrefixInfo info = ProgramPrefixUtil.computeEssentials(this);
        this.prefixLength = info.length();
    }

    @Override
    public @NonNull SyntaxElement getChild(@UnknownInitialization AbstractBlockExpression this,
            int n) {
        assert statements != null;
        if (0 <= n && n < statements.size())
            return Objects.requireNonNull(statements.get(n));
        if (n == statements.size() && value != null)
            return value;
        throw new IndexOutOfBoundsException(
            getClass().getSimpleName() + " has less than " + n + " children");
    }

    @Override
    public int getChildCount(@UnknownInitialization AbstractBlockExpression this) {
        assert statements != null;
        return statements.size() + (value == null ? 0 : 1);
    }

    public ImmutableList<Statement> getStatements() {
        return statements;
    }

    public @Nullable Expr getValue() {
        return value;
    }

    @Override
    public abstract void visit(org.key_project.rusty.ast.visitor.Visitor v);

    @Override
    public boolean isPrefix(@UnknownInitialization AbstractBlockExpression this) {
        return getChildCount() != 0;
    }

    @Override
    public boolean hasNextPrefixElement(@UnknownInitialization AbstractBlockExpression this) {
        return getChildCount() != 0 && getChild(0) instanceof PossibleProgramPrefix;
    }

    @Override
    public PossibleProgramPrefix getNextPrefixElement(
            @UnknownInitialization AbstractBlockExpression this) {
        if (hasNextPrefixElement()) {
            return (PossibleProgramPrefix) getChild(0);
        }
        throw new IndexOutOfBoundsException("No next prefix element " + this);
    }

    @Override
    public PossibleProgramPrefix getLastPrefixElement() {
        return hasNextPrefixElement() ? getNextPrefixElement().getLastPrefixElement() : this;
    }

    @Override
    public ImmutableArray<PossibleProgramPrefix> getPrefixElements() {
        return computePrefixElements(this);
    }

    @Override
    public PosInProgram getFirstActiveChildPos() {
        return PosInProgram.ZERO;
    }

    @Override
    public int getPrefixLength(@UnknownInitialization AbstractBlockExpression this) {
        return prefixLength;
    }

    public static ImmutableArray<PossibleProgramPrefix> computePrefixElements(
            PossibleProgramPrefix current) {
        final ArrayList<PossibleProgramPrefix> prefix = new ArrayList<>();
        prefix.add(current);

        while (current.hasNextPrefixElement()) {
            current = current.getNextPrefixElement();
            prefix.add(current);
        }

        return new ImmutableArray<>(prefix);
    }

    @Override
    public Type type(Services services) {
        return value == null ? TupleType.UNIT : value.type(services);
    }

    @Override
    public int hashCode() {
        if (hashCode != -1) {
            return hashCode;
        }
        final int h = computeHashCode();
        this.hashCode = h;
        return h;
    }

    public int computeHashCode() {
        return Objects.hash(getClass(), statements, value, prefixLength);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AbstractBlockExpression that = (AbstractBlockExpression) o;
        return this.prefixLength == that.prefixLength
                && Objects.equals(this.statements, that.statements)
                && Objects.equals(this.value, that.value);
    }
}
