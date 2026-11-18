/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics;

import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.TermFactory;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.strategy.quantifierHeuristics.theory.TheoryReasoning;
import org.key_project.util.collection.ImmutableSet;

/// Equality reasoning over the ground equalities assumed on a sequent, used by the quantifier
/// instantiation cost prediction. The two sides of every assumed equality are merged into a class,
/// and a term is rewritten to the representatives of its subterms, so that a clause literal already
/// satisfied by those equalities becomes syntactically equal to an assumed literal. The instance
/// then predicts as non-contributing and is not proposed as a useful step.
/// Each merge is oriented the way the equality is written on the sequent: occurrences of the left
/// side rewrite to the right side. The proof search normalizes sequent equalities by the rules of
/// the owning theory, so this direction is the theory-established one. A
/// [TheoryReasoning] can veto a rewrite that would disturb the normal forms its
/// decisions depend on ([TheoryReasoning#allowsEqualityRewrite]); then the opposite
/// direction is tried, and a doubly vetoed equality is left out of the congruence.
/// One congruence is built per sequent in [Instantiation] and shared across the cost
/// predictions of all candidate instantiations of the quantified formula, so the union-find is
/// built once rather than once per candidate.
final class Congruence {
    /// Union-find parent map over the terms merged by the assumed equalities.
    private final Map<Term, Term> parent = new HashMap<>();

    /// @param assumedLiterals the literals assumed true on the sequent
    /// @param services the services providing access to theories, termfactory etc.
    Congruence(ImmutableSet<Term> assumedLiterals, Services services) {
        for (final Term lit : assumedLiterals) {
            if (lit.op() == Equality.EQUALS) {
                union(lit.sub(0), lit.sub(1), services);
            }
        }
    }

    /// Results of [#normalize]; this object is per sequent and thread-confined.
    private final Map<Term, Term> normalized = new HashMap<>();

    /// Whether the sequent asserts no equality; then [#normalize] is the identity.
    boolean isTrivial() {
        return parent.isEmpty();
    }

    /// The representative of `t`'s class, or `t` itself if it is in no class.
    private Term find(Term t) {
        // Iterative with a second compression pass: the merge direction is dictated by the
        // sequent, so ranks cannot bound the chains, and a recursive walk would grow the stack
        // with the chain length.
        Term root = t;
        Term p;
        while ((p = parent.get(root)) != null) {
            root = p;
        }
        Term walk = t;
        while ((p = parent.get(walk)) != null && p != root) {
            parent.put(walk, root);
            walk = p;
        }
        return root;
    }

    /// Merges the classes of the two sides of an assumed equality `a = b`, preferring the
    /// sequent's own orientation: the representative of `a`'s class points to the
    /// representative of `b`'s class, so occurrences of the left side rewrite to the right
    /// side. A direction that is ill sorted or vetoed by a theory support is not installed; then
    /// the flipped direction is tried, and if that is rejected too the equality does not take part
    /// in the congruence.
    private void union(Term a, Term b, Services services) {
        final Term ra = find(a);
        final Term rb = find(b);
        if (ra.equals(rb)) {
            return;
        }
        if (allowed(ra, rb, services)) {
            parent.put(ra, rb);
        } else if (allowed(rb, ra, services)) {
            parent.put(rb, ra);
        }
    }

    /// Whether occurrences of `from` may be rewritten to `to`.
    ///
    /// The representative's sort must equal or specialize the rewritten side's sort: every operator
    /// position accepting a term of `from`'s sort then accepts the representative, so the
    /// rebuild in [#normalize] stays well sorted. A widening direction is ill sorted: a
    /// contract equality `cast<int[]>(dest) = dest` read left to right would put the
    /// `Object`-sorted variable into positions requiring `int[]`. The sort condition
    /// holds transitively along the parent links, since every link narrows the sort. On top of the
    /// sort condition every theory support must permit the rewrite.
    ///
    /// @param from the class representative that would point to `to`
    /// @param to the term that would become the representative of the merged class
    /// @param services access to the theory operators
    /// @return whether the rewrite is well sorted and no theory support vetoes it
    private boolean allowed(Term from, Term to, Services services) {
        if (to.sort() != from.sort() && !to.sort().extendsTrans(from.sort())) {
            return false;
        }
        for (final TheoryReasoning support : services.getProfile().getTheorySupports(false)) {
            if (!support.allowsEqualityRewrite(from, to, services)) {
                return false;
            }
        }
        return true;
    }

    /// Rewrites `t` bottom-up, replacing each subterm by its class representative. Quantified
    /// subterms are left untouched.
    ///
    /// @param t a term to normalize
    /// @param tf the factory that rebuilds a rewritten term
    /// @return the congruence-normal form of `t`
    Term normalize(Term t, TermFactory tf) {
        if (parent.isEmpty() || !t.boundVars().isEmpty()) {
            return t;
        }
        // Shared subterms are re-normalized once per occurrence otherwise, and the literals of
        // one congruence are normalized again for every candidate instance sharing it.
        final Term memo = normalized.get(t);
        if (memo != null) {
            return memo;
        }
        final Term result = normalizeUncached(t, tf);
        normalized.put(t, result);
        return result;
    }

    private Term normalizeUncached(Term t, TermFactory tf) {
        if (t.arity() == 0) {
            return find(t);
        }
        final Term[] subs = new Term[t.arity()];
        boolean changed = false;
        for (int i = 0; i < t.arity(); i++) {
            subs[i] = normalize(t.sub(i), tf);
            if (subs[i] != t.sub(i)) {
                changed = true;
            }
        }
        if (!changed) {
            return find(t);
        }
        try {
            return find(tf.createTerm(t.op(), subs));
        } catch (org.key_project.logic.TermCreationException e) {
            // the orientation keeps rewrites sort-compatible, but an operator with a stricter
            // requirement can still reject the rebuilt term; this is only a heuristic, so give
            // the term up unnormalised rather than fail the proof search
            return t;
        }
    }
}
