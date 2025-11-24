/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;

import org.key_project.logic.Name;
import org.key_project.logic.SyntaxElement;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.AbstractSort;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.rule.inst.SVInstantiations;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

public class ParametricSortInstance extends AbstractSort implements SyntaxElement {
    private static final Map<ParametricSortInstance, ParametricSortInstance> CACHE =
        new WeakHashMap<>();

    private final ImmutableList<GenericArgument> args;
    private final ParametricSortDecl base;
    private final ImmutableSet<Sort> extendsSorts;

    public static ParametricSortInstance get(ParametricSortDecl base,
            ImmutableList<GenericArgument> args) {
        assert args.size() == base.getParameters().size();
        ParametricSortInstance sort =
            new ParametricSortInstance(base, args);
        ParametricSortInstance cached = CACHE.get(sort);
        if (cached != null) {
            return cached;
        } else {
            CACHE.put(sort, sort);
            return sort;
        }
    }

    /// This must only be called in [ParametricSortInstance#get], which ensures that the cache is
    /// used.
    private ParametricSortInstance(ParametricSortDecl base, ImmutableList<GenericArgument> args) {
        super(makeName(base, args), base.isAbstract());

        this.extendsSorts = ImmutableSet.singleton(RustyDLTheory.ANY);
        this.base = base;
        this.args = args;
    }

    private static Name makeName(ParametricSortDecl base,
            ImmutableList<GenericArgument> parameters) {
        // The [ ] are produced by the list's toString method.
        return new Name(base.name() + "<" + parameters + ">");
    }

    public ParametricSortDecl getBase() {
        return base;
    }

    public ImmutableList<GenericArgument> getArgs() {
        return args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ParametricSortInstance that = (ParametricSortInstance) o;
        return Objects.equals(args, that.args) &&
                base == that.base;
    }

    @Override
    public int hashCode() {
        return Objects.hash(args, base);
    }

    @Override
    public @NonNull ImmutableSet<Sort> extendsSorts() {
        return extendsSorts;
    }

    @Override
    public boolean extendsTrans(@NonNull Sort sort) {
        return sort == this || extendsSorts()
                .exists((Sort superSort) -> superSort == sort || superSort.extendsTrans(sort));
    }

    public static Sort instantiate(GenericSort genericSort, Sort instantiation,
            Sort toInstantiate) {
        if (genericSort == toInstantiate) {
            return instantiation;
        } else if (toInstantiate instanceof ParametricSortInstance psort) {
            return psort.instantiate(genericSort, instantiation);
        } else {
            return toInstantiate;
        }
    }

    public Sort instantiate(GenericSort template, Sort instantiation) {
        ImmutableList<GenericArgument> newParameters =
            args.map(s -> s instanceof SortArg(Sort sort)
                    ? new SortArg(instantiate(template, instantiation, sort))
                    : s);
        return get(base, newParameters);
    }

    public boolean isComplete(SVInstantiations instMap) {
        for (GenericArgument arg : args) {
            if (arg instanceof SortArg sa) {
                if (sa.sort() instanceof ParametricSortInstance psi) {
                    if (!psi.isComplete(instMap))
                        return false;
                } else if (sa.sort() instanceof GenericSort gs) {
                    if (instMap.getGenericSortInstantiations().getInstantiation(gs) == null)
                        return false;
                }
            } else if (arg instanceof TermArg ta) {
                if (ta.term().op() instanceof SchemaVariable sv && !instMap.isInstantiated(sv)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Sort resolveSort(SchemaVariable sv, SyntaxElement instCandidate,
            SVInstantiations instMap) {
        ImmutableList<GenericArgument> newArgs = ImmutableSLList.nil();
        for (int i = args.size() - 1; i >= 0; i--) {
            GenericArgument arg = args.get(i);
            if (arg instanceof SortArg sa) {
                if (sa.sort() instanceof ParametricSortInstance psi) {
                    newArgs =
                        newArgs.prepend(new SortArg(psi.resolveSort(sv, instCandidate, instMap)));
                } else if (sa.sort() instanceof GenericSort gs) {
                    newArgs = newArgs.prepend(
                        new SortArg(instMap.getGenericSortInstantiations().getInstantiation(gs)));
                } else {
                    newArgs = newArgs.prepend(arg);
                }
            } else if (arg instanceof TermArg ta) {
                if (ta.term().op() instanceof SchemaVariable tsv) {
                    var inst =
                        tsv == sv ? (Term) instCandidate : (Term) instMap.getInstantiation(tsv);
                    newArgs = newArgs.prepend(new TermArg(inst));
                } else {
                    newArgs = newArgs.prepend(arg);
                }
            } else {
                throw new RuntimeException("Unrecognized argument type: " + arg.getClass());
            }
        }
        return get(base, newArgs);
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
