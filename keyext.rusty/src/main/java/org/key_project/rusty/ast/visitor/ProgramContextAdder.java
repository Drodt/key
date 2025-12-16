/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.visitor;

import java.rmi.UnexpectedException;
import java.util.Objects;

import org.key_project.logic.IntIterator;
import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.expr.*;
import org.key_project.rusty.ast.stmt.ExpressionStatement;
import org.key_project.rusty.ast.stmt.Statement;
import org.key_project.rusty.logic.PosInProgram;
import org.key_project.rusty.rule.inst.ContextBlockExpressionInstantiation;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.Nullable;

/// A context given as [ContextBlockExpressionInstantiation] is wrapped around a given
/// [RustyProgramElement].
public class ProgramContextAdder {
    /// singleton instance of the program context adder
    public final static ProgramContextAdder INSTANCE = new ProgramContextAdder();

    /// an empty private constructor to ensure the singleton property
    private ProgramContextAdder() {
    }

    /// wraps the context around the statements found in the putIn block
    public RustyProgramElement start(RustyProgramElement context,
            ContextBlockExpression putIn, ContextBlockExpressionInstantiation ct) {

        return wrap(context, putIn, ct.prefix().iterator(),
            ct.suffix());
    }

    protected RustyProgramElement wrap(@Nullable RustyProgramElement context,
            ContextBlockExpression putIn,
            IntIterator prefixPos, PosInProgram suffix) {
        RustyProgramElement body;

        RustyProgramElement next =
            prefixPos.hasNext()
                    ? (RustyProgramElement) Objects.requireNonNull(context)
                            .getChild(prefixPos.next())
                    : null;

        if (!prefixPos.hasNext()) {
            return createWrapperBody(context, putIn, suffix);
        } else {
            body = wrap(next, putIn, prefixPos, suffix);
            return switch (context) {
                case BlockExpression be -> createBlockExprWrapper(be, body);
                case ExpressionStatement es -> createExpressionStatementWrapper(es, body);
                case FunctionFrame ff -> createFunctionFrameWrapper(ff, (BlockExpression) body);
                case LoopScope ls -> createLoopScopeWrapper(ls, (BlockExpression) body);
                case PanicFrame pf -> createPanicFrameWrapper(pf, (BlockExpression) body);
                case null, default -> throw new RuntimeException(
                    new UnexpectedException(
                        "Unexpected block type: " + (context != null ? context.getClass() : null)));
            };
        }
    }

    /// Replaces the first part in the wrapper block. The replacement is optimized as it just
    /// returns the replacement block if it is the only child of the block to be
    /// constructed and the child is a block too.
    ///
    /// @param wrapper the StatementBlock where to replace the first statement
    /// @param replacement the StatementBlock that replaces the first statement of the block
    /// @return the resulting statement block
    private RustyProgramElement createBlockExprWrapper(BlockExpression wrapper,
            RustyProgramElement replacement) {
        int childCount = wrapper.getChildCount();
        if (childCount <= 1) {
            if (replacement instanceof BlockExpression be)
                return be;
            if (replacement instanceof Expr e)
                return new BlockExpression(ImmutableSLList.nil(), e);
        }
        var body = wrapper.getStatements().tail();
        body = body.prepend(wrapExprIfNecessary(replacement));
        return new BlockExpression(body, wrapper.getValue());
    }

    /// inserts the content of the statement block <code>putIn</code> and adds succeeding children
    /// of
    /// the innermost non-terminal element (usually statement block) in the context.
    ///
    /// @param wrapper the RustyProgramElement with the context that has to be wrapped
    /// around the content of <code>putIn</code>
    /// @param putIn the ContextBlockExpression with content that has to be wrapped by the elements
    /// hidden in
    /// the context
    /// @param suffix the PosInProgram describing the position of the first element before the
    /// suffix
    /// of the context
    /// @return the BlockExpression which encloses the content of <code>putIn</code> together with
    /// the
    /// succeeding context elements of the innermost context block (attention: in a
    /// case like <code>{{{oldStmnt; list of further stmnt;}} moreStmnts; }</code> only the
    /// underscored part is returned <code>{{ __{putIn;....}__ }moreStmnts;}</code> adding
    /// the other braces including the <code>moreStmnts;</code> part has to be done
    /// elsewhere.
    private RustyProgramElement createWrapperBody(@Nullable RustyProgramElement wrapper,
            ContextBlockExpression putIn, PosInProgram suffix) {
        if (wrapper instanceof BlockExpression be) {
            final int putInLength = putIn.getChildCount();

            // ATTENTION: may be -1
            final int lastChild = suffix.last();

            final int childLeft = wrapper.getChildCount() - lastChild;

            int childrenToAdd = putInLength + childLeft;

            if (be.getValue() != null)
                --childrenToAdd;

            if (childLeft == 0 || lastChild == -1) {
                return new BlockExpression(putIn.getStatements(), putIn.getValue());
            }

            ImmutableList<Statement> body = ImmutableSLList.nil();

            for (int i = 0; i < childrenToAdd; i++) {
                if (i < putInLength) {
                    body = body.append(wrapExprIfNecessary(putIn.getChild(i)));
                } else {
                    body = body.append((Statement) wrapper.getChild(lastChild + (i - putInLength)));
                }
            }

            Expr value = be.getValue();
            if (putIn.getValue() != null && childrenToAdd < putInLength) {
                value = putIn.getValue();
            }

            return new BlockExpression(body, value);
        } else if (wrapper instanceof ExpressionStatement es) {
            assert putIn.getStatements().isEmpty() : putIn.toString();
            return new ExpressionStatement(Objects.requireNonNull(putIn.getValue()), es.hasSemi());
        } else {
            throw new RuntimeException("Unexpected context : " + wrapper);
        }
    }

    private Statement wrapExprIfNecessary(SyntaxElement se) {
        if (se instanceof Expr e)
            return new ExpressionStatement(e, true);
        if (se instanceof Statement s)
            return s;
        throw new IllegalArgumentException("Unexpected syntax element: " + se);
    }

    private ExpressionStatement createExpressionStatementWrapper(ExpressionStatement wrapper,
            RustyProgramElement replacement) {
        return new ExpressionStatement(
            replacement instanceof BlockExpression be && be.getChildCount() == 1
                    && be.getValue() != null ? Objects.requireNonNull(be.getValue())
                            : (Expr) replacement,
            wrapper.hasSemi());
    }

    private FunctionFrame createFunctionFrameWrapper(FunctionFrame wrapper,
            BlockExpression replacement) {
        return new FunctionFrame(wrapper.getResultVar(), wrapper.getFunction(), replacement);
    }

    private LoopScope createLoopScopeWrapper(LoopScope old, BlockExpression body) {
        return new LoopScope(old.getIndex(), old.getReturnVar(), body);
    }

    private PanicFrame createPanicFrameWrapper(PanicFrame wrapper,
                                                     BlockExpression replacement) {
        return new PanicFrame(wrapper.getPanicVar(), replacement);
    }
}
