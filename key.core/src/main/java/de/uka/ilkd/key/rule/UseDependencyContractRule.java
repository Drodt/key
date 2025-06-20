/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.ldt.LocSetLDT;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.NullSort;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.init.ContractPO;
import de.uka.ilkd.key.proof.mgt.ComplexRuleJustificationBySpec;
import de.uka.ilkd.key.proof.mgt.RuleJustificationBySpec;
import de.uka.ilkd.key.speclang.Contract;
import de.uka.ilkd.key.speclang.DependencyContract;
import de.uka.ilkd.key.speclang.HeapContext;

import org.key_project.logic.Name;
import org.key_project.logic.PosInTerm;
import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Pair;

import org.jspecify.annotations.NonNull;

import static de.uka.ilkd.key.logic.equality.IrrelevantTermLabelsProperty.IRRELEVANT_TERM_LABELS_PROPERTY;


public final class UseDependencyContractRule implements BuiltInRule {

    public static final UseDependencyContractRule INSTANCE = new UseDependencyContractRule();

    private static final Name NAME = new Name("Use Dependency Contract");


    // -------------------------------------------------------------------------
    // constructors
    // -------------------------------------------------------------------------

    private UseDependencyContractRule() {
    }


    // -------------------------------------------------------------------------
    // internal methods
    // -------------------------------------------------------------------------

    private static List<JTerm> getEqualityDefs(JTerm term, Sequent seq) {
        final List<JTerm> result = new LinkedList<>();
        for (SequentFormula cf : seq.antecedent()) {
            final JTerm formula = (JTerm) cf.formula();
            if (formula.op() instanceof Equality && formula.sub(1).equals(term)) {
                result.add(formula.sub(0));
            }
        }
        return result;
    }


    private static List<Pair<JTerm, PosInOccurrence>> getEqualityDefsAndPos(
            Term term, Sequent seq) {
        final List<Pair<JTerm, PosInOccurrence>> result =
            new LinkedList<>();
        for (SequentFormula cf : seq.antecedent()) {
            final JTerm formula = (JTerm) cf.formula();
            if (formula.op() instanceof Equality && formula.sub(1).equals(term)) {
                final PosInOccurrence pos =
                    new PosInOccurrence(cf, PosInTerm.getTopLevel(), true);
                result.add(new Pair<>(formula.sub(0), pos));
            }
        }
        return result;
    }


    private ImmutableSet<JTerm> addEqualDefs(ImmutableSet<JTerm> terms, Goal g) {
        ImmutableList<JTerm> result = ImmutableSLList.nil();

        for (SequentFormula cf : g.sequent().antecedent()) {
            final JTerm formula = (JTerm) cf.formula();
            if (formula.op() instanceof Equality && terms.contains(formula.sub(1))) {
                result = result.prepend(formula.sub(0));
            }
        }
        return terms.union(DefaultImmutableSet.fromImmutableList(result));
    }


    private boolean hasRawSteps(JTerm heapTerm, Sequent seq, Services services) {
        final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
        final Operator op = heapTerm.op();
        assert heapTerm.sort().equals(heapLDT.targetSort());
        if (op == heapLDT.getStore() || op == heapLDT.getCreate() || op == heapLDT.getAnon()
                || op == heapLDT.getMemset()) {
            return true;
        } else if (op.arity() == 0) {
            final List<JTerm> defs = getEqualityDefs(heapTerm, seq);
            for (JTerm def : defs) {
                if (hasRawSteps(def, seq, services)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }


    private static void getRawSteps(JTerm heapTerm, Sequent seq, Services services,
            List<JTerm> result) {
        final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
        final Operator op = heapTerm.op();
        assert heapTerm.sort().equals(heapLDT.targetSort());
        if (op == heapLDT.getStore() || op == heapLDT.getCreate() || op == heapLDT.getAnon()
                || op == heapLDT.getMemset()) {
            final JTerm h = heapTerm.sub(0);
            result.add(h);
            getRawSteps(h, seq, services, result);
        } else if (op.arity() == 0) {
            final List<JTerm> defs = getEqualityDefs(heapTerm, seq);
            for (JTerm def : defs) {
                getRawSteps(def, seq, services, result);
            }
        }
    }


    private static PosInOccurrence getFreshLocsStep(
            PosInOccurrence heapPos, Sequent seq,
            Services services) {
        final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
        final LocSetLDT locSetLDT = services.getTypeConverter().getLocSetLDT();
        final var heapTerm = heapPos.subTerm();
        final var op = heapTerm.op();
        assert heapTerm.sort().equals(heapLDT.targetSort());
        if (heapTerm.op() == heapLDT.getAnon()
                && heapTerm.sub(1).op().equals(locSetLDT.getEmpty())) {
            return heapPos;
        } else if (op.arity() == 0) {
            final List<Pair<JTerm, PosInOccurrence>> defs =
                getEqualityDefsAndPos(heapTerm, seq);
            for (Pair<JTerm, PosInOccurrence> def : defs) {
                final PosInOccurrence defHeapPos = def.second.down(0);
                assert defHeapPos.subTerm().equals(def.first);
                final PosInOccurrence pos =
                    getFreshLocsStep(defHeapPos, seq, services);
                if (pos != null) {
                    return pos;
                }
            }
            return null;
        } else {
            return null;
        }
    }


    private static Pair<JTerm, ImmutableList<PosInOccurrence>> getChangedLocsForStep(
            JTerm heapTerm,
            JTerm stepHeap, Sequent seq, Services services) {
        final HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
        final Operator op = heapTerm.op();
        assert heapTerm.sort().equals(heapLDT.targetSort());
        final TermBuilder TB = services.getTermBuilder();
        if (heapTerm.equals(stepHeap)) {
            return new Pair<>(TB.empty(),
                ImmutableSLList.nil());
        } else if (op == heapLDT.getStore()) {
            final JTerm h = heapTerm.sub(0);
            final JTerm o = heapTerm.sub(1);
            final JTerm f = heapTerm.sub(2);
            final JTerm locs = TB.singleton(o, f);
            final Pair<JTerm, ImmutableList<PosInOccurrence>> furtherLocs =
                getChangedLocsForStep(h, stepHeap, seq, services);
            return new Pair<>(TB.union(locs, furtherLocs.first),
                furtherLocs.second);
        } else if (op == heapLDT.getCreate()) {
            final JTerm h = heapTerm.sub(0);
            final Pair<JTerm, ImmutableList<PosInOccurrence>> furtherLocs =
                getChangedLocsForStep(h, stepHeap, seq, services);
            return furtherLocs;
        } else if (op == heapLDT.getAnon() || op == heapLDT.getMemset()) {
            final JTerm h = heapTerm.sub(0);
            final JTerm s = heapTerm.sub(1);
            final Pair<JTerm, ImmutableList<PosInOccurrence>> furtherLocs =
                getChangedLocsForStep(h, stepHeap, seq, services);
            return new Pair<>(TB.union(s, furtherLocs.first),
                furtherLocs.second);
        } else if (op.arity() == 0) {
            final List<Pair<JTerm, PosInOccurrence>> defs =
                getEqualityDefsAndPos(heapTerm, seq);
            for (Pair<JTerm, PosInOccurrence> def : defs) {
                final Pair<JTerm, ImmutableList<PosInOccurrence>> furtherLocs =
                    getChangedLocsForStep(def.first, stepHeap, seq, services);
                if (furtherLocs != null) {
                    return new Pair<>(furtherLocs.first,
                        furtherLocs.second.prepend(def.second));
                }
            }
        }
        return null;
    }


    public static boolean isBaseOcc(JTerm focus, JTerm candidate) {
        if (!candidate.op().equals(focus.op())) {
            return false;
        }
        for (int i = 1, n = candidate.arity(); i < n; i++) {
            if (!(candidate.sub(i).equalsModProperty(focus.sub(i), IRRELEVANT_TERM_LABELS_PROPERTY)
                    || candidate.sub(i).op() instanceof LogicVariable)) {
                return false;
            }
        }
        return true;
    }


    private static void collectBaseOccsHelper(JTerm focus,
            PosInOccurrence pos,
            Map<JTerm, PosInOccurrence> result) {
        final JTerm candidate = (JTerm) pos.subTerm();
        if (isBaseOcc(focus, candidate)) {
            result.put(candidate.sub(0), pos);
        }
        for (int i = 0, n = candidate.arity(); i < n; i++) {
            collectBaseOccsHelper(focus, pos.down(i), result);
        }
    }


    private static Map<JTerm, PosInOccurrence> collectBaseOccs(
            JTerm focus, Sequent seq) {
        assert focus.op() instanceof IObserverFunction;
        final Map<JTerm, PosInOccurrence> result =
            new LinkedHashMap<>();
        for (SequentFormula cf : seq.antecedent()) {
            final PosInOccurrence pos =
                new PosInOccurrence(cf, PosInTerm.getTopLevel(), true);
            collectBaseOccsHelper(focus, pos, result);
        }
        for (SequentFormula cf : seq.succedent()) {
            final PosInOccurrence pos =
                new PosInOccurrence(cf, PosInTerm.getTopLevel(), false);
            collectBaseOccsHelper(focus, pos, result);
        }
        return result;
    }


    public static List<PosInOccurrence> getSteps(
            List<LocationVariable> heapContext,
            PosInOccurrence pos, Sequent seq, Services services) {
        final JTerm focus = (JTerm) pos.subTerm();
        assert focus.op() instanceof IObserverFunction;

        final List<PosInOccurrence> result = new LinkedList<>();

        // special treatment for anon(h, empty, h')
        final PosInOccurrence freshLocsStep =
            getFreshLocsStep(pos.down(0), seq, services);
        if (freshLocsStep != null) {
            result.add(freshLocsStep);
            return result;
        }

        // get raw steps
        final List<JTerm> rawSteps = new LinkedList<>();
        int index = 0;
        final int stateCount = ((IObserverFunction) focus.op()).getStateCount();
        final int numHeaps = ((IObserverFunction) focus.op()).getHeapCount(services);
        while (index < stateCount * numHeaps) {
            getRawSteps(focus.sub(index++), seq, services, rawSteps);
            if (((IObserverFunction) focus.op()).getStateCount() == 2) {
                getRawSteps(focus.sub(index++), seq, services, rawSteps);
            }
            if (rawSteps.size() > 0) {
                // get base occs
                final Map<JTerm, PosInOccurrence> baseOccs =
                    collectBaseOccs(focus, seq);

                // filter steps
                for (JTerm rawStep : rawSteps) {
                    final PosInOccurrence step =
                        baseOccs.get(rawStep);
                    if (step != null) {
                        result.add(step);
                    }
                }
            }
        }

        return result;
    }


    public static PosInOccurrence findStepInIfInsts(
            List<PosInOccurrence> steps,
            UseDependencyContractApp app, TermServices services) {
        for (PosInOccurrence pio : app.assumesInsts()) {
            if (steps.contains(pio)) {
                return pio;
            }
        }
        return null;
    }


    /**
     * Returns the dependency contracts which are applicable for the passed target.
     */
    public static ImmutableSet<Contract> getApplicableContracts(Services services, KeYJavaType kjt,
            IObserverFunction target) {
        ImmutableSet<Contract> result =
            services.getSpecificationRepository().getContracts(kjt, target);
        for (Contract contract : result) {
            if (!(contract instanceof DependencyContract)) {
                result = result.remove(contract);
            }
        }
        return result;
    }


    // -------------------------------------------------------------------------
    // public interface
    // -------------------------------------------------------------------------

    @Override
    public boolean isApplicable(Goal goal, PosInOccurrence pio) {
        if (pio == null) {
            return false;
        }

        // top level symbol must be observer
        final JTerm focus = (JTerm) pio.subTerm();
        if (!(focus.op() instanceof IObserverFunction target)) {
            return false;
        }

        // there must not be free variables in the focus term
        if (!focus.freeVars().isEmpty()) {
            return false;
        }

        // abort if inside of transformer
        if (Transformer.inTransformer(pio)) {
            return false;
        }

        // heap term of observer must be store-term (or anon, create,
        // memset, ...)
        final Services services = goal.proof().getServices();
        // final List<LocationVariable> heaps = HeapContext.getModifiableHeaps(services, false);
        boolean hasRawSteps = false;
        for (int i = 0; i < target.getHeapCount(services) * target.getStateCount(); i++) {
            if (hasRawSteps(focus.sub(i), goal.sequent(), services)) {
                hasRawSteps = true;
                break;
            }
        }
        if (!hasRawSteps) {
            return false;
        }
        // there must be contracts for the observer
        final KeYJavaType kjt = target.isStatic() ? target.getContainerType()
                : services.getJavaInfo().getKeYJavaType(
                    focus.sub(target.getHeapCount(services) * target.getStateCount()).sort());
        assert kjt != null : "could not determine receiver type for " + focus;
        if (kjt.getSort() instanceof NullSort) {
            return false;
        }
        final ImmutableSet<Contract> contracts = getApplicableContracts(services, kjt, target);
        if (contracts.isEmpty()) {
            return false;
        }

        // applying a contract here must not create circular dependencies
        // between proofs
        return goal.proof().mgt().isContractApplicable(contracts.iterator().next());
    }

    @Override
    public @NonNull ImmutableList<Goal> apply(Goal goal, RuleApp ruleApp) {
        final var services = goal.getOverlayServices();
        // collect information
        final LocSetLDT locSetLDT = services.getTypeConverter().getLocSetLDT();
        final PosInOccurrence pio = ruleApp.posInOccurrence();
        final JTerm focus = (JTerm) pio.subTerm();
        final IObserverFunction target = (IObserverFunction) focus.op();
        final List<LocationVariable> heaps = HeapContext.getModifiableHeaps(services, false);
        final TermBuilder TB = services.getTermBuilder();

        final JTerm selfTerm;
        if (target.isStatic()) {
            selfTerm = null;
        } else {
            selfTerm = focus.sub(target.getHeapCount(services) * target.getStateCount());
        }

        ImmutableList<JTerm> paramTerms = ImmutableSLList.nil();
        for (int i = target.getHeapCount(services) * target.getStateCount()
                + (target.isStatic() ? 0 : 1); i < focus.arity(); i++) {
            paramTerms = paramTerms.append(focus.sub(i));
        }

        // configure contract
        DependencyContract contract =
            (DependencyContract) ((UseDependencyContractApp) ruleApp).getInstantiation();

        assert contract != null;

        // get step
        final PosInOccurrence step =
            ((UseDependencyContractApp) ruleApp).step();

        final boolean twoState = target.getStateCount() == 2;
        final int obsHeapCount = target.getHeapCount(services);
        Map<LocationVariable, JTerm> atPres =
            twoState ? new LinkedHashMap<>() : null;
        Map<LocationVariable, JTerm> heapTerms = new LinkedHashMap<>();
        int i = 0;
        for (LocationVariable heap : heaps) {
            if (i >= obsHeapCount) {
                break;
            }
            heapTerms.put(heap, (JTerm) step.subTerm().sub(2 * i));
            if (twoState) {
                atPres.put(heap, (JTerm) step.subTerm().sub(2 * i + 1));
            }
            i++;
        }
        final JTerm mby =
            contract.hasMby() ? contract.getMby(heapTerms, selfTerm, paramTerms, atPres, services)
                    : null;

        assert !step.subTerm().equals(focus);

        JTerm freePre = !target.isStatic() ? TB.not(TB.equals(selfTerm, TB.NULL())) : null;
        JTerm disjoint = null;
        JTerm pre = null;
        final JTerm[] subs = focus.subs().toArray(new JTerm[focus.arity()]);
        int heapExprIndex = 0;
        boolean useful = false;
        ImmutableList<PosInOccurrence> ifInsts =
            ImmutableSLList.nil();
        int hc = 0;
        for (LocationVariable heap : heaps) {
            if (hc >= obsHeapCount) {
                break;
            }
            for (boolean atPre : twoState ? new boolean[] { false, true }
                    : new boolean[] { false }) {
                // get changed locs and used equalities
                final JTerm subStep = (JTerm) step.subTerm().sub(heapExprIndex);
                final Pair<JTerm, ImmutableList<PosInOccurrence>> changedLocs =
                    getChangedLocsForStep(focus.sub(heapExprIndex), subStep, goal.sequent(),
                        services);

                assert changedLocs != null;
                // store insts
                ifInsts = ifInsts.append(changedLocs.second.prepend(step));
                if (!target.isStatic()) {
                    final JTerm cr = TB.created(subStep, selfTerm);
                    if (freePre == null) {
                        freePre = cr;
                    } else {
                        freePre = TB.and(freePre, cr);
                    }
                }
                final JTerm wf =
                    TB.and(TB.wellFormed(subStep), TB.wellFormed(focus.sub(heapExprIndex)));
                if (freePre == null) {
                    freePre = wf;
                } else {
                    freePre = TB.and(freePre, wf);
                }
                i = 0;
                for (JTerm paramTerm : paramTerms) {
                    assert freePre != null;
                    freePre = TB.and(freePre,
                        TB.reachableValue(subStep, paramTerm, target.getParamType(i++)));
                }
                final JTerm dep =
                    contract.getDep(heap, atPre, subStep, selfTerm, paramTerms, atPres, services);
                final JTerm ds = TB.disjoint(changedLocs.first, dep);
                if (disjoint == null) {
                    disjoint = ds;
                } else {
                    disjoint = TB.and(disjoint, ds);
                }
                // check if helpful
                if (!useful && !changedLocs.first.op().equals(locSetLDT.getEmpty())) {
                    final ImmutableSet<JTerm> changed =
                        addEqualDefs(TB.unionToSet(changedLocs.first), goal);
                    if (!changed.contains(dep)) {
                        useful = true;
                    }
                } else {
                    useful = true;
                }
                if (!atPre) {
                    final JTerm p =
                        contract.getPre(heap, subStep, selfTerm, paramTerms, atPres, services);
                    if (p != null) {
                        if (pre == null) {
                            pre = p;
                        } else {
                            pre = TB.and(pre, p);
                        }
                    }
                    subs[heapExprIndex] = subStep;
                }
                heapExprIndex++;
            }
            hc++;
        }

        // store insts in rule app
        ruleApp = ((IBuiltInRuleApp) ruleApp).setAssumesInsts(ifInsts);

        // create justification
        final RuleJustificationBySpec just = new RuleJustificationBySpec(contract);
        final ComplexRuleJustificationBySpec cjust = (ComplexRuleJustificationBySpec) goal.proof()
                .getInitConfig().getJustifInfo().getJustification(this);
        cjust.add(ruleApp, just);

        if (!useful) {
            return goal.split(1);
        }

        // prepare cut formula
        final ContractPO po =
            services.getSpecificationRepository().getContractPOForProof(goal.proof());
        final JTerm mbyOk;
        if (po != null && /* po.getMbyAtPre() != null && */ mby != null) {
            // mbyOk = TB.and(TB.leq(TB.zero(services), mby, services),
            // TB.lt(mby, po.getMbyAtPre(), services));
            // mbyOk = TB.prec(mby, po.getMbyAtPre(), services);
            mbyOk = TB.measuredByCheck(mby);
        } else {
            mbyOk = TB.tt();
        }
        final JTerm cutFormula = TB.and(freePre, pre, disjoint, mbyOk);


        // create "Post" branch
        final ImmutableList<Goal> result = goal.split(1);
        final JTerm termWithBaseHeap = TB.func(target, subs);
        final JTerm implication = TB.imp(cutFormula, TB.equals(focus, termWithBaseHeap));
        result.head().addFormula(new SequentFormula(implication), true, false);

        return result;
    }


    @Override
    public Name name() {
        return NAME;
    }


    @Override
    public String displayName() {
        return NAME.toString();
    }


    @Override
    public String toString() {
        return displayName();
    }

    public UseDependencyContractApp createApp(PosInOccurrence pos) {
        return createApp(pos, null);
    }

    @Override
    public UseDependencyContractApp createApp(PosInOccurrence pos, TermServices services) {
        return new UseDependencyContractApp(this, pos);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isApplicableOnSubTerms() {
        return true;
    }
}
