/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty;

import java.util.*;

import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.ast.abstraction.*;
import org.key_project.rusty.ast.abstraction.Enum;
import org.key_project.rusty.ast.abstraction.FnDefType;
import org.key_project.rusty.ast.fn.Function;
import org.key_project.rusty.logic.op.ProgramFunction;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class RustInfo {
    private final Map<Type, KeYRustyType> type2KRTCache;
    private final Set<KeYRustyType> allTypes = new LinkedHashSet<>();
    private final Set<ProgramFunction> allFunctions = new HashSet<>();
    private final Map<Function, ProgramFunction> fnToProgFn;
    private final Services services;

    /// When `true`, registering a new Java type is rejected. The parallel prover seals the
    /// type model for the duration of a multi-worker run, so a stray lazy type registration on the
    /// proving path fails fast (a loud [IllegalStateException]) instead of racing the shared
    /// model. All types are expected to be registered before proving starts -- see the
    /// `ProblemInitializer` warm-up that pre-creates the default execution context.
    /// Single-threaded proving never seals, so its behavior is unchanged.
    private volatile boolean typeRegistrationSealed = false;

    public RustInfo(Services services) {
        this.services = services;
        type2KRTCache = new HashMap<>();
        fnToProgFn = new HashMap<>();
    }

    public @Nullable KeYRustyType getKeYRustyType(String name) {
        KeYRustyType result = getPrimitiveKeYRustyType(name);
        // TODO: ADTs etc.
        return result;
    }

    public KeYRustyType getKeYRustyType(Type type) {
        if (type2KRTCache.containsKey(type)) {
            return type2KRTCache.get(type);
        } else if (type instanceof PrimitiveType pt) {
            return getPrimitiveKeYRustyType(pt);
        } else if (type instanceof TupleType tt) {
            var sort = tt.getSort(services);
            var krt = new KeYRustyType(type, sort);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof ReferenceType rt) {
            var sort = rt.getSort(services);
            var krt = new KeYRustyType(type, sort);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof FnDefType ft) {
            var krt = new KeYRustyType(ft);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof Never nt) {
            var krt = new KeYRustyType(nt);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof Closure ct) {
            var krt = new KeYRustyType(ct);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof ArrayType at) {
            var krt = new KeYRustyType(at, at.getSort(services));
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof Enum e) {
            var krt = new KeYRustyType(e, e.getSort(services));
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof GenericTyParam p) {
            var krt = new KeYRustyType(p);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof ForeignFnType f) {
            var krt = new KeYRustyType(f);
            type2KRTCache.put(type, krt);
            return krt;
        } else if (type instanceof GhostType g) {
            var krt = new KeYRustyType(g, g.getSort(services));
            type2KRTCache.put(type, krt);
            return krt;
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }

    private @Nullable KeYRustyType getPrimitiveKeYRustyType(String name) {
        PrimitiveType type = PrimitiveType.get(name);
        if (type != null) {
            return getPrimitiveKeYRustyType(type);
        } else {
            return null;
        }
    }

    private KeYRustyType getPrimitiveKeYRustyType(PrimitiveType type) {
        if (type == null) {
            throw new IllegalArgumentException("Given type is null");
        }

        if (type2KRTCache != null && type2KRTCache.containsKey(type)) {
            return type2KRTCache.get(type);
        }
        Name ldtName = type.getCorrespondingLDTName();

        Namespace<@NonNull Sort> sorts = services.getNamespaces().sorts();
        Sort sort = sorts.lookup(ldtName);

        if (sort == null) {
            throw new IllegalStateException(
                "Could not find sort " + ldtName + " for type: " + type);
        }

        var result = new KeYRustyType(type, sort);
        if (type2KRTCache != null) {
            type2KRTCache.put(type, result);
        }

        return result;
    }

    public void registerType(Type type) {
        registerType(getKeYRustyType(type));
    }

    public void registerType(KeYRustyType type) {
        allTypes.add(type);
    }

    public void registerFunction(Function function) {
        if (!fnToProgFn.containsKey(function)) {
            var pf = fnToProgFn.computeIfAbsent(function,
                (fn) -> new ProgramFunction(fn, getKeYRustyType(fn.returnType().type())));
            allFunctions.add(pf);
        }
    }

    public @Nullable ProgramFunction getFunction(Function function) {
        return fnToProgFn.get(function);
    }

    /// returns all known KeYRustyTypes of the current program type model
    ///
    /// @return all known KeYRustyTypes of the current program type model
    public Set<KeYRustyType> getAllKeYJavaTypes() {
        final Set<KeYRustyType> result = new LinkedHashSet<>();
        for (final var ty : allTypes) {
            var krt = getKeYRustyType(ty);
            if (krt != null) {
                result.add(krt);
            }
        }
        return result;
    }

    public Set<ProgramFunction> getAllFunctions() {
        return allFunctions;
    }

    /**
     * Seals the Rust type model: any subsequent attempt to register a new type throws. Used by the
     * parallel
     * prover to fail fast if a lazy type registration reaches the proving path. Idempotent.
     */
    public void sealTypeRegistration() {
        typeRegistrationSealed = true;
    }

    /** Lifts a {@link #sealTypeRegistration() seal}; the model accepts new types again. */
    public void unsealTypeRegistration() {
        typeRegistrationSealed = false;
    }

    /** @return whether the type model is currently {@link #sealTypeRegistration() sealed} */
    public boolean isTypeRegistrationSealed() {
        return typeRegistrationSealed;
    }
}
