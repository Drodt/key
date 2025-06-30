/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty;


import org.key_project.logic.Term;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.prover.rules.instantiation.caches.AssumesFormulaInstantiationCache;
import org.key_project.rusty.rule.metaconstruct.arith.Monomial;
import org.key_project.util.LRUCache;

///
/// Instances of this class provides all caches used by an individual [Proof] or more precise
/// by its [Services].
///
///
/// This is a redesign of the old static caches which were implemented via final static [Map]s
/// like
/// `private static final Map<CacheKey, TermTacletAppIndex> termTacletAppIndexCache = new
/// LRUCache<CacheKey, TermTacletAppIndex> ( MAX_TERM_TACLET_APP_INDEX_ENTRIES );`.
///
///
/// The old idea that memory is reused and shared between multiple [Proof]s by static variables
/// is wrong, because in practice it wastes memory. The problem is that cached data structures can
/// become large, especially in case of [#getTermTacletAppIndexCache()]. The static cache is
/// filled with these large data structures and not freed even if all [Proof]s are disposed
/// ([#isDisposed()]). This can fill quickly (about 30 done [Proof]s) the whole
/// memory. A new [Proof] which does not profit from the cached data structure has then no free
/// memory to live in which makes the whole **system unusable slow**.
///
///
/// The goal of this new design is to avoid all static cache variables and to collect them instead
/// as
/// instance variables in this class. Each [Proof] has its own [Services] which provides
/// a [ServiceCaches] instance to use via [#getCaches()]. The advantages are:
///
/// - The cache contains only usable content and nothing from other [Proof]s not relevant for
/// the current [Proof].
/// - The whole memory is freed when a [Proof] is disposed via [#dispose()].
/// - Multiple [Proof]s at the same time are faster because they can fill the cache up to the
/// fixed limit. Also the user interface profits from it when a user switches between proofs.
/// - Even if multiple large [Proof]s exist at the same time it seems to be no problem that
/// multiple caches exist.
/// - The old behavior in which multiple [Proof]s use the same cache can be realized just by
/// using the same [ServiceCaches] instance. This can be useful for instance in side
/// proofs.
///
///
///
/// @author Martin Hentschel
public class ServiceCaches implements SessionCaches {
    private final LRUCache<Term, Monomial> monomialCache = new LRUCache<>(2000);

    /// Cache used IfFormulaInstSeq
    private final AssumesFormulaInstantiationCache assumesFormulaInstantiationCache =
        new AssumesFormulaInstantiationCache();

    // private final LRUCache<Term, Polynomial> polynomialCache = new LRUCache<>(2000);

    public final LRUCache<Term, Monomial> getMonomialCache() {
        return monomialCache;
    }

    @Override
    public AssumesFormulaInstantiationCache getAssumesFormulaInstantiationCache() {
        return assumesFormulaInstantiationCache;
    }
}
