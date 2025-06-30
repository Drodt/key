/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.builder;

import java.util.*;
import java.util.stream.Collectors;

import org.key_project.rusty.parser.KeYRustyParserBaseVisitor;
import org.key_project.rusty.util.parsing.BuildingException;
import org.key_project.rusty.util.parsing.BuildingIssue;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.RuleContext;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/// This class brings some nice features to the visitors of key's ast.
///
///   - It makes casting implicit by using {[#accept(RuleContext)]}
///   - It allows to pass arguments by an explicit stack.
///   - It brings handling of errors and warnings.
///
///
/// @param <T> return type
/// @author Alexander Weigl
public class AbstractBuilder<T> extends KeYRustyParserBaseVisitor<T> {
    private @Nullable List<BuildingIssue> buildingIssues = null;
    private @Nullable Stack<Object> parameters = null;

    /// Helper function for avoiding cast.
    ///
    /// @param ctx
    /// @param <S>
    /// @return
    public <S> @Nullable S accept(@Nullable RuleContext ctx) {
        if (ctx == null) {
            return null;
        }
        try {
            return (S) ctx.accept(this);
        } catch (Exception e) {
            if (!(e instanceof BuildingException) && ctx instanceof ParserRuleContext) {
                throw new BuildingException((ParserRuleContext) ctx, e);
            }
            // otherwise we rethrow
            throw e;
        }
    }

    protected <T> T acceptFirst(Collection<? extends RuleContext> seq) {
        if (seq.isEmpty()) {
            return null;
        }
        return accept(seq.iterator().next());
    }

    // ask about parameterization
    protected <T> T pop() {
        if (parameters == null) {
            throw new IllegalStateException("Stack is empty");
        }
        return (T) parameters.pop();
    }

    protected void push(Object... obj) {
        if (parameters == null) {
            parameters = new Stack<>();
        }
        for (Object a : obj) {
            parameters.push(a);
        }
    }

    protected <T> @Nullable T accept(@Nullable RuleContext ctx, Object... args) {
        if (parameters == null) {
            parameters = new Stack<>();
        }
        int stackSize = parameters.size();
        push(args);
        T t = accept(ctx);
        // Stack hygiene
        while (parameters.size() > stackSize) {
            parameters.pop();
        }
        return t;
    }

    // TODO ask about generics; should this be parameterized?
    protected <S> S oneOf(ParserRuleContext... ctxs) {
        for (ParserRuleContext ctx : ctxs) {
            if (ctx != null) {
                return (S) ctx.accept(this);
            }
        }
        return null;
    }

    public @NonNull List<BuildingIssue> getBuildingIssues() {
        if (buildingIssues == null) {
            buildingIssues = new LinkedList<>();
        }
        return buildingIssues;
    }

    protected <S> List<S> mapOf(Collection<? extends ParserRuleContext> argument) {
        return argument.stream().map(it -> (S) it.accept(this)).collect(Collectors.toList());
    }

    protected void each(RuleContext... ctx) {
        for (RuleContext c : ctx) {
            accept(c);
        }
    }

    protected void each(Collection<? extends ParserRuleContext> argument) {
        for (RuleContext c : argument) {
            accept(c);
        }
    }

    protected <T2> List<T2> mapMapOf(List<? extends RuleContext>... ctxs) {
        return Arrays.stream(ctxs).flatMap(it -> it.stream().map(a -> (T2) accept(a)))
                .collect(Collectors.toList());
    }

    /// Throws a semanticError for the given ast node and message.
    ///
    /// @param ctx
    /// @param format
    /// @param args
    protected void semanticError(ParserRuleContext ctx, String format, Object... args) {
        throw new BuildingException(ctx, String.format(format, args));
    }

    /// Wraps an exception into a [BuildingException]
    ///
    /// @param e
    protected void throwEx(Throwable e) {
        throw new BuildingException(e);
    }
}
