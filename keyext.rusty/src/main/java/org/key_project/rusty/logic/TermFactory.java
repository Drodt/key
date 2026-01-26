/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic;

import java.util.Map;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class TermFactory {
    private static final ImmutableArray<Term> NO_SUBTERMS = new ImmutableArray<>();
    private final @Nullable Map<Term, Term> cache;


    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------


    public TermFactory() {
        this.cache = null;
    }

    public TermFactory(Map<Term, Term> cache) {
        this.cache = cache;
    }

    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    /// Master method for term creation. Should be the only place where terms are created in the
    /// entire system.
    public Term createTerm(Operator op, @Nullable ImmutableArray<Term> subs,
            @Nullable ImmutableArray<QuantifiableVariable> boundVars) {
        if (subs == null || subs.isEmpty()) {
            subs = NO_SUBTERMS;
        }

        return doCreateTerm(op, subs, boundVars);
    }

    public Term createTerm(@NonNull Operator op, Term... subs) {
        return createTerm(op, createSubtermArray(subs), null);
    }

    public Term createTerm(Operator op, @Nullable Term[] subs,
            @Nullable ImmutableArray<QuantifiableVariable> boundVars) {
        return createTerm(op, createSubtermArray(subs), boundVars);
    }

    public Term createTerm(Operator op, Term sub) {
        return createTerm(op, new ImmutableArray<>(sub), null);
    }

    public Term createTerm(Operator op) {
        return createTerm(op, NO_SUBTERMS, null);
    }

    // -------------------------------------------------------------------------
    // private interface
    // -------------------------------------------------------------------------

    private ImmutableArray<Term> createSubtermArray(@Nullable Term[] subs) {
        if (subs == null || subs.length == 0)
            return NO_SUBTERMS;
        // Checker framework is imprecise here
        @SuppressWarnings("type.arguments.not.inferred")
        ImmutableArray<Term> terms = new ImmutableArray<>(subs);
        return terms;
    }

    private Term doCreateTerm(Operator op, ImmutableArray<Term> subs,
            @Nullable ImmutableArray<QuantifiableVariable> boundVars) {

        final TermImpl newTerm =
            new TermImpl(op, subs, boundVars);

        // Check if caching is possible. It is not possible if a non-empty RustyBlock is available
        // in the term or in one of its children because the meta information like PositionInfos
        // may be different.
        if (cache != null && !newTerm.containsCodeBlockRecursive()) {
            Term term;
            synchronized (cache) {
                term = cache.get(newTerm);
            }
            if (term == null) {
                term = newTerm.checked();
                synchronized (cache) {
                    cache.put(term, term);
                }
            }
            return term;
        } else {
            return newTerm.checked();
        }
    }
}
