/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import java.util.Map;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.function.Function;

import org.key_project.logic.Name;
import org.key_project.logic.sort.AbstractSort;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.NonNull;

public class ParametricSortInstance extends AbstractSort {
    private static final Map<ParametricSortInstance, ParametricSortInstance> CACHE =
        new WeakHashMap<>();

    private final ImmutableList<Sort> parameters;
    private final ParametricSortDecl base;
    private final ImmutableSet<Sort> extendsSorts;

    public static ParametricSortInstance get(ParametricSortDecl base,
            ImmutableList<Sort> parameters) {
        ParametricSortInstance sort =
            new ParametricSortInstance(base, parameters);
        ParametricSortInstance cached = CACHE.get(sort);
        if (cached != null) {
            return cached;
        } else {
            CACHE.put(sort, sort);
            return sort;
        }
    }

    // This must only be called in #get, which ensures that the cache is used.
    private ParametricSortInstance(ParametricSortDecl base, ImmutableList<Sort> parameters) {
        super(makeName(base, parameters), base.isAbstract());

        this.extendsSorts = ImmutableSet.singleton(RustyDLTheory.ANY);
        this.base = base;
        this.parameters = parameters;
    }

    private static Name makeName(ParametricSortDecl base, ImmutableList<Sort> parameters) {
        // The [ ] are produced by the list's toString method.
        return new Name(base.name() + "<" + parameters + ">");
    }

    public ParametricSortDecl getBase() {
        return base;
    }

    public ImmutableList<Sort> getParameters() {
        return parameters;
    }

    public ParametricSortInstance map(Function<Sort, Sort> f) {
        ImmutableList<Sort> newParameters = parameters.map(f);
        // The cache ensures that no unnecessary duplicates are kept.
        return get(base, newParameters);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        ParametricSortInstance that = (ParametricSortInstance) o;
        return Objects.equals(parameters, that.parameters) &&
                base == that.base;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parameters, base);
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
        ImmutableList<Sort> newParameters =
            parameters.map(s -> instantiate(template, instantiation, s));
        return get(base, newParameters);
    }
}
