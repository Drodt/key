/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.proof;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.proof.PrefixTermTacletAppIndexCacheImpl.CacheKey;
import de.uka.ilkd.key.rule.*;
import de.uka.ilkd.key.util.Debug;

import org.key_project.prover.proof.rulefilter.AndRuleFilter;
import org.key_project.prover.proof.rulefilter.RuleFilter;
import org.key_project.prover.proof.rulefilter.SetRuleFilter;
import org.key_project.prover.proof.rulefilter.TacletFilter;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.strategy.NewRuleListener;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;


/**
 * the class manages the available TacletApps. This index has to be used if one wants to ask for the
 * available taclet application at a specific position (or the whole sequent if taclet is a nofind
 * rule). This means all taclet applications that have a position information are managed by this
 * index. For all others the class TacletIndex is used. This index uses also the TacletIndex to
 * calculate new TacletApps.
 */

public class TacletAppIndex {
    public static final AtomicLong PERF_CREATE_ALL = new AtomicLong();
    public static final AtomicLong PERF_UPDATE = new AtomicLong();

    private final TacletIndex tacletIndex;

    private SemisequentTacletAppIndex antecIndex;
    private SemisequentTacletAppIndex succIndex;

    private TermTacletAppIndexCacheSet indexCaches;

    private final Goal goal;

    /**
     * Object to which the appearance of new taclet apps is reported
     */
    private NewRuleListener newRuleListener = NullNewRuleListener.INSTANCE;

    /**
     * Filter that is used to restrict the set of taclets that are considered as possible members of
     * this index. This is used to distinguish between <code>TacletAppIndex</code> s exclusively for
     * automatic or interactive taclets.
     */
    private RuleFilter ruleFilter;

    /**
     * The sequent with the formulas for which taclet indices are hold by this object. Invariant:
     * <code>seq != null</code> implies that the indices <code>antecIndex</code>,
     * <code>succIndex</code> are up-to-date for the sequent <code>seq</code>
     */
    private Sequent seq;

    private final Map<CacheKey, TermTacletAppIndex> cache;

    public TacletAppIndex(TacletIndex tacletIndex, Goal goal, Services services) {
        this(tacletIndex, null, null, goal, null, TacletFilter.TRUE,
            new TermTacletAppIndexCacheSet(services.getCaches().getTermTacletAppIndexCache()),
            services.getCaches().getTermTacletAppIndexCache());
    }

    private TacletAppIndex(TacletIndex tacletIndex, SemisequentTacletAppIndex antecIndex,
            SemisequentTacletAppIndex succIndex, @NonNull Goal goal, Sequent seq,
            RuleFilter ruleFilter,
            TermTacletAppIndexCacheSet indexCaches, Map<CacheKey, TermTacletAppIndex> cache) {
        this.tacletIndex = tacletIndex;
        this.antecIndex = antecIndex;
        this.succIndex = succIndex;
        this.goal = goal;
        this.seq = seq;
        this.ruleFilter = ruleFilter;
        this.indexCaches = indexCaches;
        this.cache = cache;
    }

    public void setNewRuleListener(NewRuleListener p_newRuleListener) {
        newRuleListener = p_newRuleListener;
    }

    public void setRuleFilter(RuleFilter p_ruleFilter) {
        if (p_ruleFilter != ruleFilter) {
            ruleFilter = p_ruleFilter;
            clearAndDetachCache();
        }
    }

    /**
     * returns a new TacletAppIndex with a given TacletIndex
     */
    TacletAppIndex copyWith(TacletIndex p_tacletIndex, Goal goal) {
        return new TacletAppIndex(p_tacletIndex, antecIndex, succIndex, goal, getSequent(),
            ruleFilter, indexCaches, cache);
    }

    /**
     * Delete all cached information about taclet apps. This also makes the index cache of this
     * index independent from the caches of other indexes (expensive)
     */
    public void clearAndDetachCache() {
        clearIndexes();
        createNewIndexCache();
    }

    public void clearIndexes() {
        seq = null; // This leads to a delayed rebuild
        antecIndex = null;
        succIndex = null;
    }

    private void createNewIndexCache() {
        indexCaches = new TermTacletAppIndexCacheSet(cache);
        if (antecIndex != null) {
            antecIndex.setIndexCache(indexCaches);
        }
        if (succIndex != null) {
            succIndex.setIndexCache(indexCaches);
        }
    }

    /**
     * Forces all delayed computations to be performed, so that the cache is fully up-to-date
     * (NewRuleListener gets informed)
     */
    public void fillCache() {
        ensureIndicesExist();
    }

    private void createAllFromGoal() {
        var time = System.nanoTime();
        try {
            this.seq = getNode().sequent();

            antecIndex =
                new SemisequentTacletAppIndex(getSequent(), true, getServices(), tacletIndex(),
                    newRuleListener, ruleFilter, indexCaches);
            succIndex =
                new SemisequentTacletAppIndex(getSequent(), false, getServices(), tacletIndex(),
                    newRuleListener, ruleFilter, indexCaches);
        } finally {
            PERF_CREATE_ALL.getAndAdd(System.nanoTime() - time);
        }
    }

    private void ensureIndicesExist() {
        if (isOutdated()) {
            // Indices are not up-to-date
            createAllFromGoal();
        }
    }

    /**
     * @return true iff this index is currently outdated with respect to the sequent of the
     *         associated goal; this does not detect other modifications
     *         like an altered user
     *         constraint
     */
    private boolean isOutdated() {
        return getGoal() == null || getSequent() != getNode().sequent();
    }

    private SemisequentTacletAppIndex getIndex(PosInOccurrence pos) {
        ensureIndicesExist();
        return pos.isInAntec() ? antecIndex : succIndex;
    }

    private ImmutableList<TacletApp> getFindTacletWithPos(
            PosInOccurrence pos, TacletFilter filter,
            Services services) {
        Debug.assertFalse(pos == null);
        ImmutableList<NoPosTacletApp> tacletInsts = getFindTaclet(pos, filter);
        return createTacletApps(tacletInsts, pos, services);
    }


    /**
     * returns the set of rule applications at the given position of the given sequent.
     *
     * @param pos the PosInOccurrence to focus
     */
    public ImmutableList<TacletApp> getTacletAppAt(
            PosInOccurrence pos, TacletFilter filter,
            Services services) {
        ImmutableList<TacletApp> sal = getFindTacletWithPos(pos, filter, services);
        return prepend(sal, getNoFindTaclet(filter, services));
    }

    /**
     * creates TacletApps out of each single NoPosTacletApp object
     *
     * @param tacletInsts the list of NoPosTacletApps the TacletApps are to be created from
     * @param pos the PosInOccurrence to focus
     * @return list of all created TacletApps
     */
    static ImmutableList<TacletApp> createTacletApps(ImmutableList<NoPosTacletApp> tacletInsts,
            PosInOccurrence pos, Services services) {
        ImmutableList<TacletApp> result = ImmutableSLList.nil();
        for (NoPosTacletApp tacletApp : tacletInsts) {
            if (tacletApp.taclet() instanceof FindTaclet) {
                PosTacletApp newTacletApp = tacletApp.setPosInOccurrence(pos, services);
                if (newTacletApp != null) {
                    result = result.prepend(newTacletApp);
                }
            } else {
                result = result.prepend(tacletApp);
            }
        }
        return result;
    }

    static TacletApp createTacletApp(NoPosTacletApp tacletApp,
            PosInOccurrence pos,
            Services services) {
        if (tacletApp.taclet() instanceof FindTaclet) {
            return tacletApp.setPosInOccurrence(pos, services);
        } else {
            return tacletApp;
        }
    }

    /**
     * collects all NoFindTacletInstantiations
     *
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     * @return list of all possible instantiations
     */
    public ImmutableList<NoPosTacletApp> getNoFindTaclet(TacletFilter filter, Services services) {
        RuleFilter effectiveFilter = new AndRuleFilter(filter, ruleFilter);
        return tacletIndex().getNoFindTaclet(effectiveFilter, services);
    }

    /**
     * collects all RewriteTacletInstantiations in a subterm of the constrainedFormula described by
     * a PosInOccurrence. RewriteTaclets with wrong prefix are filtered out.
     *
     * @param pos the PosInOccurrence to focus
     * @return list of all possible instantiations
     */
    public ImmutableList<NoPosTacletApp> getRewriteTaclet(
            PosInOccurrence pos,
            TacletFilter filter) {

        final Iterator<NoPosTacletApp> it = getFindTaclet(pos, filter).iterator();

        ImmutableList<NoPosTacletApp> result = ImmutableSLList.nil();

        while (it.hasNext()) {
            final NoPosTacletApp tacletApp = it.next();
            final Taclet t = tacletApp.taclet();
            if (t instanceof RewriteTaclet && ((RewriteTaclet) t).checkPrefix(pos,
                MatchConditions.EMPTY_MATCHCONDITIONS) != null) {
                result = result.prepend(tacletApp);
            }
        }

        return result;
    }

    /**
     * collects all FindTaclets with instantiations and position
     *
     * @param pos the PosInOccurrence to focus
     * @return list of all possible instantiations
     */
    public ImmutableList<NoPosTacletApp> getFindTaclet(
            PosInOccurrence pos, TacletFilter filter) {
        return getIndex(pos).getTacletAppAt(pos, filter);
    }


    /**
     * returns the rule applications at the given PosInOccurrence and at all Positions below this.
     * The method calls getTacletAppAt for all the Positions below.
     *
     * @param pos the position where to start from
     * @param services the Services object encapsulating information about the java datastructures
     *        like (static)types etc.
     * @return the possible rule applications
     */
    public ImmutableList<TacletApp> getTacletAppAtAndBelow(PosInOccurrence pos, TacletFilter filter,
            Services services) {
        final ImmutableList<TacletApp> findTaclets =
            getIndex(pos).getTacletAppAtAndBelow(pos, filter, services);
        return prepend(findTaclets, getNoFindTaclet(filter, services));
    }

    /**
     * called if a formula has been replaced
     *
     * @param sci SequentChangeInfo describing the change of the sequent
     */
    public void sequentChanged(
            SequentChangeInfo sci) {
        if (sci.getOriginalSequent() != getSequent()) {
            // we are not up-to-date and have to rebuild everything (lazy)
            clearIndexes();
        } else {
            var time = System.nanoTime();
            updateIndices(sci);
            PERF_UPDATE.getAndAdd(System.nanoTime() - time);
        }
    }

    private void updateIndices(SequentChangeInfo sci) {
        seq = sci.sequent();

        antecIndex =
            antecIndex.sequentChanged(sci, getServices(), tacletIndex, newRuleListener);

        succIndex =
            succIndex.sequentChanged(sci, getServices(), tacletIndex, newRuleListener);
    }

    private void updateIndices(final SetRuleFilter newTaclets) {
        antecIndex =
            antecIndex.addTaclets(newTaclets, getServices(), tacletIndex, newRuleListener);
        succIndex =
            succIndex.addTaclets(newTaclets, getServices(), tacletIndex, newRuleListener);
    }


    /**
     * updates the internal caches after a new Taclet with instantiation information has been added
     * to the TacletIndex.
     *
     * @param tacletApp the partially instantiated Taclet to add
     */
    public void addedNoPosTacletApp(NoPosTacletApp tacletApp) {
        if (indexCaches.isRelevantTaclet(tacletApp.taclet())) {
            // we must flush the index cache, and we must no longer use a cache
            // that we share with other instances of <code>TacletAppIndex</code>
            // (that maybe live of different goals)
            createNewIndexCache();
        }

        if (isOutdated()) {
            // we are not up-to-date and have to rebuild everything (lazy)
            clearIndexes();
            return;
        }

        if (tacletApp.taclet() instanceof NoFindTaclet) {
            if (ruleFilter.filter(tacletApp.taclet())) {
                newRuleListener.ruleAdded(tacletApp, null);
            }
            return;
        }

        final SetRuleFilter newTaclets = new SetRuleFilter();
        newTaclets.addRuleToSet(tacletApp.taclet());

        updateIndices(newTaclets);
    }

    /**
     * updates the internal caches after a new Taclet with instantiation information has been added
     * to the TacletIndex.
     *
     * @param tacletApps set of partially instantiated {@link Taclet}s to add
     */
    public void addedNoPosTacletApps(Iterable<NoPosTacletApp> tacletApps) {
        for (TacletApp tacletApp : tacletApps) {
            if (indexCaches.isRelevantTaclet(tacletApp.taclet())) {
                // we must flush the index cache, and we must no longer use a cache
                // that we share with other instances of <code>TacletAppIndex</code>
                // (that maybe live of different goals)
                createNewIndexCache();
                break;
            }
        }

        if (isOutdated()) {
            // we are not up-to-date and have to rebuild everything (lazy)
            clearIndexes();
            return;
        }

        final SetRuleFilter newTaclets = new SetRuleFilter();
        for (NoPosTacletApp tacletApp : tacletApps) {
            if (tacletApp.taclet() instanceof NoFindTaclet) {
                if (ruleFilter.filter(tacletApp.taclet())) {
                    newRuleListener.ruleAdded(tacletApp, null);
                }
            } else {
                newTaclets.addRuleToSet(tacletApp.taclet());
            }
        }

        if (newTaclets.isEmpty()) {
            return;
        }

        updateIndices(newTaclets);
    }



    /**
     * updates the internal caches after a Taclet with instantiation information has been removed
     * from the TacletIndex.
     *
     * @param tacletApp the partially instantiated Taclet to remove
     */
    public void removedNoPosTacletApp(NoPosTacletApp tacletApp) {
        if (indexCaches.isRelevantTaclet(tacletApp.taclet())) {
            // we must flush the index cache, and we must no longer use a cache
            // that we share with other instances of <code>TacletAppIndex</code>
            // (that maybe live of different goals)
            clearAndDetachCache();
        } else {
            clearIndexes();
        }
    }

    public String toString() {
        return "TacletAppIndex with indexing, getting Taclets from" + " TacletIndex "
            + tacletIndex;
    }

    // helper because IList<NoPosTacletApp> is no IList<TacletApp>
    private static ImmutableList<TacletApp> prepend(ImmutableList<TacletApp> l1,
            ImmutableList<NoPosTacletApp> l2) {
        for (NoPosTacletApp aL2 : l2) {
            l1 = l1.prepend(aL2);
        }
        return l1;
    }

    private Goal getGoal() {
        return goal;
    }

    private Sequent getSequent() {
        return seq;
    }

    private Services getServices() {
        return getProof().getServices();
    }

    private Proof getProof() {
        return getNode().proof();
    }

    private Node getNode() {
        return goal.node();
    }

    /**
     * returns the Taclet index for this ruleAppIndex.
     */
    public TacletIndex tacletIndex() {
        return tacletIndex;
    }

    /**
     * Reports all cached rule apps. Calls ruleAdded on the given NewRuleListener for every cached
     * taclet app.
     */
    public void reportRuleApps(NewRuleListener l, Services services) {
        if (antecIndex != null) {
            antecIndex.reportRuleApps(l);
        }
        if (succIndex != null) {
            succIndex.reportRuleApps(l);
        }

        l.rulesAdded(getNoFindTaclet(TacletFilter.TRUE, services), null);
    }
}
