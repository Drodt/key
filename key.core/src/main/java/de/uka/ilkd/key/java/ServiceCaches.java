/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.java;

import java.util.Map;
import java.util.WeakHashMap;

import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.PrefixTermTacletAppIndexCacheImpl.CacheKey;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.TermTacletAppIndex;
import de.uka.ilkd.key.proof.TermTacletAppIndexCacheSet;
import de.uka.ilkd.key.rule.metaconstruct.arith.Monomial;
import de.uka.ilkd.key.rule.metaconstruct.arith.Polynomial;
import de.uka.ilkd.key.strategy.IfInstantiationCachePool;
import de.uka.ilkd.key.strategy.feature.AbstractBetaFeature.TermInfo;
import de.uka.ilkd.key.strategy.feature.AppliedRuleAppsNameCache;
import de.uka.ilkd.key.strategy.quantifierHeuristics.ClausesGraph;
import de.uka.ilkd.key.strategy.quantifierHeuristics.Metavariable;
import de.uka.ilkd.key.strategy.quantifierHeuristics.TriggersSet;

import org.key_project.logic.op.Operator;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.proof.SessionCaches;
import org.key_project.prover.rules.instantiation.caches.AssumesFormulaInstantiationCache;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.util.LRUCache;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Pair;

/**
 * <p>
 * Instances of this class provides all caches used by an individual {@link Proof} or more precise
 * by its {@link Services}.
 * </p>
 * <p>
 * This is a redesign of the old static caches which were implemented via final static {@link Map}s
 * like
 * {@code private static final Map<CacheKey, TermTacletAppIndex> termTacletAppIndexCache = new LRUCache<CacheKey, TermTacletAppIndex> ( MAX_TERM_TACLET_APP_INDEX_ENTRIES );}.
 * </p>
 * <p>
 * The old idea that memory is reused and shared between multiple {@link Proof}s by static variables
 * is wrong, because in practice it wastes memory. The problem is that cached data structures can
 * become large, especially in case of {@link #getTermTacletAppIndexCache()}. The static cache is
 * filled with these large data structures and not freed even if all {@link Proof}s are disposed
 * ({@link Proof#isDisposed()}). This can fill quickly (about 30 done {@link Proof}s) the whole
 * memory. A new {@link Proof} which does not profit from the cached data structure has then no free
 * memory to live in which makes the whole <b>system unusable slow</b>.
 * </p>
 * <p>
 * The goal of this new design is to avoid all static cache variables and to collect them instead as
 * instance variables in this class. Each {@link Proof} has its own {@link Services} which provides
 * a {@link ServiceCaches} instance to use via {@link Services#getCaches()}. The advantages are:
 * <ul>
 * <li>The cache contains only usable content and nothing from other {@link Proof}s not relevant for
 * the current {@link Proof}.</li>
 * <li>The whole memory is freed when a {@link Proof} is disposed via {@link Proof#dispose()}.</li>
 * <li>Multiple {@link Proof}s at the same time are faster because they can fill the cache up to the
 * fixed limit. Also the user interface profits from it when a user switches between proofs.</li>
 * <li>Even if multiple large {@link Proof}s exist at the same time it seems to be no problem that
 * multiple caches exist.</li>
 * <li>The old behavior in which multiple {@link Proof}s use the same cache can be realized just by
 * using the same {@link ServiceCaches} instance. This can be useful for instance in side
 * proofs.</li>
 * </ul>
 * </p>
 *
 * @author Martin Hentschel
 */
public class ServiceCaches implements SessionCaches {
    /**
     * The maximal number of index entries in {@link #getTermTacletAppIndexCache()}.
     */
    public static final int MAX_TERM_TACLET_APP_INDEX_ENTRIES = 5000;

    /**
     * The cache used by {@link TermTacletAppIndexCacheSet} instances.
     */
    private final Map<CacheKey, TermTacletAppIndex> termTacletAppIndexCache =
        new LRUCache<>(MAX_TERM_TACLET_APP_INDEX_ENTRIES);

    /*
     * Table of formulas which could be splitted using the beta rule This is the cache the method
     * "isBetaCandidate" uses
     *
     * keys: Term values: TermInfo
     */
    private final LRUCache<JTerm, TermInfo> betaCandidates = new LRUCache<>(1000);

    private final LRUCache<PosInOccurrence, RuleAppCost> ifThenElseMalusCache =
        new LRUCache<>(1000);

    private final LRUCache<Operator, Integer> introductionTimeCache =
        new LRUCache<>(10000);

    private final LRUCache<org.key_project.logic.Term, Monomial> monomialCache =
        new LRUCache<>(2000);

    private final LRUCache<org.key_project.logic.Term, Polynomial> polynomialCache =
        new LRUCache<>(2000);

    /**
     * a <code>HashMap</code> from <code>Term</code> to <code>TriggersSet</code> uses to cache all
     * created TriggersSets
     */
    private final Map<org.key_project.logic.Term, TriggersSet> triggerSetCache =
        new LRUCache<>(1000);

    /**
     * Map from <code>Term</code>(allTerm) to <code>ClausesGraph</code>
     */
    private final Map<org.key_project.logic.Term, ClausesGraph> graphCache = new LRUCache<>(1000);

    /**
     * Cache used by the TermFactory to avoid unnecessary creation of terms
     */
    private final Map<JTerm, JTerm> termCache = new LRUCache<>(20000);

    /**
     * Cache used by TypeComparisonCondition
     */
    private final Map<Sort, Map<Sort, Boolean>> disjointnessCache =
        new WeakHashMap<>();

    /**
     * Cache used by HandleArith for caching formatted terms
     */
    private final LRUCache<JTerm, JTerm> formattedTermCache = new LRUCache<>(5000);

    /**
     * Caches used bu HandleArith to cache proof results
     */
    private final LRUCache<JTerm, JTerm> provedByArithFstCache = new LRUCache<>(5000);

    private final LRUCache<Pair<JTerm, JTerm>, JTerm> provedByArithSndCache =
        new LRUCache<>(5000);

    /** Cache used by the exhaustive macro */
    private final Map<Node, PosInOccurrence> exhaustiveMacroCache =
        new WeakHashMap<>();

    /** Cache used by the ifinstantiator */
    private final IfInstantiationCachePool ifInstantiationCache = new IfInstantiationCachePool();

    /** Cache used IfFormulaInstSeq */
    private final AssumesFormulaInstantiationCache assumesFormulaInstantiationCache =
        new AssumesFormulaInstantiationCache();

    /** applied rule apps name cache */
    private final AppliedRuleAppsNameCache appliedRuleAppsNameCache =
        new AppliedRuleAppsNameCache();

    /** Cache used by EqualityConstraint to speed up meta variable search */
    private final LRUCache<org.key_project.logic.Term, ImmutableSet<Metavariable>> mvCache =
        new LRUCache<>(2000);


    /**
     * Returns the cache used by {@link TermTacletAppIndexCacheSet} instances.
     *
     * @return The cache used by {@link TermTacletAppIndexCacheSet} instances.
     */
    public final Map<CacheKey, TermTacletAppIndex> getTermTacletAppIndexCache() {
        return termTacletAppIndexCache;
    }

    public final LRUCache<JTerm, TermInfo> getBetaCandidates() {
        return betaCandidates;
    }

    public final LRUCache<PosInOccurrence, RuleAppCost> getIfThenElseMalusCache() {
        return ifThenElseMalusCache;
    }

    public final LRUCache<Operator, Integer> getIntroductionTimeCache() {
        return introductionTimeCache;
    }

    public final LRUCache<org.key_project.logic.Term, Monomial> getMonomialCache() {
        return monomialCache;
    }

    public final LRUCache<org.key_project.logic.Term, Polynomial> getPolynomialCache() {
        return polynomialCache;
    }

    public final Map<org.key_project.logic.Term, TriggersSet> getTriggerSetCache() {
        return triggerSetCache;
    }

    public final Map<org.key_project.logic.Term, ClausesGraph> getGraphCache() {
        return graphCache;
    }

    public final Map<JTerm, JTerm> getTermFactoryCache() {
        return termCache;
    }

    public final Map<Sort, Map<Sort, Boolean>> getDisjointnessCache() {
        return disjointnessCache;
    }

    public final LRUCache<JTerm, JTerm> getFormattedTermCache() {
        return formattedTermCache;
    }

    public final LRUCache<JTerm, JTerm> getProvedByArithFstCache() {
        return provedByArithFstCache;
    }

    public final LRUCache<Pair<JTerm, JTerm>, JTerm> getProvedByArithSndCache() {
        return provedByArithSndCache;
    }

    public final Map<Node, PosInOccurrence> getExhaustiveMacroCache() {
        return exhaustiveMacroCache;
    }

    public final IfInstantiationCachePool getIfInstantiationCache() {
        return ifInstantiationCache;
    }

    public final AssumesFormulaInstantiationCache getAssumesFormulaInstantiationCache() {
        return assumesFormulaInstantiationCache;
    }

    public AppliedRuleAppsNameCache getAppliedRuleAppsNameCache() {
        return appliedRuleAppsNameCache;
    }

    public LRUCache<org.key_project.logic.Term, ImmutableSet<Metavariable>> getMVCache() {
        return mvCache;
    }

}
