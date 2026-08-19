/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.strategy;

import java.util.*;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.rule.*;
import de.uka.ilkd.key.util.Debug;

import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.prover.indexing.FormulaTagManager;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.instantiation.AssumesFormulaInstSeq;
import org.key_project.prover.rules.instantiation.AssumesFormulaInstantiation;
import org.key_project.prover.rules.instantiation.InstantiationEntry;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.NumberRuleAppCost;
import org.key_project.prover.strategy.costbased.RuleAppCost;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.appcontainer.RuleAppContainer;
import org.key_project.prover.strategy.costbased.feature.Feature;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableMapEntry;
import org.key_project.util.collection.ImmutableSet;

import org.jspecify.annotations.Nullable;

/**
 * Instances of this class are immutable
 */
public abstract class TacletAppContainer extends RuleAppContainer {

    // Implementation note (DB 21/02/2014):
    // It is unlikely that we ever reach 2^31 proof nodes,
    // so age could be changed from long to int.
    // My benchmark tests however suggest that this would not
    // save any memory (at the moment).
    // This is because Java's memory alingment.

    /**
     * Creation time of this container ({@code -1} for an initial/just-loaded container). Since age
     * became a first-class container-level cost term ({@link RuleAppContainer#withAge}) this field
     * no longer feeds the cost; it is purely the {@link AssumesInstantiator} freshness key (was
     * this
     * container built before or after a given if-formula).
     */
    private final long age;
    /**
     * The age-free strategy cost: {@code getCost()} without the goal-age term that
     * {@link RuleAppContainer#withAge} adds. Stored so cost reuse can carry it forward unchanged
     * across re-expansion and only re-add the current age, with no reconstruction arithmetic.
     */
    private final RuleAppCost ageFreeCost;
    /**
     * Whether {@link #ageFreeCost} is the regular strategy cost ({@link Strategy#computeCost}) and
     * so
     * may be carried forward by cost reuse. It is {@code false} only for containers built from a
     * strategy-supplied instantiation cost ({@link Strategy#instantiateApp}, the
     * {@link #instantiateApp} collector): for those the stored cost is the instantiation score,
     * which
     * differs from a regular re-cost (e.g. an instantiated quantifier app re-costs to infinity), so
     * reusing it would be unsound. Such containers must always be re-costed normally instead.
     */
    private final boolean ageFreeCostIsRegular;

    protected TacletAppContainer(RuleApp p_app, RuleAppCost p_ageFreeCost,
            boolean p_ageFreeCostIsRegular, RuleAppCost p_cost, long p_age) {
        super(p_app, p_cost);
        ageFreeCost = p_ageFreeCost;
        ageFreeCostIsRegular = p_ageFreeCostIsRegular;
        age = p_age;
    }

    RuleAppCost getAgeFreeCost() {
        return ageFreeCost;
    }

    protected NoPosTacletApp getTacletApp() {
        return (NoPosTacletApp) getRuleApp();
    }

    public long getAge() {
        return age;
    }

    private ImmutableList<NoPosTacletApp> incMatchAssumesFormulas(Goal p_goal) {
        final AssumesInstantiator instantiator = new AssumesInstantiator(this, p_goal);
        instantiator.findAssumesFormulaInstantiations();
        return instantiator.getResults();
    }

    protected static TacletAppContainer createContainer(NoPosTacletApp p_app,
            PosInOccurrence p_pio,
            Goal p_goal, boolean p_initial) {
        // the cost is the regular strategy cost, so it may be reused
        return createContainer(p_app, p_pio, p_goal,
            p_goal.getGoalStrategy().computeCost(p_app, p_pio, p_goal), true, p_initial);
    }

    private static TacletAppContainer createContainer(NoPosTacletApp p_app,
            PosInOccurrence p_pio,
            Goal p_goal, RuleAppCost p_ageFreeCost, boolean p_ageFreeCostIsRegular,
            boolean p_initial) {
        // This relies on the fact that the method <code>Goal.getTime()</code>
        // never returns a value less than zero
        final long localage = p_initial ? -1 : p_goal.getTime();
        final RuleAppCost cost = withAge(p_ageFreeCost, p_goal);
        if (p_pio == null) {
            return new NoFindTacletAppContainer(p_app, p_ageFreeCost, p_ageFreeCostIsRegular, cost,
                localage);
        } else {
            return new FindTacletAppContainer(p_app, p_pio, p_ageFreeCost, p_ageFreeCostIsRegular,
                cost, p_goal, localage);
        }
    }

    /**
     * Create a list of new RuleAppContainers that are to be considered for application.
     */
    @Override
    public final ImmutableList<RuleAppContainer> createFurtherApps(ProofGoal<?> p_goal) {
        var goal = (Goal) p_goal;
        if (!isStillApplicable(goal)
                || (getTacletApp().assumesInstantionsComplete()
                        && !assumesFormulasStillValid(goal))) {
            return ImmutableList.nil();
        }

        final TacletAppContainer newCont = costLocalReusedContainerOr(goal);
        if (newCont == null) {
            // a veto fired on the cost-local fast path: the re-costed base would be infinite
            return ImmutableList.nil();
        }
        if (newCont.getCost() instanceof TopRuleAppCost) {
            return ImmutableList.nil();
        }

        ImmutableList<RuleAppContainer> res =
            ImmutableList.<RuleAppContainer>singleton(newCont);

        if (getTacletApp().assumesInstantionsComplete()) {
            res = addInstances(getTacletApp(), res, goal);
        } else {
            for (NoPosTacletApp tacletApp : incMatchAssumesFormulas(goal)) {
                final NoPosTacletApp app = tacletApp;
                res = addContainer(app, res, goal);
                res = addInstances(app, res, goal);
            }
        }

        return res;
    }

    /**
     * Add all instances of the given taclet app (that are possibly produced using method
     * <code>instantiateApp</code> of the strategy) to <code>targetList</code>
     */
    private ImmutableList<RuleAppContainer> addInstances(NoPosTacletApp app,
            ImmutableList<RuleAppContainer> targetList, Goal p_goal) {
        if (app.uninstantiatedVars().size() == 0) {
            return targetList;
        }
        return instantiateApp(app, targetList, p_goal);
    }

    /**
     * Use the method <code>instantiateApp</code> of the strategy for choosing the values of schema
     * variables that have not been instantiated so far
     */
    private ImmutableList<RuleAppContainer> instantiateApp(NoPosTacletApp app,
            ImmutableList<RuleAppContainer> targetList, final Goal p_goal) {
        // just for being able to modify the result-list in an
        // anonymous class
        @SuppressWarnings("unchecked")
        final ImmutableList<RuleAppContainer>[] resA = new ImmutableList[] { targetList };

        final RuleAppCostCollector collector = (newApp, cost) -> {
            if (cost instanceof TopRuleAppCost) {
                return;
            }
            resA[0] = addContainer((NoPosTacletApp) newApp, resA[0], p_goal, cost);
        };
        p_goal.getGoalStrategy().instantiateApp(app, getPosInOccurrence(p_goal), p_goal, collector);

        return resA[0];
    }

    /**
     * Create a container object for the given taclet app, provided that the app is
     * <code>sufficientlyComplete</code>, and add the container to <code>targetList</code>
     */
    private ImmutableList<RuleAppContainer> addContainer(NoPosTacletApp app,
            ImmutableList<RuleAppContainer> targetList, Goal p_goal) {
        return targetList.prepend(
            createContainer(app, getPosInOccurrence(p_goal), p_goal, false));
    }

    /**
     * Create a container object for the given taclet app, provided that the app is
     * <code>sufficientlyComplete</code>, and add the container to <code>targetList</code>
     */
    private ImmutableList<RuleAppContainer> addContainer(NoPosTacletApp app,
            ImmutableList<RuleAppContainer> targetList, Goal p_goal, RuleAppCost cost) {
        if (!sufficientlyCompleteApp(app)) {
            return targetList;
        }
        return targetList.prepend(createContainer(app,
            getPosInOccurrence(p_goal), p_goal, cost, false, false));
    }

    private static boolean sufficientlyCompleteApp(NoPosTacletApp app) {
        final ImmutableSet<SchemaVariable> needed = app.uninstantiatedVars();
        if (needed.size() == 0) {
            return true;
        }
        for (SchemaVariable aNeeded : needed) {
            if (app.isInstantiationRequired(aNeeded)) {
                return false;
            }
        }
        return true;
    }

    private TacletAppContainer createContainer(Goal p_goal) {
        return createContainer(getTacletApp(), getPosInOccurrence(p_goal), p_goal, false);
    }

    /**
     * Re-cost the base app for {@link #createFurtherApps}. On the cost-reuse fast path (taclet
     * classified cost-local by {@link CostReuse}, numeric age-free cost) the full
     * {@link de.uka.ilkd.key.strategy.Strategy#computeCost} is skipped: the stored age-free cost is
     * carried forward verbatim and {@link RuleAppContainer#withAge} re-adds the current goal age
     * when the new container is built -- no reconstruction arithmetic, and initial containers
     * (age {@code -1}) reuse soundly too, since age is no longer part of the stored cost. The
     * {@code NonDuplicateApp}-family vetoes that contribute are re-evaluated first; if one fires
     * the
     * full cost would be {@link TopRuleAppCost}, so {@code null} is returned (drop the app).
     * Otherwise, and whenever reuse is disabled/inapplicable, falls back to the normal recompute.
     */
    private @Nullable TacletAppContainer costLocalReusedContainerOr(Goal p_goal) {
        // only a regular (computeCost) base may be carried forward; an instantiation-supplied base
        // would re-cost differently, so such containers always fall through to a normal recompute
        if (ageFreeCostIsRegular && getAgeFreeCost() instanceof NumberRuleAppCost base) {
            final CostReuse.Eligibility elig = CostReuse.eligibility(p_goal.getGoalStrategy(),
                p_goal.proof(), getTacletApp().taclet());
            // A subterm-local cost may always be carried forward; a formula-local one only while
            // the
            // find formula is unchanged (an independent sibling rewrite leaves the find subterm but
            // not the formula intact). Otherwise fall through to a normal recompute.
            if (elig != null && (!elig.weakStable() || findFormulaUnchanged(p_goal))) {
                final PosInOccurrence pos = getPosInOccurrence(p_goal);
                final MutableState mState = new MutableState();
                for (Feature veto : elig.vetoes()) {
                    if (veto.computeCost(getTacletApp(), pos, p_goal,
                        mState) instanceof TopRuleAppCost) {
                        return null;
                    }
                }
                // only for debugging or CI to raise warning flags for wrongly annotated features
                if (CostReuse.VERIFY) {
                    final RuleAppCost freshBase =
                        p_goal.getGoalStrategy().computeCost(getTacletApp(), pos, p_goal);
                    if (!base.equals(freshBase)) {
                        CostReuse.warnMismatch(getTacletApp().taclet(), base, freshBase);
                    }
                }
                // carry the age-free base forward; createContainer re-adds the current age
                return createContainer(getTacletApp(), pos, p_goal, base, true, false);
            }
        }
        return createContainer(p_goal);
    }

    /**
     * @return {@code true} iff the find formula this container was created for is still present
     *         unchanged in the goal -- the precondition for carrying forward a {@code @}
     *         {@code WeakStableCost} cost. The base container has no find formula and so
     *         conservatively answers {@code false} (such a cost is never reused for it).
     */
    protected boolean findFormulaUnchanged(Goal p_goal) {
        return false;
    }

    /**
     * Create containers for NoFindTaclets.
     */
    static RuleAppContainer createAppContainers(NoPosTacletApp p_app, Goal p_goal) {
        return createAppContainers(p_app, null, p_goal);
    }

    protected static ImmutableList<RuleAppContainer> createInitialAppContainers(
            ImmutableList<NoPosTacletApp> p_app,
            PosInOccurrence p_pio, Goal p_goal) {

        List<RuleAppCost> costs = new LinkedList<>();

        for (NoPosTacletApp app : p_app) {
            costs.add(p_goal.getGoalStrategy().computeCost(app, p_pio, p_goal));
        }

        ImmutableList<RuleAppContainer> result = ImmutableList.nil();
        for (RuleAppCost cost : costs) {
            final TacletAppContainer container =
                createContainer(p_app.head(), p_pio, p_goal, cost, true, true);
            result = result.prepend(container);
            p_app = p_app.tail();
        }
        return result;
    }

    /**
     * Create containers for FindTaclets or NoFindTaclets.
     *
     * @param p_app if <code>p_pio</code> is null, <code>p_app</code> has to be a
     *        <code>TacletApp</code> for a <code>NoFindTaclet</code>, otherwise for a
     *        <code>FindTaclet</code>.
     * @return list of containers for currently applicable TacletApps, the cost may be an instance
     *         of <code>TopRuleAppCost</code>.
     */
    public static RuleAppContainer createAppContainers(NoPosTacletApp p_app,
            PosInOccurrence p_pio,
            Goal p_goal) {
        if (!(p_pio == null ? p_app.taclet() instanceof NoFindTaclet
                : p_app.taclet() instanceof FindTaclet))
        // faster than <code>assertTrue</code>
        {
            Debug.fail("Wrong type of taclet " + p_app.taclet());
        }

        // Create an initial container for the given taclet; the if-formulas of
        // the taclet are only matched lazy (by <code>createFurtherApps()</code>
        return createContainer(p_app, p_pio, p_goal, true);
    }

    /**
     * @return true iff instantiation of the assumes-formulas of the stored taclet app exist and are
     *         valid are still valid, i.e. the referenced formulas still exist
     */
    protected boolean assumesFormulasStillValid(Goal p_goal) {
        if (getTacletApp().taclet().assumesSequent().isEmpty()) {
            return true;
        }
        if (!getTacletApp().assumesInstantionsComplete()) {
            return false;
        }

        final Iterator<AssumesFormulaInstantiation> it =
            getTacletApp().assumesFormulaInstantiations().iterator();
        final FormulaTagManager tags = p_goal.getFormulaTagManager();

        while (it.hasNext()) {
            final AssumesFormulaInstantiation assumesInstantiations2 = it.next();
            if (!(assumesInstantiations2 instanceof final AssumesFormulaInstSeq assumesInst))
            // faster than assertTrue
            {
                Debug.fail("Don't know what to do with the " + "assumes-instantiation ",
                    assumesInstantiations2);
                throw new IllegalStateException(
                    "Unexpected assume-instantiation" + assumesInstantiations2);
            } else if (tags.getTagForPos(assumesInst.toPosInOccurrence()) == null) {
                // checks whether assumesInst still occurs in sequent,
                // replaces linear lookup via Sequent#contains by
                // hashmap lookup using the tag manager
                return false;
            }
        }

        return true;
    }

    /**
     * @return true iff the stored rule app is applicable for the given sequent, i.e. if the
     *         find-position does still exist (assumes-formulas are not considered)
     */
    protected abstract boolean isStillApplicable(Goal p_goal);

    protected PosInOccurrence getPosInOccurrence(Goal p_goal) {
        return null;
    }

    /**
     * Create a <code>RuleApp</code> that is suitable to be applied or <code>null</code>.
     */
    @Override
    public TacletApp completeRuleApp(ProofGoal<?> p_goal) {
        var goal = (Goal) p_goal;
        if (!(isStillApplicable(goal) && assumesFormulasStillValid(goal))) {
            return null;
        }

        TacletApp app = getTacletApp();
        PosInOccurrence pio = getPosInOccurrence(goal);
        if (!goal.getGoalStrategy().isApprovedApp(app, pio, goal)) {
            return null;
        }

        Services services = goal.proof().getServices();
        if (pio != null) {
            app = app.setPosInOccurrence(pio, services);
            if (app == null) {
                return null;
            }
        }

        if (!app.complete()) {
            return app.tryToInstantiate(services.getOverlay(goal.getLocalNamespaces()));
        } else if (!app.isExecutable()) {
            return null;
        } else {
            return app;
        }
    }

    @Override
    public final int compareTo(RuleAppContainer o) {
        // PRIMARY key: cost. This is where proof-search order is chiefly decided; the cost carries
        // the candidate's goal-age (see AgeFeature: older and younger candidates cost differently),
        // and age in turn depends on WHEN the candidate was born, e.g. when its parked assumes-base
        // was woken (see QueueRuleApplicationManager's goal-local determinism invariant). Aging is
        // deterministic per goal, so cost is deterministic per goal.
        final int byCost = super.compareTo(o);
        if (byCost != 0) {
            return byCost;
        }
        // SECONDARY key (equal cost only): order deterministically by content so the search does
        // not
        // depend on the (timing-dependent) order in which candidates were generated/selected; a
        // source of run-to-run proof variance under concurrent goal processing. Uses only stable
        // keys (rule, operator and schema-variable names; structural positions and instantiations);
        // never object hashCodes or toString(), which can embed identity (e.g. term-label hashes).
        //
        // NOTE (do not over-read this method): the content tie-break is NECESSARY but NOT
        // SUFFICIENT
        // to explain the overall search order. It only breaks exact cost ties; the bulk of the
        // order
        // comes from cost (above), which the tie-break never sees. Cost (aging) and this tie-break
        // together keep the multi-worker search deterministic; the tie-break alone is not the
        // whole story, so a change that leaves this method intact can still reorder the proof by
        // shifting costs/ages (e.g. by changing the round in which a candidate is created).
        return compareByContent(this, o);
    }

    private static int compareByContent(RuleAppContainer ca, RuleAppContainer cb) {
        if (ca == cb) {
            return 0;
        }
        final RuleApp a = ca.getRuleApp();
        final RuleApp b = cb.getRuleApp();
        // Compare the application position first, the rule second. Equal-cost candidates used to
        // surface from the queue in generation order, which follows the sequent/term structure;
        // ordering ties by position keeps that exploration policy (and thereby proof sizes) close
        // to the historical one. Rule-name-first regressed large proofs badly (TimSort.binarySort
        // doubled): at every tie an alphabetically early rule, often a split, beat the
        // position-order winner, causing splits too early in the search.
        //
        // Taclet apps are queued as NoPosTacletApps whose posInOccurrence() is null: their
        // application position lives in the container, so it is the container position that has
        // to be compared; otherwise two apps of the same taclet at different positions (with
        // equal instantiations) tie, and their order falls back to the history-dependent heap
        // insertion order. The position also has to be compared before the rule apps are
        // shortcut-compared by identity: one and the same NoPosTacletApp object is indexed at
        // every position the matched term occurs at, so containers for different occurrences of
        // an identical subterm share their rule app.
        int c = comparePos(applicationPosition(ca), applicationPosition(cb));
        if (c != 0) {
            return c;
        }
        c = a == b ? 0 : a.rule().name().compareTo(b.rule().name());
        if (c != 0) {
            return c;
        }
        if (a == b) {
            return 0;
        }
        // Same rule and focus: distinguish by instantiations (e.g. two applyEq on different eqs).
        if (a instanceof TacletApp ta && b instanceof TacletApp tb) {
            c = compareInstantiations(ta, tb);
            if (c != 0) {
                return c;
            }
            return compareAssumesInstantiations(ta, tb);
        }
        // Known blind spots, all reached only for two equal-cost apps at the same position (and,
        // for taclets, with equal instantiation names): compareByName compares operator
        // names/arity/subterms but NOT bound variables, and a modality's program block is a
        // non-subterm child it does not walk, so two focus terms differing only in bound-variable
        // names or in an embedded program tie (they are then alpha-/program-equivalent for search
        // purposes). Built-in (non-taclet) apps are ordered here only by rule name and position, so
        // several apps of the same built-in rule at one position (e.g. multiple
        // UseOperationContract
        // apps) tie and fall back to heap insertion order. If any of these ever surfaces as SC/MT
        // node divergence, extend this with the missing content key (bound vars / program /
        // built-in-app contract).
        return 0;
    }

    /**
     * The position a container's rule app is (to be) applied at: the creation-time position of a
     * find-taclet container, or the rule app's own position for built-in rule apps.
     */
    private static @Nullable PosInOccurrence applicationPosition(RuleAppContainer c) {
        if (c instanceof FindTacletAppContainer findContainer) {
            return findContainer.getApplicationPosition();
        }
        return c.getRuleApp() == null ? null : c.getRuleApp().posInOccurrence();
    }

    /**
     * Compare the {@code \assumes} instantiations of two taclet apps. These are kept separately
     * from the schema-variable map, so two apps of the same taclet at the same focus that use
     * different assumes-formulas (e.g. {@code applyEq} instances rewriting with two different
     * equations) are still tied after {@link #compareInstantiations}; without this comparison the
     * queue order of such candidates, and thereby proof search, would depend on the
     * (history-dependent) order in which they were inserted into the queue.
     */
    private static int compareAssumesInstantiations(TacletApp a, TacletApp b) {
        final ImmutableList<AssumesFormulaInstantiation> ia = a.assumesFormulaInstantiations();
        final ImmutableList<AssumesFormulaInstantiation> ib = b.assumesFormulaInstantiations();
        if (ia == ib) {
            return 0;
        }
        if (ia == null) {
            return -1;
        }
        if (ib == null) {
            return 1;
        }
        int c = Integer.compare(ia.size(), ib.size());
        if (c != 0) {
            return c;
        }
        final var ita = ia.iterator();
        final var itb = ib.iterator();
        while (ita.hasNext()) {
            final AssumesFormulaInstantiation fa = ita.next();
            final AssumesFormulaInstantiation fb = itb.next();
            final boolean seqA = fa instanceof AssumesFormulaInstSeq;
            final boolean seqB = fb instanceof AssumesFormulaInstSeq;
            c = Boolean.compare(seqA, seqB);
            if (c != 0) {
                return c;
            }
            if (seqA) {
                c = Boolean.compare(((AssumesFormulaInstSeq) fa).inAntecedent(),
                    ((AssumesFormulaInstSeq) fb).inAntecedent());
                if (c != 0) {
                    return c;
                }
            }
            c = compareFormulasByName(fa.getSequentFormula().formula(),
                fb.getSequentFormula().formula());
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    private static int comparePos(@Nullable PosInOccurrence a, @Nullable PosInOccurrence b) {
        if (a == b) {
            return 0;
        }
        if (a == null) {
            return -1;
        }
        if (b == null) {
            return 1;
        }
        int c = Boolean.compare(a.isInAntec(), b.isInAntec());
        if (c != 0) {
            return c;
        }
        final PosInTerm pa = a.posInTerm();
        final PosInTerm pb = b.posInTerm();
        final int n = Math.min(pa.depth(), pb.depth());
        for (int i = 0; i < n; i++) {
            c = Integer.compare(pa.getIndexAt(i), pb.getIndexAt(i));
            if (c != 0) {
                return c;
            }
        }
        c = Integer.compare(pa.depth(), pb.depth());
        if (c != 0) {
            return c;
        }
        // Same path: if it is literally the same formula, no need to walk it.
        if (a.sequentFormula() == b.sequentFormula()) {
            return 0;
        }
        return compareFormulasByName(a.sequentFormula().formula(), b.sequentFormula().formula());
    }

    private static int compareInstantiations(TacletApp a, TacletApp b) {
        final var ma = a.instantiations().getInstantiationMap();
        final var mb = b.instantiations().getInstantiationMap();
        if (ma == mb) {
            return 0;
        }
        // The maps iterate in build order, which differs between code paths (fresh match,
        // re-expansion, assumes completion) even for equal content. Compare canonically: sizes,
        // then the entries sorted by schema-variable name, so that the result is a consistent
        // total order: an order-sensitive walk compares mismatched keys and turns compareTo
        // asymmetric for content-equal apps, which silently corrupts the ordering of the whole
        // rule-app queue (a leftist heap merges by pairwise comparisons).
        int c = Integer.compare(ma.size(), mb.size());
        if (c != 0) {
            return c;
        }
        final var ea = sortedByName(ma);
        final var eb = sortedByName(mb);
        for (int i = 0; i < ea.size(); i++) {
            c = ea.get(i).key().name().compareTo(eb.get(i).key().name());
            if (c != 0) {
                return c;
            }
            c = compareInstValue(ea.get(i).value().getInstantiation(),
                eb.get(i).value().getInstantiation());
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }

    /** The entries of an instantiation map, sorted by schema-variable name. */
    private static List<ImmutableMapEntry<SchemaVariable, InstantiationEntry<?>>> sortedByName(
            ImmutableMap<SchemaVariable, InstantiationEntry<?>> map) {
        final List<ImmutableMapEntry<SchemaVariable, InstantiationEntry<?>>> entries =
            new ArrayList<>(map.size());
        for (final ImmutableMapEntry<SchemaVariable, InstantiationEntry<?>> entry : map) {
            entries.add(entry);
        }
        entries.sort(Comparator.comparing(entry -> entry.key().name()));
        return entries;
    }

    private static int compareInstValue(Object va, Object vb) {
        if (va instanceof Term ta && vb instanceof Term tb) {
            return compareByName(ta, tb);
        }
        if (va == vb) {
            return 0;
        }
        return String.valueOf(va).compareTo(String.valueOf(vb));
    }

    /**
     * Order on whole sequent formulas: by the cached structural name hash first
     * ({@link Term#nameHash()}), then by the structural name walk of
     * {@link #compareByName(Term, Term)}. The hash is a pure function of the operator structure
     * (so the resulting order is stable across runs, as required for the tie-break) and is cached
     * on term instances, making it an O(1) discriminator: only hash collisions ever pay the walk,
     * which then keeps the order total and consistent.
     */
    static int compareFormulasByName(Term a, Term b) {
        if (a == b) {
            return 0;
        }
        final int c = Integer.compare(a.nameHash(), b.nameHash());
        if (c != 0) {
            return c;
        }
        return compareByName(a, b);
    }

    /** Structural order on terms by operator names only, stable across runs (unlike hashCode). */
    static int compareByName(Term a, Term b) {
        // Identical (shared) subterms compare equal without a walk. Terms are structurally shared,
        // so on large focus/instantiation terms (e.g. deep heap sequents) this skips whole
        // subtrees, the dominant cost of tie-breaking equal-cost rule apps. Order-preserving:
        // a == b implies the full comparison would yield 0.
        if (a == b) {
            return 0;
        }
        int c = a.op().name().compareTo(b.op().name());
        if (c != 0) {
            return c;
        }
        c = Integer.compare(a.arity(), b.arity());
        if (c != 0) {
            return c;
        }
        for (int i = 0; i < a.arity(); i++) {
            c = compareByName(a.sub(i), b.sub(i));
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }
}
