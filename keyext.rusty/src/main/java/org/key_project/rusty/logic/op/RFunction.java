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
import org.key_project.util.collection.ImmutableList;

import org.jspecify.annotations.Nullable;

import static org.key_project.logic.op.Function.FunctionKind.ORDINARY;

public class RFunction extends Function {
    public RFunction(Name name, Sort sort, ImmutableList<Sort> argSorts,
            @Nullable ImmutableList<Boolean> whereToBind, boolean unique, boolean isRigid,
            FunctionKind kind, int introductionTime) {
        super(name, argSorts, sort, whereToBind, isRigid, unique, kind, introductionTime);

        assert sort != RustyDLTheory.UPDATE;
        assert !(unique && sort == RustyDLTheory.FORMULA);
    }

    public RFunction(Name name, Sort sort, ImmutableList<Sort> argSorts,
            @Nullable ImmutableList<Boolean> whereToBind, boolean unique) {
        this(name, sort, argSorts, whereToBind, unique, true, ORDINARY, UNRECORDED);
    }

    public RFunction(Name name, Sort sort, ImmutableList<Sort> argSorts,
            @Nullable ImmutableList<Boolean> whereToBind, boolean unique,
            FunctionKind kind,
            int introductionTime) {
        this(name, sort, argSorts, whereToBind, unique, true, kind, introductionTime);
    }

    public RFunction(Name name, Sort sort, Sort[] argSorts, Boolean @Nullable [] whereToBind,
            boolean unique) {
        this(name, sort, ImmutableList.fromArray(argSorts),
            whereToBind == null ? null : ImmutableList.fromArray(whereToBind), unique);
    }

    public RFunction(Name name, Sort sort, Sort[] argSorts, Boolean @Nullable [] whereToBind,
            boolean unique,
            FunctionKind kind,
            int introductionTime) {
        this(name, sort, ImmutableList.fromArray(argSorts),
            whereToBind == null ? null : ImmutableList.fromArray(whereToBind), unique,
            kind, introductionTime);
    }

    RFunction(Name name, Sort sort, ImmutableList<Sort> argSorts, boolean isRigid) {
        this(name, sort, argSorts, null, false, isRigid, ORDINARY, UNRECORDED);
    }

    public RFunction(Name name, Sort sort, ImmutableList<Sort> argSorts) {
        this(name, sort, argSorts, null, false);
    }

    public RFunction(Name name, Sort sort, Sort... argSorts) {
        this(name, sort, argSorts, null, false);
    }

    public RFunction(Name name, Sort sort, FunctionKind kind,
            int introductionTime, Sort... argSorts) {
        this(name, sort, argSorts, null, false, kind, introductionTime);
    }

    public RFunction(Name name, Sort sort) {
        this(name, sort, ImmutableList.nil(), null, false);
    }

    public RFunction(Name name, Sort sort, FunctionKind kind,
            int introductionTime) {
        this(name, sort, ImmutableList.nil(), null, false, true, kind, introductionTime);
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
