/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty;


import java.util.Map;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.prover.caches.AssumesInstantiationCachePool;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.prover.rules.instantiation.caches.AssumesFormulaInstantiationCache;
import org.key_project.rusty.proof.Node;
import org.key_project.rusty.rule.metaconstruct.arith.Monomial;
import org.key_project.rusty.rule.metaconstruct.arith.Polynomial;
import org.key_project.rusty.strategy.feature.AbstractBetaFeature.TermInfo;
import org.key_project.rusty.strategy.feature.AppliedRuleAppsNameCache;
import org.key_project.rusty.strategy.quantifierHeuristics.Metavariable;
import org.key_project.rusty.strategy.quantifierHeuristics.TriggersSet;
import org.key_project.util.LRUCache;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Pair;

import org.jspecify.annotations.NonNull;

/// Instances of this class provides all caches used by an individual [Proof] or more precise
/// by its [Services].
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
/// The goal of this new design is to avoid all static cache variables and to collect them instead
/// as
/// instance variables in this class. Each [Proof] has its own [Services] which provides
/// a [ServiceCaches] instance to use via [#getCaches()]. The advantages are:
///
/// - The cache contains only usable content and nothing from other [Proof]s not relevant for
/// the current [Proof].
/// - The whole memory is freed when a [Proof] is disposed via [#dispose()].
/// - Multiple [Proof]s at the same time are faster because they can fill the cache up to the
/// fixed limit. Also, the user interface profits from it when a user switches between proofs.
/// - Even if multiple large [Proof]s exist at the same time it seems to be no problem that
/// multiple caches exist.
/// - The old behavior in which multiple [Proof]s use the same cache can be realized just by
/// using the same [ServiceCaches] instance. This can be useful for instance in side
/// proofs.
///
/// @author Martin Hentschel
public class ServiceCaches implements SessionCaches {
    private final LRUCache<@NonNull Term, @NonNull Monomial> monomialCache = new LRUCache<>(2000);

    /// applied rule apps name cache
    private final AppliedRuleAppsNameCache appliedRuleAppsNameCache =
        new AppliedRuleAppsNameCache();

    /// Cache used IfFormulaInstSeq
    private final AssumesFormulaInstantiationCache assumesFormulaInstantiationCache =
        new AssumesFormulaInstantiationCache();

    /// Caches used by HandleArith to cache proof results
    private final LRUCache<@NonNull Term, @NonNull Term> provedByArithFstCache =
        new LRUCache<>(5000);
    private final LRUCache<@NonNull Pair<@NonNull Term, @NonNull Term>, @NonNull Term> provedByArithSndCache =
        new LRUCache<>(5000);

    private final LRUCache<@NonNull Term, @NonNull Polynomial> polynomialCache =
        new LRUCache<>(2000);
    private LRUCache<@NonNull Term, @NonNull ImmutableSet<@NonNull Metavariable>> mvCache =
        new LRUCache<>(2000);
    private final AssumesInstantiationCachePool<Node> assumesInstantiationCache =
        new AssumesInstantiationCachePool<>();

    // private final LRUCache<Term, Polynomial> polynomialCache = new LRUCache<>(2000);

    public final LRUCache<@NonNull Term, @NonNull Monomial> getMonomialCache() {
        return monomialCache;
    }

    private final LRUCache<@NonNull Term, @NonNull Term> formattedTermCache = new LRUCache<>(5000);

    private final LRUCache<@NonNull Term, @NonNull TermInfo> betaCandidates = new LRUCache<>(1000);

    private final LRUCache<@NonNull Operator, @NonNull Integer> introductionTimeCache =
        new LRUCache<>(10000);

    /// a <code>HashMap</code> from <code>Term</code> to <code>TriggersSet</code> uses to cache all
    /// created TriggersSets
    private final Map<Term, TriggersSet> triggerSetCache =
        new LRUCache<>(1000);

    @Override
    public AssumesFormulaInstantiationCache getAssumesFormulaInstantiationCache() {
        return assumesFormulaInstantiationCache;
    }

    public AppliedRuleAppsNameCache getAppliedRuleAppsNameCache() {
        return appliedRuleAppsNameCache;
    }

    public LRUCache<@NonNull Term, @NonNull Term> getProvedByArithFstCache() {
        return provedByArithFstCache;
    }

    public LRUCache<@NonNull Term, @NonNull Polynomial> getPolynomialCache() {
        return polynomialCache;
    }

    public LRUCache<@NonNull Pair<@NonNull Term, @NonNull Term>, @NonNull Term> getProvedByArithSndCache() {
        return provedByArithSndCache;
    }

    public LRUCache<@NonNull Term, @NonNull Term> getFormattedTermCache() {
        return formattedTermCache;
    }

    public final LRUCache<@NonNull Term, @NonNull TermInfo> getBetaCandidates() {
        return betaCandidates;
    }

    public LRUCache<@NonNull Operator, @NonNull Integer> getIntroductionTimeCache() {
        return introductionTimeCache;
    }

    public Map<Term, TriggersSet> getTriggerSetCache() {
        return triggerSetCache;
    }

    public LRUCache<@NonNull Term, @NonNull ImmutableSet<@NonNull Metavariable>> getMVCache() {
        return mvCache;
    }

    public AssumesInstantiationCachePool<Node> getAssumesInstantiationCache() {
        return assumesInstantiationCache;
    }
}
