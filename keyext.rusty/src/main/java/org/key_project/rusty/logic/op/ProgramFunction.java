/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import java.util.Objects;

import org.key_project.rusty.ast.Def;
import org.key_project.rusty.ast.RustyProgramElement;
import org.key_project.rusty.ast.SourceData;
import org.key_project.rusty.ast.abstraction.KeYRustyType;
import org.key_project.rusty.ast.expr.BlockExpression;
import org.key_project.rusty.ast.fn.Function;
import org.key_project.rusty.ast.fn.FunctionParamPattern;
import org.key_project.rusty.ast.pat.BindingPattern;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.rule.MatchConditions;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class ProgramFunction extends ObserverFunction implements RustyProgramElement, Def {
    /// The referenced function.
    private final @NonNull Function function;

    private final @NonNull KeYRustyType returnType;

    public ProgramFunction(Function function, KeYRustyType returnType) {
        super(function.name().toString(), Objects.requireNonNull(returnType.getSort()), returnType,
            getParamTypes(function));
        this.function = function;
        this.returnType = returnType;
    }

    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------

    /// Get the rusty types of the parameters required by the function fn.
    ///
    /// @param fn some function declaration
    /// @return java types of the parameters required by fn
    private static ImmutableArray<KeYRustyType> getParamTypes(Function fn) {
        KeYRustyType[] result = new KeYRustyType[fn.params().size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = fn.params().get(i).getKeYRustyType();
        }
        return new ImmutableArray<>(result);
    }

    public @NonNull Function getFunction() {
        return function;
    }

    public BlockExpression getBody() {
        return function.body();
    }


    @Override
    public void visit(Visitor v) {
        v.performActionOnProgramFunction(this);
    }

    @Override
    public @Nullable MatchConditions match(SourceData source, @Nullable MatchConditions matchCond) {
        final RustyProgramElement src = source.getSource();
        if (src == this) {
            source.next();
            return matchCond;
        } else {
            return null;
        }
    }

    public ImmutableList<ProgramVariable> collectParameters() {
        return collectParameters(function);
    }

    public static ImmutableList<ProgramVariable> collectParameters(Function function) {
        ImmutableList<ProgramVariable> params = ImmutableSLList.nil();
        for (int i = function.params().size() - 1; i >= 0; --i) {
            var param = function.params().get(i);
            if (param instanceof FunctionParamPattern fp
                    && fp.pattern() instanceof BindingPattern bp) {
                params = params.prepend(bp.pv());
            } else {
                throw new RuntimeException("Expected PV param");
            }
        }
        return params;
    }
}
