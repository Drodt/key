/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.proof;

import java.util.Map;

import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentChangeInfo;
import org.key_project.prover.strategy.NewRuleListener;
import org.key_project.rusty.Services;
import org.key_project.rusty.proof.PrefixTermTacletAppIndexCacheImpl.CacheKey;
import org.key_project.rusty.rule.*;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;


public class TacletAppIndex {
    private final TacletIndex tacletIndex;

    private SemisequentTacletAppIndex antecIndex;
    private SemisequentTacletAppIndex succIndex;

    private TermTacletAppIndexCacheSet indexCaches;

    private final Goal goal;

    /// The sequent with the formulas for which taclet indices are hold by this object. Invariant:
    /// <code>seq != null</code> implies that the indices <code>antecIndex</code>,
    /// <code>succIndex</code> are up-to-date for the sequent <code>seq</code>
    private Sequent seq;

    /// Object to which the appearance of new taclet apps is reported
    private NewRuleListener newRuleListener = NullNewRuleListener.INSTANCE;

    private final Map<CacheKey, TermTacletAppIndex> cache;

    public TacletAppIndex(TacletIndex tacletIndex, Goal goal, Services services) {
        this(tacletIndex, null, null, goal, null,
            new TermTacletAppIndexCacheSet(services.getCaches().getTermTacletAppIndexCache()),
            services.getCaches().getTermTacletAppIndexCache());
    }

    private TacletAppIndex(TacletIndex tacletIndex, SemisequentTacletAppIndex antecIndex,
            SemisequentTacletAppIndex succIndex, @NonNull Goal goal,
            Sequent seq, TermTacletAppIndexCacheSet indexCaches,
            Map<CacheKey, TermTacletAppIndex> cache) {
        this.tacletIndex = tacletIndex;
        this.antecIndex = antecIndex;
        this.succIndex = succIndex;
        this.goal = goal;
        this.seq = seq;
        this.indexCaches = indexCaches;
        this.cache = cache;
    }

    static TacletApp createTacletApp(NoPosTacletApp tacletApp, PosInOccurrence pos,
            Services services) {
        if (tacletApp.taclet() instanceof FindTaclet) {
            return tacletApp.setPosInOccurrence(pos, services);
        } else {
            return tacletApp;
        }
    }

    public TacletIndex tacletIndex() {
        return tacletIndex;
    }

    public ImmutableList<TacletApp> getTacletAppAt(PosInOccurrence pos, Services services) {
        return prepend(getFindTacletWithPos(pos, services), getNoFindTaclet(services));
    }

    private ImmutableList<TacletApp> getFindTacletWithPos(PosInOccurrence pos,
            Services services) {
        ImmutableList<NoPosTacletApp> tacletInsts = getFindTaclet(pos);
        return createTacletApps(tacletInsts, pos, services);
    }


    /// collects all NoFindTacletInstantiations
    ///
    /// @param services the Services object encapsulating information about the Rust datastructures
    /// like (static)types etc.
    /// @return list of all possible instantiations
    public ImmutableList<NoPosTacletApp> getNoFindTaclet(Services services) {
        return tacletIndex().getNoFindTaclet(services);
    }

    /// collects all FindTaclets with instantiations and position
    ///
    /// @param pos the PosInOccurrence to focus
    /// @return list of all possible instantiations
    public ImmutableList<NoPosTacletApp> getFindTaclet(PosInOccurrence pos) {
        return getIndex(pos).getTacletAppAt(pos);
    }

    private void ensureIndicesExist() {
        if (isOutdated()) {
            // Indices are not up-to-date
            createAllFromGoal();
        }
    }

    private void createAllFromGoal() {
        this.seq = getNode().sequent();

        antecIndex =
            new SemisequentTacletAppIndex(getSequent(), true, getServices(), tacletIndex(),
                newRuleListener, indexCaches);
        succIndex =
            new SemisequentTacletAppIndex(getSequent(), false, getServices(), tacletIndex(),
                newRuleListener, indexCaches);
    }

    private Services getServices() {
        return getProof().getServices();
    }

    private Proof getProof() {
        return getNode().proof();
    }

    /// @return true iff this index is currently outdated with respect to the sequent of the
    /// associated goal; this does not detect other modifications
    /// like an altered user
    /// constraint
    private boolean isOutdated() {
        return getGoal() == null || getSequent() != getNode().sequent();
    }

    private Goal getGoal() {
        return goal;
    }

    private Sequent getSequent() {
        return seq;
    }

    private Node getNode() {
        return goal.getNode();
    }

    private SemisequentTacletAppIndex getIndex(PosInOccurrence pos) {
        ensureIndicesExist();
        return pos.isInAntec() ? antecIndex : succIndex;
    }

    public void setNewRuleListener(NewRuleListener newRuleListener) {
        this.newRuleListener = newRuleListener;
    }

    private static ImmutableList<TacletApp> prepend(ImmutableList<TacletApp> l1,
            ImmutableList<NoPosTacletApp> l2) {
        for (NoPosTacletApp aL2 : l2) {
            l1 = l1.prepend(aL2);
        }
        return l1;
    }

    /// creates TacletApps out of each single NoPosTacletApp object
    ///
    /// @param tacletInsts the list of NoPosTacletApps the TacletApps are to be created from
    /// @param pos the PosInOccurrence to focus
    /// @return list of all created TacletApps
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

    /// returns a new TacletAppIndex with a given TacletIndex
    TacletAppIndex copyWith(TacletIndex p_tacletIndex, Goal goal) {
        return new TacletAppIndex(p_tacletIndex, antecIndex, succIndex, goal, getSequent(),
            indexCaches, cache);
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
            updateIndices(sci);
        }
    }

    private void updateIndices(SequentChangeInfo sci) {
        seq = sci.sequent();

        antecIndex =
            antecIndex.sequentChanged(sci, getServices(), tacletIndex, newRuleListener);

        succIndex =
            succIndex.sequentChanged(sci, getServices(), tacletIndex, newRuleListener);
    }

    /// updates the internal caches after a new Taclet with instantiation information has been added
    /// to the TacletIndex.
    ///
    /// @param tacletApp the partially instantiated Taclet to add
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
            newRuleListener.ruleAdded(tacletApp, null);
            return;
        }

        updateIndices(tacletApp);
    }

    private void updateIndices(final NoPosTacletApp newTaclet) {
        antecIndex =
            antecIndex.addSingleTaclet(newTaclet, getServices(), tacletIndex, newRuleListener);
        succIndex =
            succIndex.addSingleTaclet(newTaclet, getServices(), tacletIndex, newRuleListener);
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

    /// returns the rule applications at the given PosInOccurrence and at all Positions below this.
    /// The method calls getTacletAppAt for all the Positions below.
    ///
    /// @param pos the position where to start from
    /// @param services the Services object encapsulating information about the java datastructures
    /// like (static)types etc.
    /// @return the possible rule applications
    public ImmutableList<TacletApp> getTacletAppAtAndBelow(PosInOccurrence pos,
            Services services) {
        final ImmutableList<TacletApp> findTaclets =
            getIndex(pos).getTacletAppAtAndBelow(pos, services);
        return prepend(findTaclets, getNoFindTaclet(services));
    }

    /// Reports all cached rule apps. Calls ruleAdded on the given NewRuleListener for every cached
    /// taclet app.
    public void reportRuleApps(NewRuleListener l, Services services) {
        if (antecIndex != null) {
            antecIndex.reportRuleApps(l);
        }
        if (succIndex != null) {
            succIndex.reportRuleApps(l);
        }

        l.rulesAdded(getNoFindTaclet(services), null);
    }

    /// Forces all delayed computations to be performed, so that the cache is fully up-to-date
    /// (NewRuleListener gets informed)
    public void fillCache() {
        ensureIndicesExist();
    }
}
