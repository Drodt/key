/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ldt;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.expr.BinaryExpression;
import org.key_project.rusty.ast.expr.LiteralExpression;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.op.RFunction;
import org.key_project.rusty.logic.sort.GenericParameter;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class FieldLDT extends LDT {
    public static final Name NAME = new Name("Field");
    private final Services services;
    private final ParametricSortDecl fieldSort;

    private final ParametricFunctionDecl get;
    private final ParametricFunctionDecl set;

    public FieldLDT(Services services) {
        super(NAME, services);
        this.services = services;
        fieldSort = parametricSort();
        get = addParametricFunction(services, "get");
        set = addParametricFunction(services, "set");
    }

    public RFunction createField(String prefix, Name fieldName, Type type) {
        var argSort = type.getSort(services);
        ParametricSortInstance sort =
            ParametricSortInstance.get(fieldSort, ImmutableList.of(new SortArg(argSort)));
        Name name = new Name(prefix + fieldName.toString());
        var fn = new RFunction(name, sort, new ImmutableArray<>(), null, true);
        services.getNamespaces().functions().addSafely(fn);
        return fn;
    }

    public ParametricFunctionDecl getGet() {
        return get;
    }

    public ParametricFunctionDecl getSet() {
        return set;
    }

    public ParametricFunctionDecl createGenericField(String prefix, Name fieldName, Type type,
            ImmutableList<GenericParameter> generics) {
        var argSort = type.getSort(services);
        ParametricSortInstance sort =
            ParametricSortInstance.get(fieldSort, ImmutableList.of(new SortArg(argSort)));
        Name name = new Name(prefix + fieldName.toString());
        var fn = new ParametricFunctionDecl(name, generics, new ImmutableArray<>(), sort, null,
            true, true, false);
        services.getNamespaces().parametricFunctions().addSafely(fn);
        return fn;
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
