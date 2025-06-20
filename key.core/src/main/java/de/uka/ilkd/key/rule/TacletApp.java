/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

import de.uka.ilkd.key.java.*;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.java.reference.TypeReference;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.ClashFreeSubst.VariableCollectVisitor;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.logic.sort.ProgramSVSort;
import de.uka.ilkd.key.proof.VariableNameProposer;
import de.uka.ilkd.key.rule.inst.GenericSortCondition;
import de.uka.ilkd.key.rule.inst.GenericSortException;
import de.uka.ilkd.key.rule.inst.SVInstantiations.UpdateLabelPair;
import de.uka.ilkd.key.util.Debug;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.Namespace;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.op.sv.SchemaVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.rules.instantiation.*;
import org.key_project.prover.rules.instantiation.IllegalInstantiationException;
import org.key_project.prover.rules.instantiation.MatchConditions;
import org.key_project.prover.sequent.*;
import org.key_project.util.collection.*;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * A TacletApp object contains information required for a concrete application. These information
 * may consist of
 * <ul>
 * <li>instantiations of the schemavariables</li>
 * <li>position of the find term</li>
 * </ul>
 * Its methods <code>complete</code> or <code>sufficientlyComplete</code> are used to determine if
 * the information is complete or at least sufficient (can be completed using meta variables)
 * complete, so that is can be applied.
 */
public abstract class TacletApp implements RuleApp {
    public static final AtomicLong PERF_EXECUTE = new AtomicLong();
    public static final AtomicLong PERF_SET_SEQUENT = new AtomicLong();
    public static final AtomicLong PERF_PRE = new AtomicLong();

    /** the taclet for which the application information is collected */
    private final /* @NonNull */ org.key_project.prover.rules.Taclet taclet;

    /**
     * contains the instantiations of the schema variables of the Taclet
     */
    protected final de.uka.ilkd.key.rule.inst.SVInstantiations instantiations;

    /**
     * caches a created match condition {@code (instantiations, RenameTable.EMPTY)}
     */
    private final de.uka.ilkd.key.rule.MatchConditions matchConditions;

    /**
     * chosen instantiations for the assumes-sequent formulas
     */
    protected final ImmutableList<AssumesFormulaInstantiation> ifInstantiations;

    /**
     * set of schema variables that appear in the Taclet and need to be instantiated but are not
     * instantiated yet. This means SchemaVariables in addrule-sections have to be ignored
     */
    private volatile ImmutableSet<SchemaVariable> missingVars = null;

    /**
     * the update context given by the current instantiations must not be changed
     */
    protected boolean updateContextFixed = false;

    /**
     * constructs a TacletApp for the given taclet, with an empty instantiation map
     */
    TacletApp(org.key_project.prover.rules.Taclet taclet) {
        this(taclet, de.uka.ilkd.key.rule.inst.SVInstantiations.EMPTY_SVINSTANTIATIONS, null);
    }

    TacletApp(org.key_project.prover.rules.Taclet taclet, SVInstantiations instantiations,
            ImmutableList<AssumesFormulaInstantiation> ifInstantiations) {
        this.taclet = taclet;
        this.instantiations = (de.uka.ilkd.key.rule.inst.SVInstantiations) instantiations;
        this.ifInstantiations = ifInstantiations;
        this.matchConditions =
            new de.uka.ilkd.key.rule.MatchConditions(this.instantiations, RenameTable.EMPTY_TABLE);
    }

    /**
     * collects all bound variables above the occurrence of the schemavariable whose prefix is given
     *
     * @param prefix the TacletPrefix of the schemavariable
     * @param instantiations the SVInstantiations so that the find(if)-expression matches
     * @return set of the bound variables
     */
    protected static ImmutableSet<QuantifiableVariable> boundAtOccurrenceSet(TacletPrefix prefix,
            SVInstantiations instantiations) {
        return collectPrefixInstantiations(prefix, instantiations);
    }

    /**
     * collects all bound variables above the occurrence of the schemavariable whose prefix is given
     *
     * @param prefix the TacletPrefix of the schemavariable
     * @param instantiations the SVInstantiations so that the find(if)-expression matches
     * @param pos the posInOccurrence describing the position of the schemavariable
     * @return set of the bound variables
     */
    protected static ImmutableSet<QuantifiableVariable> boundAtOccurrenceSet(TacletPrefix prefix,
            SVInstantiations instantiations, PosInOccurrence pos) {

        ImmutableSet<QuantifiableVariable> result = boundAtOccurrenceSet(prefix, instantiations);

        if (prefix.context()) {
            result = result.union(collectBoundVarsAbove(pos));
        }

        return result;
    }

    /**
     * collects all those logic variable that are instances of the variableSV of the given prefix
     *
     * @param pre the TacletPrefix of a SchemaVariable that is bound
     * @param instantiations the SVInstantiations to look at
     * @return the set of the logic variables whose elements are the instantiations of a bound
     *         SchemaVariable appearing in the TacletPrefix
     */
    private static ImmutableSet<QuantifiableVariable> collectPrefixInstantiations(TacletPrefix pre,
            SVInstantiations instantiations) {

        ImmutableSet<QuantifiableVariable> instanceSet = DefaultImmutableSet.nil();

        for (final var prefixSchemaVar : pre.prefix()) {
            instanceSet = instanceSet.add(
                (LogicVariable) ((JTerm) instantiations.getInstantiation(prefixSchemaVar)).op());
        }
        return instanceSet;
    }

    /**
     * returns the taclet the application information is collected for
     *
     * @return the Taclet the application information is collected for
     */
    public Taclet taclet() {
        return (Taclet) taclet;
    }

    /**
     * returns the rule the application information is collected for
     *
     * @return the Rule the application information is collected for
     */
    @Override
    public Taclet rule() {
        return (Taclet) taclet;
    }

    /**
     * returns the instantiations for the application of the Taclet at the specified position.
     *
     * @return the SVInstantiations needed to instantiate the Taclet
     */
    public de.uka.ilkd.key.rule.inst.SVInstantiations instantiations() {
        return instantiations;
    }

    public de.uka.ilkd.key.rule.MatchConditions matchConditions() {
        return matchConditions;
    }

    public ImmutableList<AssumesFormulaInstantiation> assumesFormulaInstantiations() {
        return ifInstantiations;
    }

    public boolean isUpdateContextFixed() {
        return updateContextFixed;
    }

    /**
     * resolves collisions between bound SchemaVariables in an SVInstantiation
     *
     * @param insts the original SVInstantiations
     * @return the resolved SVInstantiations
     */
    protected static SVInstantiations resolveCollisionVarSV(
            org.key_project.prover.rules.Taclet taclet, SVInstantiations insts,
            Services services) {

        HashMap<LogicVariable, SchemaVariable> collMap = new LinkedHashMap<>();

        for (var pair : insts.getInstantiationMap()) {
            if (pair.key() instanceof VariableSV varSV) {
                JTerm value = (JTerm) pair.value().getInstantiation();
                if (!collMap.containsKey(value.op())) {
                    collMap.put((LogicVariable) value.op(), varSV);
                } else {
                    insts = replaceInstantiation(taclet, insts, varSV, services);
                }
            }
        }
        return insts;
    }

    /**
     * delivers the term below the (unique) quantifier of a bound SchemaVariable in the given term.
     *
     * @param varSV the searched Bound Schemavariable
     * @param term the term to be searched in
     * @return the term below the given quantifier in the given term
     */
    private static JTerm getTermBelowQuantifier(SchemaVariable varSV, JTerm term) {
        for (int i = 0; i < term.arity(); i++) {
            for (int j = 0; j < term.varsBoundHere(i).size(); j++) {
                if (term.varsBoundHere(i).get(j) == varSV) {
                    return term.sub(i);
                }
            }
            var rec = getTermBelowQuantifier(varSV, term.sub(i));
            if (rec != null) {
                return rec;
            }
        }
        return null;
    }

    /**
     * delivers the term below the (unique) quantifier of a bound SchemaVariable varSV in the find
     * and if-parts of the Taclet
     *
     * @param varSV the searched bound SchemaVariable
     * @return the term below the given quantifier in the find and if-parts of the Taclet
     */
    private static JTerm getTermBelowQuantifier(org.key_project.prover.rules.Taclet taclet,
            SchemaVariable varSV) {
        for (SequentFormula sequentFormula : taclet.assumesSequent()) {
            JTerm result = getTermBelowQuantifier(varSV, (JTerm) sequentFormula.formula());
            if (result != null) {
                return result;
            }
        }

        if (taclet instanceof FindTaclet findTaclet) {
            return getTermBelowQuantifier(varSV, findTaclet.find());
        } else {
            return null;
        }
    }

    /**
     * returns true iff the instantiation of a bound SchemaVariable contains the given Logicvariable
     *
     * @param boundVars ImmutableArray<QuantifiableVariable> with the bound SchemaVariables
     * @param x the LogicVariable that is looked for
     * @param insts the SVInstantiations where to get the necessary instantiations of the bound
     *        SchemaVariables
     * @return true iff the instantiation of a Bound Schemavariable contains the given Logicvariable
     */
    private static boolean contains(ImmutableArray<QuantifiableVariable> boundVars,
            LogicVariable x,
            SVInstantiations insts) {
        for (int i = 0; i < boundVars.size(); i++) {
            JTerm instance = (JTerm) insts.getInstantiation((SchemaVariable) boundVars.get(i));
            if (instance.op() == x) {
                return true;
            }
        }

        return false;
    }

    /**
     * returns a new SVInstantiations that modifies the given SVInstantiations insts at the bound
     * SchemaVariable varSV to a new LogicVariable.
     */
    protected static SVInstantiations replaceInstantiation(
            org.key_project.prover.rules.Taclet taclet, SVInstantiations insts,
            SchemaVariable varSV,
            Services services) {
        JTerm term = getTermBelowQuantifier(taclet, varSV);
        LogicVariable newVariable = new LogicVariable(
            new Name(((JTerm) insts.getInstantiation(varSV)).op().name() + "0"),
            ((JTerm) insts.getInstantiation(varSV)).sort());
        // __CHANGE__ How to name the new variable? TODO
        JTerm newVariableTerm = services.getTermBuilder().var(newVariable);
        return replaceInstantiation(insts, term, varSV, newVariableTerm, services);
    }

    /**
     * returns a new SVInstantiations that modifies the given SVInstantiations insts at the bound
     * SchemaVariable u to the Term (that is a LogicVariable) y.
     */
    private static de.uka.ilkd.key.rule.inst.SVInstantiations replaceInstantiation(
            SVInstantiations insts, JTerm t, SchemaVariable u, JTerm y, Services services) {

        var result = (de.uka.ilkd.key.rule.inst.SVInstantiations) insts;
        LogicVariable x = (LogicVariable) ((JTerm) insts.getInstantiation(u)).op();
        if (t.op() instanceof SchemaVariable sv) {
            if (!(t.op() instanceof VariableSV)) {
                ClashFreeSubst cfSubst = new ClashFreeSubst(x, y, services.getTermBuilder());
                result =
                    result.replace(sv, cfSubst.apply((JTerm) insts.getInstantiation(sv)), services);
            }
        } else {
            for (int i = 0; i < t.arity(); i++) {
                if (!contains(t.varsBoundHere(i), x, insts)) {
                    result = replaceInstantiation(result, t.sub(i), u, y, services);
                }
            }

        }
        result = result.replace(u, y, services);
        return result;
    }

    /**
     * asserts that the rule application is complete and executable
     *
     */
    @Override
    public void checkApplicability() {
        if (!complete()) {
            throw new IllegalStateException(
                "Tried to apply rule \n" + taclet + "\nthat is not complete." + this);
        }

        if (!isExecutable()) {
            throw new RuntimeException(
                "taclet application with unsatisfied 'checkPrefix': " + this);
        }
    }

    /*
     * checks if application conditions are satisfied and returns <code>true</code> if this is the
     * case
     */
    public boolean isExecutable() {
        // bugfix #1336, see bugtracker
        if (taclet instanceof RewriteTaclet rwTaclet) {
            ImmutableList<UpdateLabelPair> oldUpdCtx =
                matchConditions().getInstantiations().getUpdateContext();
            var newConditions = rwTaclet.checkPrefix(posInOccurrence(),
                de.uka.ilkd.key.rule.MatchConditions.EMPTY_MATCHCONDITIONS);
            if (newConditions == null) {
                return false;
            }
            final ImmutableList<UpdateLabelPair> newUpdCtx =
                newConditions.getInstantiations().getUpdateContext();
            return oldUpdCtx.equals(newUpdCtx);
        }
        return true;
    }

    /*
     * applies the specified rule at the specified position if all schema variables have been
     * instantiated and creates a new goal if the if-sequent does not match. The new goal proves
     * that the assumes-sequent can be derived.
     *
     * @param goal the Goal where to apply the rule
     *
     * @param services the Services encapsulating all java information
     *
     * @return list of new created goals
     */
    /*
     * public ImmutableList<Goal> executeForceIf(Goal goal, Services services) { if (!complete()) {
     * throw new IllegalStateException("Applied rule not complete."); }
     * goal.addAppliedRuleApp(this); return taclet().apply(goal, services, this, true); }
     */

    /**
     * calculate needed SchemaVariables that have not been instantiated yet. This means to ignore
     * SchemaVariables that appear only in addrule-sections of Taclets
     *
     * @return ImmutableSet<SchemaVariable> that need to be instantiated but are not
     */
    protected ImmutableSet<SchemaVariable> calculateNonInstantiatedSV() {
        if (missingVars == null) {
            ImmutableSet<SchemaVariable> localMissingVars =
                DefaultImmutableSet.nil();
            TacletSchemaVariableCollector coll =
                new TacletSchemaVariableCollector(instantiations());
            coll.visitWithoutAddrule(taclet());
            Iterator<SchemaVariable> it = coll.varIterator();
            while (it.hasNext()) {
                SchemaVariable var = it.next();
                if (!instantiations().isInstantiated(var)) {
                    localMissingVars = localMissingVars.add(var);
                }
            }
            missingVars = localMissingVars;
        }

        return missingVars;
    }


    /**
     * creates a new Tacletapp where the SchemaVariable sv is instantiated with the given term.
     * Sort equality is checked. If the check fails an IllegalArgumentException is thrown.
     *
     * @param sv the SchemaVariable to be instantiated
     * @param term the Term the SchemaVariable is instantiated with
     * @param services the services object
     * @param interesting whether instantiations for this schema variable should be kept in the list
     *        of "interesting" instantiations
     * @return the new TacletApp
     */
    public TacletApp addCheckedInstantiation(SchemaVariable sv, JTerm term, Services services,
            boolean interesting) {
        if (sv instanceof VariableSV && !(term.op() instanceof LogicVariable)) {
            throw new IllegalInstantiationException("Could not add " + "the instantiation of " + sv
                + " because " + term + " is no variable.");
        }

        final var newMC =
            taclet.getMatcher().matchSV(sv, term, matchConditions(), services);

        if (newMC == null) {
            throw new IllegalInstantiationException(
                "Instantiation " + term + " of " + sv
                    + " does not satisfy the variable conditions");
        }

        var svInst = (de.uka.ilkd.key.rule.inst.SVInstantiations) newMC.getInstantiations();
        if (interesting) {
            svInst = svInst.makeInteresting(sv, services);
        }

        return addInstantiation(svInst, services);

    }

    /**
     * returns the variables that have not yet been instantiated and need to be instantiated to
     * apply the Taclet. (These are not all SchemaVariables like the one that appear only in the
     * addrule sections)
     *
     * @return ImmutableSet<SchemaVariable> with SchemaVariables that have not been instantiated yet
     */
    public @NonNull ImmutableSet<SchemaVariable> uninstantiatedVars() {
        return calculateNonInstantiatedSV();
    }


    /**
     * returns true if the given {@link SchemaVariable} must be explicitly instantiated it does not
     * check whether sv is already instantiated or not
     *
     * @param sv the SchemaVariable
     * @return true if sv must be instantiated
     */
    public boolean isInstantiationRequired(SchemaVariable sv) {
        return !(sv instanceof SkolemTermSV || sv instanceof VariableSV);
    }

    /**
     * @return A taclet app which is more complete than the original one (although there might still
     *         be some leftover uninstantiated variables) or null if nothing changed.
     */
    public final TacletApp tryToInstantiateAsMuchAsPossible(Services services) {
        TacletApp app = instantiationHelper(false, services);
        if (app == null)
            return null;

        if (app != this) {
            final var appMC =
                app.taclet().getMatcher().checkConditions(app.matchConditions(), services);
            if (appMC == null) {
                return null;
            }
            return app;
        }

        return null;
    }

    /**
     * @return A TacletApp with this.sufficientlyComplete() or null
     */
    public final @Nullable TacletApp tryToInstantiate(Services services) {
        TacletApp app = instantiationHelper(true, services);
        if (app == null)
            return null;

        if (app != this) {
            final var appMC =
                app.taclet().getMatcher().checkConditions(app.matchConditions(), services);
            if (appMC == null) {
                return null;
            } else {
                app = app.setMatchConditions(appMC, services);
            }
        }

        if (!app.complete()) {
            return null;
        }
        return app;
    }

    private TacletApp instantiationHelper(boolean force, Services services) {
        final VariableNamer varNamer = services.getVariableNamer();
        final TermBuilder tb = services.getTermBuilder();

        TacletApp app = this;
        ImmutableList<String> proposals = ImmutableSLList.nil();

        for (final SchemaVariable variable : uninstantiatedVars()) {
            if (!(variable instanceof JOperatorSV operatorSv)) {
                continue;
            }
            if (operatorSv.arity() != 0) {
                continue;
            }

            if (operatorSv.sort() == ProgramSVSort.VARIABLE) {
                String proposal =
                    varNamer.getSuggestiveNameProposalForProgramVariable(operatorSv, this,
                        services, proposals);
                ProgramElement pe =
                    app.getProgramElement(proposal, (ProgramSV) operatorSv, services);
                app = app.addCheckedInstantiation(operatorSv, pe, services, true);
                proposals = proposals.append(proposal);
            } else if (operatorSv.sort() == ProgramSVSort.LABEL) {
                boolean nameclash;
                do {
                    String proposal =
                        VariableNameProposer.DEFAULT.getProposal(this, operatorSv, services,
                            null, proposals);
                    ProgramElement pe =
                        app.getProgramElement(proposal, (ProgramSV) operatorSv, services);
                    proposals = proposals.prepend(proposal);
                    try {
                        app = app.addCheckedInstantiation(operatorSv, pe, services, true);
                    } catch (IllegalInstantiationException iie) {
                        // name clash
                        nameclash = true;
                    }
                    // FIXME: This loop is never executed more than once
                    // The following line might belong to the try-block.
                    // Leave it untouched, however, since no problems
                    // reported with this established code. MU 10-2012
                    nameclash = false;
                } while (nameclash);
            } else if (operatorSv instanceof SkolemTermSV) {
                // if the sort of the schema variable is generic,
                // ensure that it is instantiated
                app = forceGenericSortInstantiation(app, operatorSv, services);
                if (app == null) {
                    return null;
                }

                String proposal =
                    VariableNameProposer.DEFAULT.getProposal(app, operatorSv, services, null,
                        proposals);

                proposals = proposals.append(proposal);

                app = app.createSkolemConstant(proposal, operatorSv, true, services);

            } else if (operatorSv instanceof VariableSV) {
                // if the sort of the schema variable is generic,
                // ensure that it is instantiated
                app = forceGenericSortInstantiation(app, operatorSv, services);
                if (app == null) {
                    return null;
                }

                String proposal;
                Collection<String> conflictNames = collectClashNames(operatorSv, services);
                do {
                    proposal =
                        VariableNameProposer.DEFAULT.getProposal(this, operatorSv, services, null,
                            proposals);
                    proposals = proposals.prepend(proposal);
                } while (conflictNames.contains(proposal));

                LogicVariable v =
                    new LogicVariable(new Name(proposal), getRealSort(operatorSv, services));

                app = app.addCheckedInstantiation(operatorSv, tb.var(v), services, true);
            } else {
                if (force) {
                    return null;
                }
            }
        }
        return app;
    }

    /**
     * <p>
     * Collect names which would result in a clash for a new logical variable.
     * </p>
     * <p>
     * The clashing names include the names of the bound and unbound variables of "notFreeIn"
     * clauses. Additionally, equally-named program variables are avoided.
     * </p>
     * <p>
     * While this analysis is not strictly necessary (two different variables may bear the same
     * name), it is vital not to cause confusion with the user.
     * </p>
     *
     * @param sv the schema variable to instantiate with a fresh variable, not <code>null</code>
     * @param services the services object, not <code>null</code>
     * @return a fresh created collection of strings in which a freshly created variable name should
     *         not fall.
     */
    private Collection<String> collectClashNames(SchemaVariable sv, TermServices services) {
        Collection<String> result = new LinkedHashSet<>();
        VariableCollectVisitor vcv = new VariableCollectVisitor();
        for (final var nv : taclet().varsNotFreeIn()) {
            if (nv.first() == sv) {
                JTerm term = (JTerm) instantiations.getInstantiation(nv.second());
                if (term != null) {
                    term.execPostOrder(vcv);
                }
            }
        }

        for (QuantifiableVariable var : vcv.vars()) {
            result.add(var.name().toString());
        }

        for (Named progvar : services.getNamespaces().programVariables().allElements()) {
            result.add(progvar.name().toString());
        }

        return result;
    }

    /**
     * If the sort of the given schema variable is generic, add a condition to the instantiations of
     * the given taclet app that requires the sort to be instantiated
     *
     * @return the new taclet app, or <code>null</code> if the sort of <code>sv</code> is generic
     *         and cannot be instantiated (at least at the time)
     */
    private static TacletApp forceGenericSortInstantiation(TacletApp app, JOperatorSV sv,
            Services services) {
        final GenericSortCondition c = GenericSortCondition.forceInstantiation(sv.sort(), false);
        if (c != null) {
            try {
                app = app.setInstantiation(app.instantiations().add(c, services), services);
            } catch (GenericSortException e) {
                return null;
            }
        }
        return app;
    }

    /**
     * @param services the Services class allowing access to the type model
     * @return p_s iff p_s is not a generic sort, the concrete sort p_s is instantiated with
     *         currently otherwise
     * @throws GenericSortException iff p_s is a generic sort which is not yet instantiated
     */
    public Sort getRealSort(JOperatorSV p_sv, TermServices services) {
        return instantiations().getGenericSortInstantiations().getRealSort(p_sv, services);
    }

    /**
     * Create a new constant named "instantiation" and instantiate "sv" with. This constant will
     * later (by "createSkolemFunctions") be replaced by a function having the occurring
     * metavariables as arguments
     *
     * @param services the Services class allowing access to the type model
     */
    public TacletApp createSkolemConstant(String instantiation, JOperatorSV sv,
            boolean interesting, Services services) {
        return createSkolemConstant(instantiation, sv, getRealSort(sv, services), interesting,
            services);
    }

    public TacletApp createSkolemConstant(String instantiation, SchemaVariable sv, Sort sort,
            boolean interesting, Services services) {
        final Function c =
            new JFunction(new Name(instantiation), sort, true, new Sort[0]);
        return addInstantiation(sv, services.getTermBuilder().func(c), interesting, services);
    }


    public void registerSkolemConstants(Namespace<@NonNull Function> fns) {
        final var insts = instantiations();
        for (final var pair : insts.getInstantiationMap()) {
            final var sv = pair.key();
            if (sv instanceof SkolemTermSV skolemSV) {
                final JTerm inst = insts.getInstantiation(skolemSV);

                // skolem constant might already be registered in
                // case it is used in the \addrules() section of a rule
                if (fns.lookup(inst.op().name()) == null) {
                    fns.addSafely((Function) inst.op());
                }
            }
        }
    }

    /**
     * adds a new instantiation to this TacletApp
     *
     * @param sv the SchemaVariable to be instantiated
     * @param term the Term the SchemaVariable is instantiated with
     * @return the new TacletApp
     */
    public abstract TacletApp addInstantiation(SchemaVariable sv, JTerm term, boolean interesting,
            Services services);

    /**
     * adds a new instantiation to this TacletApp. This method does not check (beside some very
     * rudimentary tests) if the instantiation is possible. If you cannot guarantee that adding the
     * entry <code>(sv, pe)</code> will result in a valid taclet instantiation, you have to use
     * {@link #addCheckedInstantiation(SchemaVariable, ProgramElement, Services, boolean)} instead
     *
     * @param sv the SchemaVariable to be instantiated
     * @param pe the ProgramElement the SV is instantiated with
     * @param interesting a boolean marking if the instantiation of this sv needs to be saved for
     *        later proof loading (<code>interesting==true</code>) or if it can be derived
     *        deterministically (e.g. by matching) ( <code>interesting==false</code>)
     * @return a new taclet application equal to this one but including the newly added
     *         instantiation entry <code>(sv, pe)</code>
     */
    public abstract TacletApp addInstantiation(SchemaVariable sv, ProgramElement pe,
            boolean interesting, Services services);

    /**
     * checks if the instantiation of <code>sv</code> with <code>pe</code> is possible. If the
     * resulting instantiation is valid a new taclet application with an extended instantiation
     * mapping is created and returned. Otherwise, an exception is thrown.
     *
     * @param sv the SchemaVariable to be instantiated
     * @param pe the ProgramElement the SV is instantiated with
     * @param services the Services
     * @param interesting a boolean marking if the instantiation of this sv needs to be saved for
     *        later proof loading (<code>interesting==true</code>) or if it can be derived
     *        deterministically (e.g. by matching) ( <code>interesting==false</code>)
     * @return a new taclet application equal to this one but including the newly added
     *         instantiation entry <code>(sv, pe)</code>, if the instantiation results in a valid
     *         taclet application otherwise an exception is thrown
     * @throws IllegalInstantiationException exception thrown if <code>sv</code> cannot be
     *         instantiated with <code>pe</code> no matter if in general or due to side conditions
     *         posed by this particular application
     *
     */
    public TacletApp addCheckedInstantiation(SchemaVariable sv, ProgramElement pe,
            Services services, boolean interesting) {

        final var cond = taclet().getMatcher().matchSV(sv, pe, matchConditions, services);

        if (cond == null) {
            throw new IllegalInstantiationException(
                "SchemaVariable " + sv + " could not be matched with program element " + pe
                    + " under the provided constraints " + matchConditions);
        } else {
            var svInst = (de.uka.ilkd.key.rule.inst.SVInstantiations) cond.getInstantiations();
            if (interesting) {
                svInst = svInst.makeInteresting(sv, services);
            }
            return addInstantiation(svInst, services);
        }
    }

    public TacletApp addInstantiation(SchemaVariable sv, Name name, Services services) {
        return addInstantiation(
            de.uka.ilkd.key.rule.inst.SVInstantiations.EMPTY_SVINSTANTIATIONS.addInteresting(sv,
                name, services),
            services);
    }

    /**
     * creates a new Taclet application containing all the instantiations given by the
     * SVInstantiations
     *
     * @param svi the SVInstantiations whose entries are the needed instantiations
     * @return the new Taclet application
     */
    public abstract TacletApp addInstantiation(SVInstantiations svi, Services services);

    /**
     * creates a new Taclet application containing all the instantiations given by the
     * SVInstantiations and forget the old ones
     *
     * @param svi the SVInstantiations whose entries are the needed instantiations
     * @return the new Taclet application
     */
    protected abstract TacletApp setInstantiation(SVInstantiations svi, Services services);

    /**
     * creates a new Taclet application containing all the instantiations, constraints and new
     * metavariables given by the mc object and forget the old ones
     */
    public abstract TacletApp setMatchConditions(MatchConditions mc, Services services);

    /**
     * creates a new Taclet application containing all the instantiations, constraints, new
     * metavariables and if formula instantiations given and forget the old ones
     */
    protected abstract TacletApp setAllInstantiations(
            MatchConditions mc,
            ImmutableList<AssumesFormulaInstantiation> ifInstantiations, Services services);

    /**
     * Creates a new Taclet application by matching the given formulas against the formulas of the
     * assumes-sequent, adding SV instantiations, constraints and metavariables as needed. This will
     * fail
     * if the assumes-formulas have already been instantiated.
     */
    public TacletApp setIfFormulaInstantiations(ImmutableList<AssumesFormulaInstantiation> p_list,
            Services p_services) {
        if (p_list == null) {
            // (LG 2022-02-07) Apparently findIfFormulaInstantiations() might return null
            // instantiations that should actually be nil().
            // So we replace null with nil() here as a bugfix.
            p_list = ImmutableSLList.nil();
        }
        assert ifInstsCorrectSize(p_list) && ifInstantiations == null
                : "If instantiations list has wrong size "
                    + "or the if formulas have already been instantiated";

        MatchConditions mc =
            taclet().getMatcher().matchAssumes(p_list, matchConditions, p_services);

        return mc == null ? null : setAllInstantiations(mc, p_list, p_services);
    }

    /**
     * Find all possible instantiations of the assumes-sequent formulas within the sequent "seq".
     *
     * @param seq uninstantiated if sequent from taclet
     * @param services the {@link Services} to access information about the logic signature or
     *        program model
     * @return a list of tacletapps with the found assumes-formula instantiations When the IfSequent
     *         is
     *         empty, it returns a tacletapp with ifInstantiations == null instead of
     *         ifInstantiations == nil(), seemingly (LG 2022-02-07) to be more efficient.
     */
    public ImmutableList<TacletApp> findIfFormulaInstantiations(Sequent seq, Services services) {
        // TODO Why not return just the list of IfFormulaInstantiations?

        Debug.assertTrue(ifInstantiations == null,
            "The if formulas have already been instantiated");

        if (taclet().assumesSequent().isEmpty()) {
            return ImmutableSLList.<TacletApp>nil().prepend(this);
        }

        return findIfFormulaInstantiationsHelp(
            createSemisequentList(taclet().assumesSequent().succedent()),
            createSemisequentList(taclet().assumesSequent().antecedent()),
            AssumesFormulaInstSeq.createList(seq, false, services),
            AssumesFormulaInstSeq.createList(seq, true, services),
            ImmutableSLList.nil(), matchConditions(), services);
    }

    /**
     * Recursive function for matching the remaining tail of an if sequent
     *
     * @param ruleSuccTail tail of the current uninstantiated semisequent as list (i.e. if
     *        succedent)
     * @param ruleAntecTail the following uninstantiated semisequent (i.e. if antecedent) or null
     * @param instSucc list of the formulas to match the current if semisequent formulas with
     * @param instAntec list of the formulas of the antecedent
     * @param instAlreadyMatched matched instantiations, for exactly those formulas that are no
     *        longer in ruleSuccTail and ruleAntecTail
     * @param matchCond match conditions until now, i.e. after matching the first formulas of the
     *        assumes-sequent
     * @param services the {@link Services} to access information about the logic signature or
     *        program model
     * @return a list of tacletapps with the found if formula instantiations
     */
    private ImmutableList<TacletApp> findIfFormulaInstantiationsHelp(
            ImmutableList<SequentFormula> ruleSuccTail,
            ImmutableList<SequentFormula> ruleAntecTail,
            ImmutableArray<AssumesFormulaInstantiation> instSucc,
            ImmutableArray<AssumesFormulaInstantiation> instAntec,
            ImmutableList<AssumesFormulaInstantiation> instAlreadyMatched,
            MatchConditions matchCond,
            Services services) {

        while (ruleSuccTail.isEmpty()) {
            if (ruleAntecTail == null) {
                // All formulas have been matched, collect the results
                TacletApp res = setAllInstantiations(matchCond, instAlreadyMatched, services);
                if (res != null) {
                    return ImmutableSLList.<TacletApp>nil().prepend(res);
                }
                return ImmutableSLList.nil();
            } else {
                // Change from succedent to antecedent
                ruleSuccTail = ruleAntecTail;
                ruleAntecTail = null;
                instSucc = instAntec;
            }
        }

        // Match the current formula
        AssumesMatchResult mr =
            taclet().getMatcher().matchAssumes(instSucc, ruleSuccTail.head().formula(),
                matchCond, services);

        // For each matching formula call the method again to match
        // the remaining terms
        ImmutableList<TacletApp> res = ImmutableSLList.nil();
        Iterator<MatchConditions> itMC = mr.matchConditions().iterator();
        ruleSuccTail = ruleSuccTail.tail();
        for (final AssumesFormulaInstantiation instantiationCandidate : mr.candidates()) {
            res = res.prepend(findIfFormulaInstantiationsHelp(ruleSuccTail, ruleAntecTail, instSucc,
                instAntec, instAlreadyMatched.prepend(instantiationCandidate),
                itMC.next(), services));
        }

        return res;
    }

    private ImmutableList<SequentFormula> createSemisequentList(
            Semisequent p_ss) {
        ImmutableList<SequentFormula> res = ImmutableSLList.nil();

        for (SequentFormula p_s : p_ss) {
            res = res.prepend(p_s);
        }

        return res;
    }

    /**
     * returns a new PosTacletApp that is equal to this TacletApp except that the position is set to
     * the given PosInOccurrence.
     *
     * <p>
     * <b>CAUTION:</b> If you call this method, consider to call
     * {@link NoPosTacletApp#matchFind(PosInOccurrence, Services)}
     * first (if applicable) as
     * otherwise the TacletApp may become invalid. (This happened sometimes during interactive
     * proofs).
     *
     * @param pos the PosInOccurrence of the newl created PosTacletApp
     * @return the new TacletApp
     */
    public PosTacletApp setPosInOccurrence(PosInOccurrence pos,
            Services services) {
        if (taclet() instanceof NoFindTaclet) {
            throw new IllegalStateException("Cannot add position to an taclet" + " without find");
        }
        return PosTacletApp.createPosTacletApp((FindTaclet) taclet(), instantiations(),
            assumesFormulaInstantiations(), pos, services);
    }

    /**
     * @return true iff the if-instantiation list is not null or no if sequent is needed
     */
    public boolean assumesInstantionsComplete() {
        return ifInstantiations != null || taclet().assumesSequent().isEmpty();
    }

    /**
     * compares the given Object with this one and returns true iff both are from type TacletApp
     * with equal taclets, instantiations and positions.
     */
    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o == null || o.getClass() != this.getClass()) {
            return false;
        }
        final TacletApp s = (TacletApp) o;
        return (s.taclet.equals(taclet) && s.instantiations.equals(instantiations))
                && (Objects.equals(ifInstantiations, s.ifInstantiations));
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 37 * result + taclet.hashCode();
        result = 37 * result + instantiations.hashCode();
        result = 37 * result + (ifInstantiations == null ? 0 : ifInstantiations.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "Application of Taclet " + taclet() + " with " + instantiations() + " and "
            + assumesFormulaInstantiations();
    }

    /**
     * checks if there are name conflicts (i.e. there are two matched bound SchemaVariable that are
     * matched to variables with an equal name); if yes a new TacletApp is returned that equals this
     * TacletApp except that the name conflict is resolved by replacing the instantiation of one of
     * the conflict-causing SchemaVariables by a bound SchemaVariable with a new name; if the check
     * is negative, the same TacletApp is returned.
     *
     * @return a conflict resolved TacletApp, remainder equal to this TacletApp
     */
    public TacletApp prepareUserInstantiation(Services services) {
        TacletApp result = this;
        var varSV = varSVNameConflict();
        while (varSV != null) {
            result = setInstantiation(
                replaceInstantiation(taclet, result.instantiations(), varSV, services), services);
            varSV = result.varSVNameConflict();
        }
        return result;
    }

    protected abstract ImmutableSet<QuantifiableVariable> contextVars(SchemaVariable sv);

    /**
     * creates a new variable namespace by adding names of the instantiations of the schema
     * variables in the context of the given schema variable and (if the TacletApp's prefix has the
     * context flag set) by adding names of the logic variables of the context.
     *
     * @param sv the schema variable to be considered
     * @param var_ns the old variable namespace
     * @return the new created variable namespace
     */
    public Namespace<@NonNull QuantifiableVariable> extendVarNamespaceForSV(
            Namespace<@NonNull QuantifiableVariable> var_ns, SchemaVariable sv) {
        Namespace<@NonNull QuantifiableVariable> ns = new Namespace<>(var_ns);
        TacletPrefix tacletPrefix = (TacletPrefix) taclet().getPrefix(sv);
        for (final var schemaVariable : tacletPrefix.prefix()) {
            if (instantiations().getInstantiation(schemaVariable) instanceof JTerm term &&
                    term.op() instanceof LogicVariable lv) {
                ns.add(lv);
            } else {
                throw new IllegalStateException(
                    "Tried to extend the quantifiable variable namespace with something" +
                        "that is not a logicvariable");
            }
        }
        if (tacletPrefix.context()) {
            for (final QuantifiableVariable quantifiableVariable : contextVars(sv)) {
                ns.add(quantifiableVariable);
            }
        }
        return ns;
    }

    /**
     * create a new function namespace by adding all newly instantiated skolem symbols to a new
     * namespace.
     *
     * @author mulbrich
     * @param func_ns the original function namespace, not <code>null</code>
     * @return the new function namespace that bases on the original one
     */
    public Namespace<@NonNull Function> extendedFunctionNameSpace(
            Namespace<@NonNull Function> func_ns) {
        Namespace<@NonNull Function> ns = new Namespace<>(func_ns);
        for (var pair : instantiations.getInstantiationMap()) {
            if (pair.key() instanceof SkolemTermSV skolemSV) {
                JTerm inst = instantiations.getInstantiation(skolemSV);
                Operator op = inst.op();
                assert op instanceof Function
                        : "At this point the skolem instantiation is expected to "
                            + "be a function symbol, not " + inst;
                ns.addSafely((Function) op);
            }
        }
        return ns;
    }

    /**
     * returns the bound SchemaVariable that causes a name conflict (i.e. there are two matched
     * bound SchemaVariables that are matched to variables with an equal name, the returned
     * SchemaVariable is (an arbitrary) one of these SchemaVariables) or null if there is no such
     * conflict
     *
     * @return null iff there is no conflict and one of the conflict-causing bound SchemaVariables
     */
    public SchemaVariable varSVNameConflict() {
        for (final SchemaVariable sv : uninstantiatedVars()) {
            if (sv instanceof TermSV || sv instanceof FormulaSV) {
                TacletPrefix prefix = (TacletPrefix) taclet().getPrefix(sv);
                HashSet<Name> names = new LinkedHashSet<>();
                if (prefix.context()) {
                    for (QuantifiableVariable quantifiableVariable : contextVars(sv)) {
                        names.add(quantifiableVariable.name());
                    }
                }
                for (var varSV : prefix.prefix()) {
                    JTerm inst = (JTerm) instantiations().getInstantiation(varSV);
                    if (inst != null) {
                        Name name = inst.op().name();
                        if (!names.contains(name)) {
                            names.add(name);
                        } else {
                            return varSV;
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * check whether the number of if instantiations is correct
     *
     * @param list list of instantiations (non-null)
     * @return true iff the list of if instantiations has the correct size
     */
    public boolean ifInstsCorrectSize(ImmutableList<AssumesFormulaInstantiation> list) {
        Semisequent antec = taclet().assumesSequent().antecedent();
        Semisequent succ = taclet().assumesSequent().succedent();
        return list.size() == (antec.size() + succ.size());
    }

    /**
     * only for debugging purposes; otherwise use {@link #ifInstsCorrectSize(ImmutableList)}
     *
     * @return true iff the list of if instantiations has the correct size or is null
     */
    protected static boolean ifInstsCorrectSize(org.key_project.prover.rules.Taclet p_taclet,
            ImmutableList<AssumesFormulaInstantiation> p_list) {
        return p_list == null || p_list.size() == (p_taclet.assumesSequent().antecedent().size()
                + p_taclet.assumesSequent().succedent().size());
    }

    public ProgramElement getProgramElement(String instantiation, ProgramSV sv,
            Services services) {
        Sort svSort = sv.sort();
        if (svSort == ProgramSVSort.LABEL) {
            return new ProgramElementName(instantiation);
        } else if (svSort == ProgramSVSort.VARIABLE) {
            final NewVarcond nvc = (NewVarcond) taclet.varDeclaredNew(sv);
            if (nvc != null) {
                KeYJavaType kjt;
                Object o = nvc.getTypeDefiningObject();
                if (o instanceof SchemaVariable peerSV) {
                    final TypeConverter tc = services.getTypeConverter();
                    final Object peerInst = instantiations().getInstantiation(peerSV);
                    if (peerInst instanceof TypeReference) {
                        kjt = ((TypeReference) peerInst).getKeYJavaType();
                    } else {
                        Expression peerInstExpr;
                        if (peerInst instanceof JTerm peerTerm) {
                            peerInstExpr = tc.convertToProgramElement(peerTerm);
                        } else {
                            peerInstExpr = (Expression) peerInst;
                        }
                        kjt = tc.getKeYJavaType(peerInstExpr,
                            instantiations().getContextInstantiation().activeStatementContext());
                    }
                } else {
                    kjt = (KeYJavaType) o;
                }
                assert kjt != null : "could not find kjt for: " + o;
                return new LocationVariable(VariableNamer.parseName(instantiation), kjt);
            }
        }
        return null;
    }

    /**
     * checks if the variable conditions of type 'x not free in y' are hold by the found
     * instantiations. The variable conditions is used implicit in the prefix. (Used to calculate
     * the prefix)
     *
     * @param taclet the Taclet that is tried to be instantiated. A match for the find (or/and if)
     *        has been found.
     * @param instantiations the SVInstantiations so that the find(if) expression matches
     * @param pos the PosInOccurrence where the Taclet is applied
     * @return true iff all variable conditions x not free in y are hold
     */
    public static boolean checkVarCondNotFreeIn(org.key_project.prover.rules.Taclet taclet,
            SVInstantiations instantiations,
            PosInOccurrence pos) {
        final var it = ((de.uka.ilkd.key.rule.inst.SVInstantiations) instantiations).svIterator();
        while (it.hasNext()) {
            var sv = it.next();
            if (sv instanceof TermSV || sv instanceof FormulaSV) {
                if (!((JTerm) instantiations.getInstantiation(sv)).freeVars()
                        .subset(boundAtOccurrenceSet((TacletPrefix) taclet.getPrefix(sv),
                            instantiations, pos))) {

                    return false;
                }
            }
        }
        return true;
    }

    /**
     * collects all bound vars that are bound above the subterm described by the given term position
     * information
     *
     * @param pos the PosInOccurrence describing a subterm in Term
     * @return a set of logic variables that are bound above the specified subterm
     */
    protected static ImmutableSet<QuantifiableVariable> collectBoundVarsAbove(
            PosInOccurrence pos) {
        ImmutableSet<QuantifiableVariable> result = DefaultImmutableSet.nil();

        PIOPathIterator it = pos.iterator();
        int i;
        ImmutableArray<QuantifiableVariable> vars;

        while ((i = it.next()) != -1) {
            vars = (ImmutableArray<QuantifiableVariable>) it.getSubTerm().varsBoundHere(i);
            for (i = 0; i < vars.size(); i++) {
                result = result.add(vars.get(i));
            }
        }

        return result;
    }

}
