/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.symbolic_execution.slicing;

import java.util.*;

import de.uka.ilkd.key.java.Expression;
import de.uka.ilkd.key.java.JavaTools;
import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.SourceElement;
import de.uka.ilkd.key.java.expression.PassiveExpression;
import de.uka.ilkd.key.java.reference.*;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.ldt.JavaDLTheory;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.label.ParameterlessTermLabel;
import de.uka.ilkd.key.logic.label.SymbolicExecutionTermLabel;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.mgt.ProofEnvironment;
import de.uka.ilkd.key.strategy.StrategyProperties;
import de.uka.ilkd.key.symbolic_execution.object_model.ISymbolicEquivalenceClass;
import de.uka.ilkd.key.symbolic_execution.util.SymbolicExecutionSideProofUtil;
import de.uka.ilkd.key.symbolic_execution.util.SymbolicExecutionUtil;
import de.uka.ilkd.key.util.ProofStarter;
import de.uka.ilkd.key.util.SideProofUtil;

import org.key_project.logic.Name;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.UpdateableOperator;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.engine.impl.ApplyStrategyInfo;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;
import org.key_project.util.collection.Pair;
import org.key_project.util.java.CollectionUtil;

/**
 * Defines the basic functionality for slicing algorithms.
 *
 * @author Martin Hentschel
 */
public abstract class AbstractSlicer {
    /**
     * Computes the slice.
     *
     * @param seedNode The seed {@link Node} to start slicing at.
     * @param term The seed {@link JTerm}.
     * @param sec The optional {@link ISymbolicEquivalenceClass}es to consider.
     * @return The computed slice.
     */
    public ImmutableArray<Node> slice(Node seedNode, JTerm term,
            ImmutableList<ISymbolicEquivalenceClass> sec) throws ProofInputException {
        return slice(seedNode, toLocation(seedNode.proof().getServices(), term), sec);
    }

    /**
     * Computes the slice.
     *
     * @param seedNode The seed {@link Node} to start slicing at.
     * @param seedLocation The seed {@link ReferencePrefix}.
     * @param sec The optional {@link ISymbolicEquivalenceClass}es to consider.
     * @return The computed slice.
     */
    public ImmutableArray<Node> slice(Node seedNode, ReferencePrefix seedLocation,
            ImmutableList<ISymbolicEquivalenceClass> sec) throws ProofInputException {
        // Solve this reference
        PosInOccurrence pio =
            seedNode.getAppliedRuleApp().posInOccurrence();
        final JTerm topLevel = (JTerm) pio.sequentFormula().formula();
        JTerm modalityTerm = TermBuilder.goBelowUpdates(topLevel);
        Services services = seedNode.proof().getServices();
        ExecutionContext ec =
            JavaTools.getInnermostExecutionContext(modalityTerm.javaBlock(), services);
        ReferencePrefix thisReference = ec != null ? ec.getRuntimeInstance() : null;
        // Perform slicing
        return slice(seedNode, toLocation(services, seedLocation, ec, thisReference), sec);
    }

    /**
     * Computes the slice.
     *
     * @param seedNode The seed {@link Node} to start slicing at.
     * @param seedLocation The seed {@link ReferencePrefix}.
     * @param sec The optional {@link ISymbolicEquivalenceClass}es to consider.
     * @return The computed slice.
     */
    public ImmutableArray<Node> slice(Node seedNode, Location seedLocation,
            ImmutableList<ISymbolicEquivalenceClass> sec) throws ProofInputException {
        // Ensure that seed node is valid
        if (seedNode.getAppliedRuleApp() == null) {
            throw new IllegalStateException(
                "No rule applied on seed Node '" + seedNode.serialNr() + "'.");
        }
        PosInOccurrence pio =
            seedNode.getAppliedRuleApp().posInOccurrence();
        JTerm applicationTerm = (JTerm) pio.subTerm();
        Pair<ImmutableList<JTerm>, JTerm> pair = TermBuilder.goBelowUpdates2(applicationTerm);
        JTerm modalityTerm = pair.second;
        SymbolicExecutionTermLabel label =
            SymbolicExecutionUtil.getSymbolicExecutionLabel(modalityTerm);
        if (label == null) {
            throw new IllegalStateException("Modality at applied rule does not have the "
                + SymbolicExecutionTermLabel.NAME + " term label.");
        }
        // Perform slicing
        return doSlicing(seedNode, seedLocation, sec);
    }

    /**
     * Performs the slicing.
     *
     * @param seedNode The seed {@link Node} to start slicing at.
     * @param seedLocation The seed {@link Location}.
     * @param sec The optional {@link ISymbolicEquivalenceClass}es to consider.
     * @return The computed slice.
     */
    protected abstract ImmutableArray<Node> doSlicing(Node seedNode, Location seedLocation,
            ImmutableList<ISymbolicEquivalenceClass> sec) throws ProofInputException;

    /**
     * The result returned by {@link AbstractSlicer#analyzeSequent}.
     *
     * @author Martin Hentschel
     */
    protected static class SequentInfo {
        /**
         * The found aliases.
         */
        private final Map<Location, SortedSet<Location>> aliases;

        /**
         * The local values.
         */
        private final Map<ProgramVariable, JTerm> localValues;

        /**
         * The current {@link ExecutionContext}.
         */
        private final ExecutionContext executionContext;

        /**
         * The 'this'-reference if available.
         */
        private final ReferencePrefix thisReference;

        /**
         * Constructor.
         *
         * @param aliases The found aliases.
         * @param thisReference The this-reference if available.
         */
        public SequentInfo(Map<Location, SortedSet<Location>> aliases,
                Map<ProgramVariable, JTerm> localValues, ExecutionContext executionContext,
                ReferencePrefix thisReference) {
            assert aliases != null;
            assert localValues != null;
            this.aliases = aliases;
            this.localValues = localValues;
            this.executionContext = executionContext;
            this.thisReference = thisReference;
        }

        /**
         * Returns the found aliases.
         *
         * @return The found aliases.
         */
        public Map<Location, SortedSet<Location>> getAliases() {
            return aliases;
        }

        /**
         * Returns the local values.
         *
         * @return The local values.
         */
        public Map<ProgramVariable, JTerm> getLocalValues() {
            return localValues;
        }

        /**
         * Returns the current {@link ExecutionContext}.
         *
         * @return The current {@link ExecutionContext}.
         */
        public ExecutionContext getExecutionContext() {
            return executionContext;
        }

        /**
         * Returns the this-reference if available.
         *
         * @return The this-reference if available.
         */
        public ReferencePrefix getThisReference() {
            return thisReference;
        }
    }

    /**
     * Computes the aliases specified by the updates of the current {@link Node} at the application
     * {@link PosInOccurrence} and computes the current {@code this} reference.
     *
     * @param node The {@link Node} to analyze.
     * @return The computed {@link SequentInfo} or {@code null} if the {@link Node} is not
     *         supported.
     */
    protected SequentInfo analyzeSequent(Node node, ImmutableList<ISymbolicEquivalenceClass> sec) {
        PosInOccurrence pio =
            node.getAppliedRuleApp().posInOccurrence();
        JTerm topLevel = (JTerm) pio.sequentFormula().formula();
        Pair<ImmutableList<JTerm>, JTerm> pair = TermBuilder.goBelowUpdates2(topLevel);
        JTerm modalityTerm = pair.second;
        SymbolicExecutionTermLabel label =
            SymbolicExecutionUtil.getSymbolicExecutionLabel(modalityTerm);
        Services services = node.proof().getServices();
        HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
        if (label != null) {
            // Solve this reference
            ExecutionContext ec =
                JavaTools.getInnermostExecutionContext(modalityTerm.javaBlock(), services);
            ReferencePrefix thisReference = ec != null ? ec.getRuntimeInstance() : null;
            // Compute aliases
            Map<Location, SortedSet<Location>> aliases =
                new HashMap<>();
            Map<ProgramVariable, JTerm> localValues = new HashMap<>();
            analyzeEquivalenceClasses(services, sec, aliases, thisReference);
            analyzeSequent(services, node.sequent(), aliases, thisReference);
            analyzeUpdates(pair.first, services, heapLDT, aliases, localValues, ec, thisReference);
            return new SequentInfo(aliases, localValues, ec, thisReference);
        } else {
            return null; // Not the modality of interest.
        }
    }

    /**
     * Analyzes the given {@link ISymbolicEquivalenceClass}es.
     *
     * @param services The {@link Services} to use.
     * @param sec The {@link ISymbolicEquivalenceClass} to analyze.
     * @param aliases The alias {@link Map} to fill.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void analyzeEquivalenceClasses(Services services,
            ImmutableList<ISymbolicEquivalenceClass> sec,
            Map<Location, SortedSet<Location>> aliases, ReferencePrefix thisReference) {
        if (sec != null) {
            for (ISymbolicEquivalenceClass eq : sec) {
                ImmutableList<JTerm> terms = eq.getTerms();
                List<Location> locations = new ArrayList<>(terms.size());
                for (JTerm term : terms) {
                    if (SymbolicExecutionUtil.hasReferenceSort(services, term)) {
                        Location location = toLocation(services, term);
                        if (location != null) {
                            locations.add(location);
                        }
                    }
                }
                if (locations.size() >= 2) {
                    Location first = null;
                    for (Location location : locations) {
                        if (first == null) {
                            first = location;
                        } else {
                            updateAliases(services, first, location, aliases, thisReference);
                        }
                    }
                }
            }
        }
    }

    /**
     * Analyzes the given {@link Sequent} for equalities specified by top level formulas.
     *
     * @param services The {@link Services} to use.
     * @param sequent The {@link Sequent} to analyze.
     * @param aliases The alias {@link Map} to fill.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void analyzeSequent(Services services, Sequent sequent,
            Map<Location, SortedSet<Location>> aliases, ReferencePrefix thisReference) {
        for (SequentFormula sf : sequent.antecedent()) {
            JTerm term = (JTerm) sf.formula();
            if (Equality.EQUALS == term.op()) {
                analyzeEquality(services, term, aliases, thisReference);
            }
        }
        for (SequentFormula sf : sequent.succedent()) {
            JTerm term = (JTerm) sf.formula();
            if (Junctor.NOT == term.op()) {
                JTerm negatedTerm = term.sub(0);
                if (Equality.EQUALS == negatedTerm.op()) {
                    analyzeEquality(services, negatedTerm, aliases, thisReference);
                }
            }
        }
    }

    /**
     * Analyzes the given equality {@link JTerm} for aliased locations.
     *
     * @param services The {@link Services} to use.
     * @param equality The equality {@link JTerm} to analyze.
     * @param aliases The alias {@link Map} to fill.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void analyzeEquality(Services services, JTerm equality,
            Map<Location, SortedSet<Location>> aliases, ReferencePrefix thisReference) {
        JTerm firstSub = equality.sub(0);
        JTerm secondSub = equality.sub(1);
        if (SymbolicExecutionUtil.hasReferenceSort(services, firstSub)
                && SymbolicExecutionUtil.hasReferenceSort(services, secondSub)) {
            Location first = toLocation(services, firstSub);
            Location second = toLocation(services, secondSub);
            if (first != null && second != null) {
                updateAliases(services, first, second, aliases, thisReference);
            }
        }
    }

    /**
     * Utility method used by {@link #analyzeSequent} to analyze the given updates.
     *
     * @param updates The update {@link JTerm}s to analyze.
     * @param services The {@link Services} to use.
     * @param heapLDT The {@link HeapLDT} of the {@link Services}.
     * @param aliases The alias {@link Map} to fill.
     * @param localValues The local values to fill.
     * @param ec The current {@link ExecutionContext}.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void analyzeUpdates(ImmutableList<JTerm> updates, Services services, HeapLDT heapLDT,
            Map<Location, SortedSet<Location>> aliases, Map<ProgramVariable, JTerm> localValues,
            ExecutionContext ec, ReferencePrefix thisReference) {
        for (JTerm update : updates) {
            analyzeUpdate(update, services, heapLDT, aliases, localValues, ec, thisReference);
        }
    }

    /**
     * Recursive utility method used by
     * {@link #analyzeUpdates} to analyze a given update.
     *
     * @param term The update {@link JTerm} to analyze.
     * @param services The {@link Services} to use.
     * @param heapLDT The {@link HeapLDT} of the {@link Services}.
     * @param aliases The alias {@link Map} to fill.
     * @param localValues The local values to fill.
     * @param ec The current {@link ExecutionContext}.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void analyzeUpdate(JTerm term, Services services, HeapLDT heapLDT,
            Map<Location, SortedSet<Location>> aliases, Map<ProgramVariable, JTerm> localValues,
            ExecutionContext ec, ReferencePrefix thisReference) {
        if (term.op() == UpdateJunctor.PARALLEL_UPDATE
                || term.op() == UpdateApplication.UPDATE_APPLICATION) {
            for (int i = 0; i < term.arity(); i++) {
                analyzeUpdate(term.sub(i), services, heapLDT, aliases, localValues, ec,
                    thisReference);
            }
        } else if (term.op() instanceof ElementaryUpdate) {
            UpdateableOperator target = ((ElementaryUpdate) term.op()).lhs();
            if (SymbolicExecutionUtil.isHeap(target, heapLDT)) {
                analyzeHeapUpdate(term.sub(0), services, heapLDT, aliases, thisReference);
            } else {
                if (target instanceof ProgramVariable) {
                    localValues.put((ProgramVariable) target, term.sub(0));
                }
                Location sourceLocation = toLocation(services, term.sub(0));
                if (target instanceof ReferencePrefix && sourceLocation != null) {
                    Location targetLocation =
                        toLocation(services, (ReferencePrefix) target, ec, thisReference);
                    updateAliases(services, targetLocation, sourceLocation, aliases, thisReference);
                }
            }
        } else {
            throw new IllegalArgumentException("Can not analyze update '" + term + "'.");
        }
    }

    /**
     * Recursive utility method used by {@link #analyzeUpdate} to
     * analyze a given update.
     *
     * @param term The heap update {@link JTerm} to analyze.
     * @param services The {@link Services} to use.
     * @param heapLDT The {@link HeapLDT} of the {@link Services}.
     * @param aliases The alias {@link Map} to fill.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void analyzeHeapUpdate(JTerm term, Services services, HeapLDT heapLDT,
            Map<Location, SortedSet<Location>> aliases, ReferencePrefix thisReference) {
        final Function store = heapLDT.getStore();
        final Function create = heapLDT.getCreate();
        if (term.op() == store) {
            // Analyze parent heap
            analyzeHeapUpdate(term.sub(0), services, heapLDT, aliases, thisReference);
            // Check for alias in current store
            if (SymbolicExecutionUtil.hasReferenceSort(services, term.sub(3))) {
                Location source = toLocation(services, term.sub(3));
                if (source != null) {
                    Location targetPrefix = toLocation(services, term.sub(1));
                    Location targetVariable = toLocation(services, term.sub(2));
                    updateAliases(services,
                        targetPrefix != null ? targetPrefix.append(targetVariable) : targetVariable,
                        source, aliases, thisReference);
                }
            }
        } else if (term.op() == create) {
            // Analyze parent heap
            analyzeHeapUpdate(term.sub(0), services, heapLDT, aliases, thisReference);
        } else if (term.op() instanceof IProgramVariable) {
            // Nothing to do, root of heap reached.
        } else if (SymbolicExecutionUtil.isHeap(term.op(), heapLDT)) {
            // Nothing to do, just another heap
        } else {
            throw new IllegalStateException("Can not analyze heap update '" + term + "'.");
        }
    }

    /**
     * Recursive method to list all modified {@link Location}s in the given {@link JTerm}.
     *
     * @param term The update {@link JTerm} to analyze.
     * @param services The {@link Services} to use.
     * @param heapLDT The {@link HeapLDT} of the {@link Services}.
     * @param listToFill The result {@link List} with {@link Location}s to fill.
     * @param ec The current {@link ExecutionContext}.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void listModifiedLocations(JTerm term, Services services, HeapLDT heapLDT,
            List<Location> listToFill, ExecutionContext ec, ReferencePrefix thisReference,
            Set<Location> relevantLocations, Node node) throws ProofInputException {
        if (term.op() == UpdateJunctor.PARALLEL_UPDATE
                || term.op() == UpdateApplication.UPDATE_APPLICATION) {
            for (int i = 0; i < term.arity(); i++) {
                listModifiedLocations(term.sub(i), services, heapLDT, listToFill, ec, thisReference,
                    relevantLocations, node);
            }
        } else if (term.op() instanceof ElementaryUpdate) {
            UpdateableOperator target = ((ElementaryUpdate) term.op()).lhs();
            if (SymbolicExecutionUtil.isBaseHeap(target, heapLDT)) {
                listModifiedHeapLocations(term.sub(0), services, heapLDT, listToFill, thisReference,
                    relevantLocations, node);
            } else {
                if (target instanceof ProgramVariable) {
                    listToFill
                            .add(toLocation(services, (ProgramVariable) target, ec, thisReference));
                }
            }
        } else {
            throw new IllegalArgumentException("Can not analyze update '" + term + "'.");
        }
    }

    /**
     * Recursive utility method used by
     * {@link #listModifiedLocations} to analyze a
     * given update.
     *
     * @param term The heap update {@link JTerm} to analyze.
     * @param services The {@link Services} to use.
     * @param heapLDT The {@link HeapLDT} of the {@link Services}.
     * @param listToFill The result {@link List} with {@link Location}s to fill.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void listModifiedHeapLocations(JTerm term, Services services, HeapLDT heapLDT,
            List<Location> listToFill, ReferencePrefix thisReference,
            Set<Location> relevantLocations, Node node) throws ProofInputException {
        if (term.op() == heapLDT.getStore()) {
            // Analyze parent heap
            listModifiedHeapLocations(term.sub(0), services, heapLDT, listToFill, thisReference,
                relevantLocations, node);
            // Check for alias in current store
            if (SymbolicExecutionUtil.hasReferenceSort(services, term.sub(3))) {
                Location source = toLocation(services, term.sub(3));
                if (source != null) {
                    Location targetPrefix = toLocation(services, term.sub(1));
                    listToFill.add(targetPrefix);
                }
            }
        } else if (term.op() == heapLDT.getCreate()) {
            // Analyze parent heap
            listModifiedHeapLocations(term.sub(0), services, heapLDT, listToFill, thisReference,
                relevantLocations, node);
        } else if (term.op() instanceof IProgramVariable) {
            // Nothing to do, root of heap reached.
        } else if (term.op() == heapLDT.getAnon()) {
            if (!relevantLocations.isEmpty()) { // Nothing to do if relevant locations are empty
                JTerm anonHeap = term.sub(2);
                // Idea: Compute all values of relevant locations in a side proof. Modified
                // locations are anonymized.
                // New OneStepSimplifier is required because it has an internal state and the
                // default instance can't be used parallel.
                ProofEnvironment sideProofEnv = SymbolicExecutionSideProofUtil
                        .cloneProofEnvironmentWithOwnOneStepSimplifier(node.proof(), true);
                ApplyStrategyInfo<Proof, Goal> info = null;
                try {
                    // Create location terms
                    List<Location> resultLocations =
                        new ArrayList<>(relevantLocations.size());
                    List<JTerm> resultTerms = new ArrayList<>(relevantLocations.size());
                    List<Sort> resultSorts = new ArrayList<>(relevantLocations.size());
                    for (Location location : relevantLocations) {
                        JTerm locationTerm =
                            location.toTerm(sideProofEnv.getServicesForEnvironment());
                        if (!(locationTerm.op() instanceof IProgramVariable)) { // Ignore local
                                                                                // variables.
                            resultLocations.add(location);
                            resultTerms.add(locationTerm);
                            resultSorts.add(locationTerm.sort());
                        }
                    }
                    if (!resultTerms.isEmpty()) {
                        // Create predicate which will be used in formulas to store the value
                        // interested in.
                        Function newPredicate = new JFunction(
                            new Name(sideProofEnv.getServicesForEnvironment().getTermBuilder()
                                    .newName("ResultPredicate")),
                            JavaDLTheory.FORMULA, new ImmutableArray<>(resultSorts));
                        // Create formula which contains the value interested in.
                        JTerm newTerm =
                            sideProofEnv.getServicesForEnvironment().getTermBuilder().func(
                                newPredicate, resultTerms.toArray(new JTerm[0]));

                        Sequent sequentToProve =
                            SymbolicExecutionUtil.createSequentToProveWithNewSuccedent(node,
                                node.getAppliedRuleApp().posInOccurrence(), newTerm);
                        ProofStarter starter = SideProofUtil.createSideProof(sideProofEnv,
                            sequentToProve, "Analyze Anon Update");
                        info = SymbolicExecutionSideProofUtil.startSideProof(node.proof(), starter,
                            StrategyProperties.METHOD_CONTRACT, StrategyProperties.LOOP_INVARIANT,
                            StrategyProperties.QUERY_ON, StrategyProperties.SPLITTING_NORMAL);
                        // Check for anonymized values in the side proof goals
                        assert !info.getProof().closed();
                        for (Goal goal : info.getProof().openGoals()) {
                            JTerm operatorTerm = SymbolicExecutionSideProofUtil
                                    .extractOperatorTerm(goal, newPredicate);
                            assert operatorTerm != null;
                            for (int i = 0; i < operatorTerm.arity(); i++) {
                                JTerm valueTerm = SymbolicExecutionUtil.replaceSkolemConstants(
                                    goal.sequent(), operatorTerm.sub(i), services);
                                if (valueTerm.arity() >= 1) {
                                    JTerm heap = valueTerm.sub(0);
                                    if (anonHeap.equals(heap)) {
                                        listToFill.add(resultLocations.get(i));
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    SymbolicExecutionSideProofUtil.disposeOrStore("Analyze Anon Update", info);
                }
            }
        } else if (SymbolicExecutionUtil.isHeap(term.op(), heapLDT)) {
            if (!relevantLocations.isEmpty()) { // Nothing to do if relevant locations are empty
                // Idea: Compute all values of relevant locations in a side proof. Modified
                // locations are anonymized.
                // New OneStepSimplifier is required because it has an internal state and the
                // default instance can't be used parallel.
                ProofEnvironment sideProofEnv = SymbolicExecutionSideProofUtil
                        .cloneProofEnvironmentWithOwnOneStepSimplifier(node.proof(), true);
                ApplyStrategyInfo<Proof, Goal> info = null;
                try {
                    // Create location terms
                    List<Location> resultLocations =
                        new ArrayList<>(relevantLocations.size());
                    List<JTerm> resultTerms = new ArrayList<>(relevantLocations.size());
                    List<Sort> resultSorts = new ArrayList<>(relevantLocations.size());
                    for (Location location : relevantLocations) {
                        JTerm locationTerm =
                            location.toTerm(sideProofEnv.getServicesForEnvironment());
                        if (!(locationTerm.op() instanceof IProgramVariable)) { // Ignore local
                                                                                // variables.
                            resultLocations.add(location);
                            resultTerms.add(locationTerm);
                            resultSorts.add(locationTerm.sort());
                        }
                    }
                    if (!resultTerms.isEmpty()) {
                        // Create predicate which will be used in formulas to store the value
                        // interested in.
                        Function newPredicate = new JFunction(
                            new Name(sideProofEnv.getServicesForEnvironment().getTermBuilder()
                                    .newName("ResultPredicate")),
                            JavaDLTheory.FORMULA, new ImmutableArray<>(resultSorts));
                        // Create formula which contains the value interested in.
                        TermBuilder tb = sideProofEnv.getServicesForEnvironment().getTermBuilder();
                        JTerm newTerm = tb.func(newPredicate,
                            resultTerms.toArray(new JTerm[0]));
                        newTerm = tb.apply(
                            tb.elementary(heapLDT.getHeapForName(HeapLDT.BASE_HEAP_NAME), term),
                            newTerm);
                        Sequent sequentToProve = SymbolicExecutionUtil
                                .createSequentToProveWithNewSuccedent(node, null, newTerm);
                        ProofStarter starter = SideProofUtil.createSideProof(sideProofEnv,
                            sequentToProve, "Analyze Anon Update");
                        info = SymbolicExecutionSideProofUtil.startSideProof(node.proof(), starter,
                            StrategyProperties.METHOD_CONTRACT, StrategyProperties.LOOP_INVARIANT,
                            StrategyProperties.QUERY_ON, StrategyProperties.SPLITTING_NORMAL);
                        // Check for anonymized values in the side proof goals
                        assert !info.getProof().closed();
                        for (Goal goal : info.getProof().openGoals()) {
                            JTerm operatorTerm = SymbolicExecutionSideProofUtil
                                    .extractOperatorTerm(goal, newPredicate);
                            assert operatorTerm != null;
                            for (int i = 0; i < operatorTerm.arity(); i++) {
                                JTerm valueTerm = SymbolicExecutionUtil.replaceSkolemConstants(
                                    goal.sequent(), operatorTerm.sub(i), services);
                                if (valueTerm.arity() >= 1) {
                                    JTerm heap = valueTerm.sub(0);
                                    if (heap.containsLabel(
                                        ParameterlessTermLabel.ANON_HEAP_LABEL)) {
                                        listToFill.add(resultLocations.get(i));
                                    }
                                }
                            }
                        }
                    }
                } finally {
                    SymbolicExecutionSideProofUtil.disposeOrStore("Analyze Heap Assignment", info);
                }
            }
        } else {
            throw new IllegalStateException("Can not analyze update '" + term + "'.");
        }
    }

    /**
     * Adds the found alias consisting of first and second {@link ReferencePrefix} to the alias
     * {@link Map}. If required, all participating entries in the {@link Map} are updated to ensure
     * consistency.
     *
     * @param services The {@link Services} to use.
     * @param first The first alias.
     * @param second The second alias.
     * @param aliases The alias {@link Map} to update.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     */
    protected void updateAliases(Services services, Location first, Location second,
            Map<Location, SortedSet<Location>> aliases, ReferencePrefix thisReference) {
        // Try to get Set for key
        SortedSet<Location> firstValues = aliases.get(first);
        SortedSet<Location> secondValues = aliases.get(second);
        SortedSet<Location> values;
        if (firstValues == null && secondValues == null) {
            values = createSortedSet();
            aliases.put(first, values);
            aliases.put(second, values);
        } else if (firstValues != null && secondValues == null) {
            values = firstValues;
            aliases.put(second, values);
        } else if (firstValues == null && secondValues != null) {
            values = secondValues;
            aliases.put(first, values);
        } else if (firstValues != null && secondValues != null) { // both are not null
            values = firstValues;
            for (Location existingLocation : secondValues) {
                aliases.put(existingLocation, values);
            }
            values.addAll(secondValues);
        } else {
            // Can not happen!
            throw new IllegalStateException("Reached a state which should never happen.");
        }
        values.add(first);
        values.add(second);
    }

    /**
     * Creates a {@link SortedSet} which ensures that the elements are sorted.
     *
     * @return The new created {@link SortedSet}.
     */
    protected SortedSet<Location> createSortedSet() {
        return new TreeSet<>(new Comparator<>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(Location o1, Location o2) {
                int o1DotCount = o1.getDepth();
                int o2DotCount = o2.getDepth();
                if (o1DotCount < o2DotCount) {
                    return 1;
                } else if (o1DotCount > o2DotCount) {
                    return -1;
                } else {
                    return o1.toString().compareTo(o2.toString());
                }
            }
        }); // Order is important for normalization;
    }

    /**
     * Returns the representative alias for the given {@link ReferencePrefix}.
     *
     * @param services The {@link Services} to use.
     * @param referencePrefix The {@link ReferencePrefix}.
     * @param info The {@link SequentInfo} with the aliases and so on.
     * @return The representative alias.
     */
    protected Location normalizeAlias(Services services, ReferencePrefix referencePrefix,
            SequentInfo info) {
        Location location = toLocation(services, referencePrefix, info.getExecutionContext(),
            info.getThisReference());
        return normalizeAlias(services, location, info);
    }

    /**
     * Returns the representative alias for the given {@link Location}.
     *
     * @param services The {@link Services} to use.
     * @param location The {@link Location}.
     * @param info The {@link SequentInfo} with the aliases and so on.
     * @return The representative alias.
     */
    protected Location normalizeAlias(Services services, Location location, SequentInfo info) {
        ImmutableList<Access> normalizedAccesses = ImmutableSLList.nil();
        for (Access access : location.getAccesses()) {
            if (access.isArrayIndex()) {
                access = normalizeArrayIndex(access, info);
            }
            normalizedAccesses = normalizedAccesses.append(access);
            Location oldLocation = new Location(normalizedAccesses);
            Location newLocation = computeRepresentativeAlias(oldLocation, info.getAliases());
            if (!oldLocation.equals(newLocation)) {
                normalizedAccesses = normalizeAlias(services, newLocation, info).getAccesses();
            }
        }
        return new Location(normalizedAccesses);
    }

    /**
     * Normalizes the given array index.
     *
     * @param access The {@link Access} representing an array index.
     * @param info The {@link SequentInfo} with the aliases and so on.
     * @return The normalized array access.
     */
    protected Access normalizeArrayIndex(Access access, SequentInfo info) {
        ImmutableArray<JTerm> oldTerms = access.getDimensionExpressions();
        JTerm[] newTerms = new JTerm[oldTerms.size()];
        for (int i = 0; i < newTerms.length; i++) {
            JTerm oldTerm = oldTerms.get(i);
            if (oldTerm.op() instanceof ProgramVariable) {
                JTerm value = info.getLocalValues().get((ProgramVariable) oldTerm.op());
                if (value != null) {
                    oldTerm = value;
                }
            }
            newTerms[i] = oldTerm;
        }
        return new Access(new ImmutableArray<>(newTerms));
    }

    /**
     * Computes the representative alias of the given {@link Location}.
     *
     * @param location The given {@link Location}.
     * @param aliases The available aliases.
     * @return The representative alias.
     */
    protected Location computeRepresentativeAlias(Location location,
            Map<Location, SortedSet<Location>> aliases) {
        Set<Location> alternatives = aliases.get(location);
        if (alternatives != null) {
            return alternatives.iterator().next(); // Return first alternative
        } else {
            return location;
        }
    }

    /**
     * Computes the {@link ReferencePrefix} of the given {@link SourceElement}.
     *
     * @param sourceElement The {@link SourceElement} to work with.
     * @return The {@link ReferencePrefix} or {@code null} if the {@link SourceElement} can't be
     *         represented as {@link ReferencePrefix}.
     */
    protected ReferencePrefix toReferencePrefix(SourceElement sourceElement) {
        if (sourceElement instanceof PassiveExpression) {
            if (sourceElement.getChildCount() != 1) {
                throw new IllegalStateException(
                    "PassiveExpression '" + sourceElement + "' has not exactly one child.");
            }
            sourceElement = ((PassiveExpression) sourceElement).getChildAt(0);
        }
        if (sourceElement instanceof FieldReference) {
            return (FieldReference) sourceElement;
        } else if (sourceElement instanceof ProgramVariable) {
            return (ProgramVariable) sourceElement;
        } else if (sourceElement instanceof ArrayReference) {
            return (ArrayReference) sourceElement;
        } else {
            return null;
        }
    }

    /**
     * Checks if the given {@link SourceElement} is directly or indirectly contained (aliased) in
     * the {@link Set} of relevant locations. If it is contained, the element will be removed.
     *
     * @param services The {@link Services} to use.
     * @param sourceElement The {@link SourceElement} to check.
     * @param relevantLocations The {@link Set} with locations of interest.
     * @param info The {@link SequentInfo} with the aliases and so on.
     * @return {@code true} is relevant and was removed, {@code false} is not relevant and nothing
     *         has changed.
     */
    protected boolean removeRelevant(Services services, ReferencePrefix sourceElement,
            Set<Location> relevantLocations, SequentInfo info) {
        Location normalized = normalizeAlias(services, sourceElement, info);
        return performRemoveRelevant(services, normalized, relevantLocations, info);
    }

    /**
     * Checks if the given {@link Location} is directly or indirectly contained (aliased) in the
     * {@link Set} of relevant locations. If it is contained, the element will be removed.
     *
     * @param services The {@link Services} to use.
     * @param location The {@link Location} to check.
     * @param relevantLocations The {@link Set} with locations of interest.
     * @param info The {@link SequentInfo} with the aliases and so on.
     * @return {@code true} is relevant and was removed, {@code false} is not relevant and nothing
     *         has changed.
     */
    protected boolean removeRelevant(Services services, Location location,
            Set<Location> relevantLocations, SequentInfo info) {
        Location normalized = normalizeAlias(services, location, info);
        return performRemoveRelevant(services, normalized, relevantLocations, info);
    }

    /**
     * Checks if the given {@link Location} is directly or indirectly contained (aliased) in the
     * {@link Set} of relevant locations. If it is contained, the element will be removed.
     *
     * @param services The {@link Services} to use.
     * @param normalized The {@link Location} to check.
     * @param relevantLocations The {@link Set} with locations of interest.
     * @param info The {@link SequentInfo} with the aliases and so on.
     * @return {@code true} is relevant and was removed, {@code false} is not relevant and nothing
     *         has changed.
     */
    protected boolean performRemoveRelevant(Services services, Location normalized,
            Set<Location> relevantLocations, SequentInfo info) {
        boolean relevant = false;
        Iterator<Location> iterator = relevantLocations.iterator();
        while (!relevant && iterator.hasNext()) {
            Location next = iterator.next();
            Location nextNormalized = normalizeAlias(services, next, info);
            if (normalized.equals(nextNormalized)) {
                iterator.remove();
                relevant = true;
            }
        }
        return relevant;
    }

    /**
     * Converts the given {@link ReferencePrefix} into a {@link Location}.
     *
     * @param services The {@link Services} to use.
     * @param prefix The {@link ReferencePrefix} to convert.
     * @param ec The current {@link ExecutionContext}.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     * @return The {@link Location} representing the given {@link ReferencePrefix}.
     */
    protected Location toLocation(Services services, ReferencePrefix prefix, ExecutionContext ec,
            ReferencePrefix thisReference) {
        ImmutableList<Access> accesses =
            toLocationRecursive(services, prefix, ec, thisReference, ImmutableSLList.nil());
        return new Location(accesses);
    }

    /**
     * Utility method used by {@link #toLocation} to
     * recursively extract the {@link Access} instances.
     *
     * @param services The {@link Services} to use.
     * @param prefix The {@link ReferencePrefix} to work with.
     * @param ec The current {@link ExecutionContext}.
     * @param thisReference The {@link ReferencePrefix} which is represented by {@code this}
     *        ({@link ThisReference}).
     * @param children The already known child {@link Access}s.
     * @return An {@link ImmutableList} containing all {@link Access}s of the
     *         {@link ReferencePrefix} in the order of access.
     */
    protected ImmutableList<Access> toLocationRecursive(Services services, ReferencePrefix prefix,
            ExecutionContext ec, ReferencePrefix thisReference, ImmutableList<Access> children) {
        if (prefix instanceof ProgramVariable) {
            return children.prepend(new Access((ProgramVariable) prefix));
        } else if (prefix instanceof FieldReference fr) {
            ReferencePrefix parent = fr.getReferencePrefix();
            children = children.prepend(new Access(fr.getProgramVariable()));
            if (parent != null) {
                return toLocationRecursive(services, parent, ec, thisReference, children);
            } else {
                return children;
            }
        } else if (prefix instanceof ThisReference) {
            if (thisReference instanceof ProgramVariable) {
                return children.prepend(new Access((ProgramVariable) thisReference));
            } else if (thisReference instanceof FieldReference) {
                return toLocationRecursive(services, thisReference, ec, thisReference, children);
            } else {
                throw new IllegalStateException(
                    "Unsupported this reference '" + thisReference + "'.");
            }
        } else if (prefix instanceof ArrayReference ar) {
            children =
                children.prepend(new Access(toTerm(services, ar.getDimensionExpressions(), ec)));
            return toLocationRecursive(services, ar.getReferencePrefix(), ec, thisReference,
                children);
        } else {
            throw new IllegalStateException("Unsupported prefix '" + prefix + "'.");
        }
    }

    /**
     * Converts the given {@link Expression}s into {@link JTerm}s.
     *
     * @param services The {@link Services} to use.
     * @param expressions The {@link Expression}s to convert.
     * @param ec The current {@link ExecutionContext}.
     * @return The created {@link JTerm}s.
     */
    public static ImmutableArray<JTerm> toTerm(Services services,
            ImmutableArray<Expression> expressions, ExecutionContext ec) {
        JTerm[] terms = new JTerm[expressions.size()];
        int i = 0;
        for (Expression expression : expressions) {
            terms[i] = toTerm(services, expression, ec);
            i++;
        }
        return new ImmutableArray<>(terms);
    }

    /**
     * Converts the given {@link Expression} into a {@link JTerm}.
     *
     * @param services The {@link Services} to use.
     * @param expression The {@link Expression} to convert.
     * @param ec The current {@link ExecutionContext}.
     * @return The created {@link JTerm}.
     */
    public static JTerm toTerm(Services services, Expression expression, ExecutionContext ec) {
        return services.getTypeConverter().convertToLogicElement(expression, ec);
    }

    /**
     * Converts the given {@link JTerm} into a {@link Location}.
     *
     * @param services The {@link Services} to use.
     * @param term The {@link JTerm} to convert.
     * @return The {@link Location} or {@code null} if the {@link JTerm} could not be represented as
     *         {@link Location}.
     */
    public static Location toLocation(Services services, JTerm term) {
        if (term.op() instanceof ProgramVariable) {
            return new Location(new Access((ProgramVariable) term.op()));
        } else if (SymbolicExecutionUtil.isNullSort(term.sort(), services)) {
            return null;
        } else {
            HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
            if (term.op() == heapLDT.getSelect(term.sort(), services)) {
                Location prefix = toLocation(services, term.sub(1));
                JTerm arrayIndex =
                    SymbolicExecutionUtil.getArrayIndex(services, heapLDT, term.sub(2));
                if (arrayIndex != null) {
                    return prefix.append(new Access(arrayIndex));
                } else {
                    Location variable = toLocation(services, term.sub(2));
                    return prefix != null ? prefix.append(variable) : variable;
                }
            } else {
                String name = term.op().name().toString();
                int index = name.indexOf("::");
                if (index >= 0) {
                    String fullTypeName = name.substring(0, index);
                    String fieldName = name.substring(index + 3);
                    ProgramVariable pv =
                        services.getJavaInfo().getAttribute(fullTypeName + "::" + fieldName);
                    assert term.op() == services.getTypeConverter().getHeapLDT()
                            .getFieldSymbolForPV((LocationVariable) pv, services);
                    return new Location(new Access(pv));
                } else {
                    return null;
                }
            }
        }
    }

    /**
     * Returns the first found alternative which is still valid.
     *
     * @param oldAlternatives The old alternatives.
     * @param newAlternatives The new alternatives.
     * @return The found alternative or {@code null} if not available.
     */
    protected Location findNewAlternative(final SortedSet<Location> oldAlternatives,
            final SortedSet<Location> newAlternatives) {
        return CollectionUtil.search(oldAlternatives,
            element -> !newAlternatives.contains(element));
    }

    /**
     * Computes the length of a common prefix.
     *
     * @param candidates The possible candidates.
     * @param toCheck The {@link ImmutableList} to check.
     * @return The common prefix length which is {@code 0} if no elements are common.
     */
    public static <T> int computeFirstCommonPrefixLength(ImmutableList<ImmutableList<T>> candidates,
            ImmutableList<T> toCheck) {
        int commonLength = 0;
        Iterator<ImmutableList<T>> iter = candidates.iterator();
        while (commonLength < 1 && iter.hasNext()) {
            ImmutableList<T> next = iter.next();
            if (startsWith(toCheck, next)) {
                commonLength = next.size();
            }
        }
        return commonLength;
    }

    /**
     * Checks if the given {@link ImmutableList} starts with the given prefix.
     *
     * @param list The {@link List} to check.
     * @param prefix The prefix to check.
     * @return {@code true} the first elements in the {@link ImmutableList} are the prefix,
     *         {@code false} if the first elements are not equal to the prefix.
     */
    public static <T> boolean startsWith(ImmutableList<T> list, ImmutableList<T> prefix) {
        if (list.size() >= prefix.size()) {
            Iterator<T> listIter = list.iterator();
            Iterator<T> prefixIter = prefix.iterator();
            boolean same = true;
            while (same && prefixIter.hasNext()) {
                T listNext = listIter.next();
                T prefixNext = prefixIter.next();
                if (!Objects.equals(listNext, prefixNext)) {
                    same = false;
                }
            }
            return same;
        } else {
            return false;
        }
    }
}
