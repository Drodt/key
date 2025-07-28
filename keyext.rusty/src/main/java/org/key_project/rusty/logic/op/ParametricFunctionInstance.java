/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.sort.*;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;

public class ParametricFunctionInstance extends RFunction {
    private static final Map<ParametricFunctionInstance, ParametricFunctionInstance> CACHE =
        new WeakHashMap<>();

    private final ImmutableList<ParamSortArg> args;
    private final ParametricFunctionDecl base;

    public static ParametricFunctionInstance get(ParametricFunctionDecl decl,
            ImmutableList<ParamSortArg> args) {
        assert args.size() == decl.getParameters().size();
        var instMap = getInstMap(decl, args);
        var argSorts = instantiate(decl, instMap);
        var sort = instantiate(decl.sort(), instMap);
        var fn = new ParametricFunctionInstance(decl, args, argSorts, sort);
        var cached = CACHE.get(fn);
        if (cached != null) {
            return cached;
        }
        CACHE.put(fn, fn);
        return fn;
    }

    private ParametricFunctionInstance(ParametricFunctionDecl base,
            ImmutableList<ParamSortArg> args, ImmutableArray<Sort> argSorts, Sort sort) {
        super(makeName(base, args), sort, argSorts, base.getWhereToBind(), base.isUnique(),
            base.isRigid(),
            base.isSkolemConstant());
        this.base = base;
        this.args = args;
    }

    public ParametricFunctionDecl getBase() {
        return base;
    }

    public ImmutableList<ParamSortArg> getArgs() {
        return args;
    }

    private static Name makeName(ParametricFunctionDecl base,
            ImmutableList<ParamSortArg> parameters) {
        // The [ ] are produced by the list's toString method.
        return new Name(base.name() + "<" + parameters + ">");
    }

    private static ImmutableArray<Sort> instantiate(ParametricFunctionDecl base,
            HashMap<ParamSortParam, ParamSortArg> instMap) {
        var baseArgSorts = base.argSorts();
        var argSorts = new Sort[baseArgSorts.size()];

        for (int i = 0; i < baseArgSorts.size(); i++) {
            var sort = baseArgSorts.get(i);
            argSorts[i] = instantiate(sort, instMap);
        }

        return new ImmutableArray<>(argSorts);
    }

    private static HashMap<ParamSortParam, ParamSortArg> getInstMap(ParametricFunctionDecl base,
            ImmutableList<ParamSortArg> args) {
        var map = new HashMap<ParamSortParam, ParamSortArg>();

        for (int i = 0; i < base.getParameters().size(); i++) {
            var param = base.getParameters().get(i);
            var arg = args.get(i);
            map.put(param, arg);
        }
        return map;
    }

    private static Sort instantiate(Sort sort, Map<ParamSortParam, ParamSortArg> map) {
        if (sort instanceof GenericSort gs) {
            var param = new GenericSortParam(gs);
            var arg = map.get(param);
            return arg == null ? gs : ((SortArg) arg).sort();
        } else if (sort instanceof ParametricSortInstance psi) {
            var base = psi.getBase();
            ImmutableList<ParamSortArg> args = ImmutableSLList.nil();
            for (int i = psi.getArgs().size() - 1; i >= 0; i--) {
                var psiArg = psi.getArgs().get(i);
                if (psiArg instanceof SortArg(Sort s)) {
                    args = args.prepend(new SortArg(instantiate(s, map)));
                } else if (psiArg instanceof TermArg ta) {
                    if (ta.term().op() instanceof RFunction rf) {
                        var t = map.get(new ConstParam(rf.name(), rf.sort()));
                        var arg = t == null ? ta : t;
                        args = args.prepend(arg);
                    } else {
                        var m = 1;
                    }
                }
            }
            return ParametricSortInstance.get(base, args);
        } else {
            return sort;
        }
    }

    @Override
    public int getChildCount() {
        return args.size();
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        return Objects.requireNonNull(args.get(n));
    }
}
