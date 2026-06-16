/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.TermCreationException;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.RustyDLTheory;
import org.key_project.rusty.logic.sort.GenericSort;
import org.key_project.rusty.logic.sort.ProgramSVSort;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public class RFunction extends Function {
    public RFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts,
            @Nullable ImmutableArray<Boolean> whereToBind, boolean unique, boolean isRigid,
            boolean isSkolemConstant) {
        super(name, argSorts, sort, whereToBind, isRigid, unique, isSkolemConstant);

        assert sort != RustyDLTheory.UPDATE;
        assert !(unique && sort == RustyDLTheory.FORMULA);
    }

    public RFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts,
            @Nullable ImmutableArray<Boolean> whereToBind, boolean unique) {
        this(name, sort, argSorts, whereToBind, unique, true, false);
    }

    public RFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts,
            @Nullable ImmutableArray<Boolean> whereToBind, boolean unique,
            boolean isSkolemConstant) {
        this(name, sort, argSorts, whereToBind, unique, true, isSkolemConstant);
    }

    public RFunction(Name name, Sort sort, Sort[] argSorts, Boolean @Nullable [] whereToBind,
            boolean unique) {
        this(name, sort, new ImmutableArray<>(argSorts),
            whereToBind == null ? null : new ImmutableArray<>(whereToBind), unique);
    }

    public RFunction(Name name, Sort sort, Sort[] argSorts, Boolean @Nullable [] whereToBind,
            boolean unique,
            boolean isSkolemConstant) {
        this(name, sort, new ImmutableArray<>(argSorts),
            whereToBind == null ? null : new ImmutableArray<>(whereToBind), unique,
            isSkolemConstant);
    }

    RFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts, boolean isRigid) {
        this(name, sort, argSorts, null, false, isRigid, false);
    }

    public RFunction(Name name, Sort sort, ImmutableArray<Sort> argSorts) {
        this(name, sort, argSorts, null, false);
    }

    public RFunction(Name name, Sort sort, Sort... argSorts) {
        this(name, sort, argSorts, null, false);
    }

    public RFunction(Name name, Sort sort, boolean isSkolemConstant, Sort... argSorts) {
        this(name, sort, argSorts, null, false, isSkolemConstant);
    }

    public RFunction(Name name, Sort sort) {
        this(name, sort, new ImmutableArray<>(), null, false);
    }

    public RFunction(Name name, Sort sort, boolean isSkolemConstant) {
        this(name, sort, new ImmutableArray<>(), null, false, true, isSkolemConstant);
    }

    /// In addition to the arity checks of the base operator, validates that every argument's sort
    /// conforms to the declared argument sort (so ill-typed terms such as `add(int, bool)` are
    /// rejected at construction). Mirrors legacy KeY's sorted-operator check.
    @Override
    public <T extends Term> void validTopLevelException(T term) throws TermCreationException {
        super.validTopLevelException(term);
        for (int i = 0, n = arity(); i < n; i++) {
            if (!possibleSub(i, term.sub(i))) {
                throw new TermCreationException(this, term);
            }
        }
    }

    /// Whether `sub` may legally occur as the `at`-th argument: its sort must be a (transitive)
    /// subsort of the declared argument sort, with the usual escapes for schema variables (matched
    /// loosely), the top sort `any`, term transformers (the meta sort) and program-SV sorts.
    private boolean possibleSub(int at, Term sub) {
        if (sub.op() instanceof SchemaVariable) {
            return true;
        }
        final Sort s = sub.sort();
        final Sort argSort = argSort(at);
        // A generic argument sort (e.g. the `E` of a parametric `ghost<[E]>`) accepts any term: the
        // generic is bound to a concrete sort at application time, where matching does the check.
        return argSort instanceof GenericSort
                || s == RustyDLTheory.ANY
                || s == AbstractTermTransformer.METASORT
                || s instanceof ProgramSVSort
                || argSort == AbstractTermTransformer.METASORT
                || argSort instanceof ProgramSVSort
                || s.extendsTrans(argSort);
    }
}
