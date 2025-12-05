/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.Label;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.abstraction.Never;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.ty.NeverRustType;
import org.key_project.rusty.ast.visitor.RustyASTWalker;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class InfiniteLoopExpression implements LoopExpression, LabeledExpression {
    private final @Nullable Label label;
    private final Expr body;
    private @Nullable Type ty = null;

    public InfiniteLoopExpression(@Nullable Label label, Expr body) {
        this.label = label;
        this.body = body;
    }

    public InfiniteLoopExpression(ExtList list) {
        label = list.get(Label.class);
        body = Objects.requireNonNull(list.get(Expr.class));
    }

    public @Nullable Label label() {
        return label;
    }

    public @NonNull Expr body() {
        return body;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnInfiniteLoop(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0 && label != null)
            return label;
        if (n == 0)
            return body;
        throw new IndexOutOfBoundsException("Infinite loop expression has only 1 child");
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (label != null)
            sb.append(label).append(": ");
        sb.append("loop ").append(body);
        return sb.toString();
    }

    @Override
    public Type type(Services services) {
        if (ty == null) {
            ty = computeType(services);
        }
        return ty;
    }

    private Type computeType(Services services) {
        var bc = new BreakCollector(this, label);
        bc.start();
        var breaks = bc.getRelevantBreaks();
        if (breaks.isEmpty()) {
            return Never.INSTANCE;
        }
        var breaksIt = breaks.iterator();
        Type tyMax = breaksIt.next().type(services);
        while (breaksIt.hasNext()) {
            var b = breaksIt.next();
            var ty = b.type(services);
            if (ty == Never.INSTANCE) {
                continue;
            }
            // TODO(DD): Use max type?
            if (ty != tyMax) {
                throw new IllegalStateException("Incompatible break types: " + ty + " != " + tyMax);
            }
        }
        return tyMax;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (o == null || getClass() != o.getClass())
            return false;
        InfiniteLoopExpression that = (InfiniteLoopExpression) o;
        return Objects.equals(label, that.label) && Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(label, body);
    }

    private static class BreakCollector extends RustyASTWalker {
        private final Set<BreakExpression> relevantBreaks = new HashSet<>();
        private final @Nullable Label label;
        private int loopDepth = 0;

        BreakCollector(RustyProgramElement root, @Nullable Label label) {
            super(root);
            this.label = label;
        }

        @Override
        protected void doAction(RustyProgramElement node) {
            if (node instanceof BreakExpression be) {
                if (loopDepth == 1 && be.label() == null || be.label() != null && be.label().equals(label)) {
                    relevantBreaks.add(be);
                }
            }
        }

        @Override
        protected void enter(RustyProgramElement parent) {
            super.enter(parent);
            if (parent instanceof LoopExpression) {
                loopDepth++;
            }
        }

        @Override
        protected void leave(RustyProgramElement parent) {
            super.leave(parent);
            if (parent instanceof LoopExpression) {
                loopDepth--;
            }
        }

        public Set<BreakExpression> getRelevantBreaks() {
            return relevantBreaks;
        }
    }
}
