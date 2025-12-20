package org.key_project.rusty.ast.abstraction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.ast.ty.GhostRustType;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;

import java.util.HashMap;
import java.util.Map;

public class GhostType implements Type{
    private final Type inner;
    private final Name name;

    static final Map<Type, GhostType> cache = new HashMap<>();

    public static GhostType get(Type inner) {
        return cache.computeIfAbsent(inner, i -> new GhostType(inner));
    }

    private GhostType(Type inner) {
        this.inner = inner;
        name = new Name("Ghost<" + inner.toString() + ">");
    }

    @Override
    public @Nullable Sort getSort(Services services) {
        Sort inner = this.inner.getSort(services);
        ParametricSortDecl pSort = services.getLDTs().getGhostLDT().parametricSort();
        assert pSort != null;
        return ParametricSortInstance.get(pSort, ImmutableList.of(new SortArg(inner)));
    }


    @Override
    public RustType toRustType(Services services) {
        return new GhostRustType(inner.toRustType(services));
    }

    @Override
    public Type instantiate(Map<GenericParam, GenericTyArg> instMap, Services services) {
        var it = inner.instantiate(instMap, services);
        if (it == inner)
            return this;
        return get(it);

    }

    @Override
    public @NonNull Name name() {
        return name;
    }

    private Type inner(){
        return inner;
    }
}
