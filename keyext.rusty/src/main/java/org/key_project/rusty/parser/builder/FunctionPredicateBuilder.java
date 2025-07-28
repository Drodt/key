/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.builder;

import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.Function;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.op.ParametricFunctionDecl;
import org.key_project.rusty.logic.op.RFunction;
import org.key_project.rusty.logic.sort.ConstParam;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.GenericSortParam;
import org.key_project.rusty.logic.sort.ParamSortParam;
import org.key_project.rusty.parser.KeYRustyParser;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.NonNull;

public class FunctionPredicateBuilder extends DefaultBuilder {
    public FunctionPredicateBuilder(Services services, NamespaceSet nss) {
        super(services, nss);
    }

    @Override
    public Object visitFile(KeYRustyParser.FileContext ctx) {
        return accept(ctx.decls());
    }

    @Override
    public Object visitDecls(KeYRustyParser.DeclsContext ctx) {
        mapMapOf(ctx.pred_decls(), ctx.func_decls(), ctx.transform_decls(), ctx.datatype_decls());
        return null;
    }

    @Override
    public Object visitDatatype_decl(KeYRustyParser.Datatype_declContext ctx) {
        // weigl: all datatypes are free ==> functions are unique!
        // boolean freeAdt = ctx.FREE() != null;
        var sort = sorts().lookup(ctx.name.getText());
        var dtNamespace = new Namespace<@NonNull Function>();
        for (KeYRustyParser.Datatype_constructorContext constructorContext : ctx
                .datatype_constructor()) {
            Name name = new Name(constructorContext.name.getText());
            Sort[] args = new Sort[constructorContext.sortId().size()];
            var argNames = constructorContext.argName;
            for (int i = 0; i < args.length; i++) {
                Sort argSort = accept(constructorContext.sortId(i));
                args[i] = argSort;
                var argName = argNames.get(i).getText();
                var alreadyDefinedFn = dtNamespace.lookup(argName);
                if (alreadyDefinedFn != null
                        && (!alreadyDefinedFn.sort().equals(argSort)
                                || !alreadyDefinedFn.argSort(0).equals(sort))) {
                    throw new RuntimeException("Name already in namespace: " + argName);
                }
                Function fn = new RFunction(new Name(argName), argSort, new Sort[] { sort }, null,
                    false, false);
                dtNamespace.add(fn);
            }
            Function function = new RFunction(name, sort, args, null, true, false);
            namespaces().functions().addSafely(function);
        }
        namespaces().functions().addSafely(dtNamespace.allElements());
        return null;
    }


    @Override
    public Object visitPred_decl(KeYRustyParser.Pred_declContext ctx) {
        String pred_name = accept(ctx.funcpred_name());
        List<Boolean> whereToBind = accept(ctx.where_to_bind());
        List<Sort> argSorts = accept(ctx.arg_sorts());
        if (whereToBind != null && whereToBind.size() != argSorts.size()) {
            semanticError(ctx, "Where-to-bind list must have same length as argument list");
        }

        Function p = null;

        assert pred_name != null;
        int separatorIndex = pred_name.indexOf("::");
        if (separatorIndex > 0) {
            String sortName = pred_name.substring(0, separatorIndex);
            String baseName = pred_name.substring(separatorIndex + 2);
            Sort genSort = lookupSort(sortName);
            // if (genSort instanceof GenericSort) {
            // assert argSorts != null;
            // p = SortDependingFunction.createFirstInstance((GenericSort) genSort,
            // new Name(baseName), RustyDLTheory.FORMULA, argSorts.toArray(new Sort[0]), false);
            // }
        }

        if (p == null) {
            assert argSorts != null;
            p = new RFunction(new Name(pred_name), RustyDLTheory.FORMULA,
                argSorts.toArray(new Sort[0]),
                whereToBind == null ? null : whereToBind.toArray(new Boolean[0]), false);
        }

        if (lookup(p.name()) == null) {
            functions().add(p);
        } else {
            // weigl: agreement on KaKeY meeting: this should be an error.
            semanticError(ctx, "Predicate '" + p.name() + "' is already defined!");
        }
        return null;
    }

    @Override
    public Object visitFunc_decl(KeYRustyParser.Func_declContext ctx) {
        boolean unique = ctx.UNIQUE() != null;
        String funcName = accept(ctx.funcpred_name());
        var sorts = new Namespace<>(nss.sorts());
        var consts = new Namespace<>(nss.functions());

        List<ParamSortParam> paramSortParams = ctx.formal_sort_param_decls() == null ? null
                : visitFormal_sort_param_decls(ctx.formal_sort_param_decls());
        if (paramSortParams != null) {
            for (ParamSortParam param : paramSortParams) {
                if (param instanceof GenericSortParam(GenericSort gs)) {
                    sorts.add(gs);
                } else if (param instanceof ConstParam(Name name, Sort sort)) {
                    consts.add(new RFunction(name, sort));
                }
            }
        }

        return withSortAndConsts(sorts, consts, () -> {
            Sort retSort = accept(ctx.sortId());
            List<Boolean[]> whereToBind = accept(ctx.where_to_bind());
            List<Sort> argSorts = accept(ctx.arg_sorts());
            assert argSorts != null;

            if (whereToBind != null && whereToBind.size() != argSorts.size()) {
                semanticError(ctx, "Where-to-bind list must have same length as argument list");
            }

            RFunction f = null;
            assert funcName != null;
            int separatorIndex = funcName.indexOf("::");
            if (separatorIndex > 0) {
                String sortName = funcName.substring(0, separatorIndex);
                String baseName = funcName.substring(separatorIndex + 2);
                Sort genSort = lookupSort(sortName);
                // if (genSort instanceof GenericSort) {
                // f = SortDependingFunction.createFirstInstance((GenericSort) genSort,
                // new Name(baseName), retSort, argSorts.toArray(new Sort[0]), unique);
                // }
            }

            // TODO debug this; why Boolean[]?

            if (f == null) {
                Name name = new Name(funcName);
                Sort[] sortsArray = argSorts.toArray(new Sort[0]);
                Boolean[] whereToBind1 =
                    whereToBind == null ? null : whereToBind.toArray(new Boolean[0]);
                if (paramSortParams == null)
                    f = new RFunction(name, retSort, sortsArray,
                        whereToBind1, unique);
                else {
                    var d = new ParametricFunctionDecl(name,
                        ImmutableList.fromList(paramSortParams), new ImmutableArray<>(sortsArray),
                        retSort, whereToBind1 == null ? null : new ImmutableArray<>(whereToBind1),
                        unique, true, false);
                    nss.parametricFunctions().add(d);
                    return null;
                }
            }
            if (lookup(f.name()) == null) {
                functions().parent().add(f);
            } else {
                // weigl: agreement on KaKeY meeting: this should be an error.
                semanticError(ctx, "Function '" + funcName + "' is already defined!");
            }
            return f;
        });
    }

    @Override
    public Object visitFunc_decls(KeYRustyParser.Func_declsContext ctx) {
        return mapOf(ctx.func_decl());
    }

    public Object visitPred_decls(KeYRustyParser.Pred_declsContext ctx) {
        return mapOf(ctx.pred_decl());
    }

}
