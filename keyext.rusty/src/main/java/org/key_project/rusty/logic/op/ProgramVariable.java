/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import java.util.Objects;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.op.AbstractSortedOperator;
import org.key_project.logic.op.Modifier;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.Res;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.abstraction.KeYRustyType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.rule.MatchConditions;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ProgramVariable extends AbstractSortedOperator
        implements Expr, UpdateableOperator, Res, IProgramVariable {
    private final KeYRustyType type;

    public ProgramVariable(Name name, Sort s, KeYRustyType type) {
        super(name, s, Modifier.NONE);
        this.type = type;
    }

    public ProgramVariable(Name name, KeYRustyType type) {
        this(name, Objects.requireNonNull(type.getSort()), type);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        throw new IndexOutOfBoundsException("Program variable does not have a child");
    }

    @Override
    public int getChildCount() {
        return 0;
    }

    public KeYRustyType getKeYRustyType() {
        return type;
    }


    @Override
    public @Nullable MatchConditions match(SourceData source, @Nullable MatchConditions mc) {
        final var src = source.getSource();
        source.next();
        if (src == this) {
            return mc;
        } else {
            return null;
        }
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnProgramVariable(this);
    }

    @Override
    public Type type(Services services) {
        return Objects.requireNonNull(type.getRustyType());
    }
}
