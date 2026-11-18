/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.logic;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.expr.BinaryExpression;
import org.key_project.rusty.ast.expr.LiteralExpression;
import org.key_project.rusty.ldt.LDT;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.rusty.logic.sort.SortImpl;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

public class RustyDLTheory extends LDT {
    /// Name of the RustyDL theory
    public static final Name NAME = new Name("RustyDLTheory");
    /// Formulas are represented as "terms" of this sort.
    public static final Sort FORMULA = new SortImpl(new Name("Formula"));
    /// Updates are represented as "terms" of this sort.
    public static final Sort UPDATE = new SortImpl(new Name("Update"));
    /// Any is a supersort of all sorts.
    public static final Sort ANY = new SortImpl(new Name("any"));
    /// Name of [#getCastSymbol(Sort,Services)].
    public static final Name CAST_NAME = new Name("cast");

    public RustyDLTheory() {
        super(NAME, FORMULA);
    }

    public final ParametricFunctionDecl getCastSymbol(Services services) {
        return services.getNamespaces().parametricFunctions().lookup(CAST_NAME);
    }

    /// retrieves the cast function for the given sort
    ///
    /// @param sort the Sort for which to retrieve the cast function
    /// @param services the TermServices for lookup
    /// @return the found cast function
    public final ParametricFunctionInstance getCastSymbol(Sort sort, Services services) {
        ParametricFunctionDecl castFunction =
            services.getNamespaces().parametricFunctions().lookup(CAST_NAME);
        if (castFunction == null) {
            throw new IllegalStateException("No 'cast' function found for any type.");
        }
        return ParametricFunctionInstance.get(castFunction, ImmutableList.of(new SortArg(sort)));
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
}
