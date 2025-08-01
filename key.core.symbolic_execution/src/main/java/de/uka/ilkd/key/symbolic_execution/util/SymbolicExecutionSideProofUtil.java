/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.symbolic_execution.util;

import java.util.*;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.java.abstraction.KeYJavaType;
import de.uka.ilkd.key.ldt.HeapLDT;
import de.uka.ilkd.key.logic.*;
import de.uka.ilkd.key.logic.label.TermLabel;
import de.uka.ilkd.key.logic.label.TermLabelManager.TermLabelConfiguration;
import de.uka.ilkd.key.logic.op.*;
import de.uka.ilkd.key.proof.Goal;
import de.uka.ilkd.key.proof.Node;
import de.uka.ilkd.key.proof.Proof;
import de.uka.ilkd.key.proof.calculus.JavaDLSequentKit;
import de.uka.ilkd.key.proof.init.InitConfig;
import de.uka.ilkd.key.proof.init.JavaProfile;
import de.uka.ilkd.key.proof.init.Profile;
import de.uka.ilkd.key.proof.init.ProofInputException;
import de.uka.ilkd.key.proof.mgt.AxiomJustification;
import de.uka.ilkd.key.proof.mgt.ProofEnvironment;
import de.uka.ilkd.key.proof.mgt.RuleJustification;
import de.uka.ilkd.key.proof.mgt.RuleJustificationInfo;
import de.uka.ilkd.key.rule.BuiltInRule;
import de.uka.ilkd.key.rule.OneStepSimplifier;
import de.uka.ilkd.key.rule.Taclet;
import de.uka.ilkd.key.rule.tacletbuilder.TacletBuilder;
import de.uka.ilkd.key.settings.ProofSettings;
import de.uka.ilkd.key.strategy.StrategyProperties;
import de.uka.ilkd.key.symbolic_execution.profile.SimplifyTermProfile;
import de.uka.ilkd.key.symbolic_execution.profile.SymbolicExecutionJavaProfile;
import de.uka.ilkd.key.symbolic_execution.rule.ResultsAndCondition;
import de.uka.ilkd.key.util.ProofStarter;
import de.uka.ilkd.key.util.SideProofUtil;

import org.key_project.logic.Choice;
import org.key_project.logic.Name;
import org.key_project.logic.Namespace;
import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.prover.engine.impl.ApplyStrategyInfo;
import org.key_project.prover.sequent.Sequent;
import org.key_project.prover.sequent.SequentFormula;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSet;
import org.key_project.util.collection.Pair;
import org.key_project.util.java.CollectionUtil;

import org.jspecify.annotations.NonNull;

/**
 * Provides utility methods for side proofs.
 *
 * @author Martin Hentschel
 */
public final class SymbolicExecutionSideProofUtil {
    /**
     * Forbid instances.
     */
    private SymbolicExecutionSideProofUtil() {
    }

    /**
     * Computes a general {@link Sequent} to prove in a side proof which contains all
     * {@link SequentFormula} of the original {@link Sequent} except the given current
     * {@link SequentFormula} and those which contains modalities and queries.
     *
     * @param goalSequent The original {@link Sequent} of the current {@link Goal}.
     * @param currentSF The {@link SequentFormula} to ignore.
     * @return The general initial {@link Sequent}.
     */
    public static Sequent computeGeneralSequentToProve(Sequent goalSequent,
            SequentFormula currentSF) {
        Sequent sequentToProve = JavaDLSequentKit.getInstance().getEmptySequent();
        for (SequentFormula sf : goalSequent.antecedent()) {
            if (sf != currentSF) {
                if (!containsModalityOrQuery(sf)) {
                    sequentToProve = sequentToProve.addFormula(sf, true, false).sequent();
                }
            }
        }
        for (SequentFormula sf : goalSequent.succedent()) {
            if (sf != currentSF) {
                if (!containsModalityOrQuery(sf)) {
                    sequentToProve =
                        sequentToProve.addFormula(sf, false, false).sequent();
                }
            }
        }
        return sequentToProve;
    }

    /**
     * <p>
     * Starts the side proof and extracts the result {@link JTerm}.
     * </p>
     * <p>
     * New used names are automatically added to the {@link Namespace} of the {@link Services}.
     * </p>
     *
     * @param services The {@link Services} to use.
     * @param proof The {@link Proof} from on which the side proof si performed.
     * @param sideProofEnvironment The given {@link ProofEnvironment} of the side proof.
     * @param sequentToProve The {@link Sequent} to prove in a side proof.
     * @param label The {@link TermLabel} which is used to compute the result.
     * @param description The side proof description.
     * @param splittingOption The splitting options to use.
     * @param addNamesToServices {@code true} defines that used names in result and conditions are
     *        added to the namespace of the given {@link Services}, {@code false} means that names
     *        are not added.
     * @return The found result {@link JTerm} and the conditions.
     * @throws ProofInputException Occurred Exception.
     */
    public static List<Pair<JTerm, Node>> computeResults(Services services, Proof proof,
            ProofEnvironment sideProofEnvironment, Sequent sequentToProve, TermLabel label,
            String description, String methodTreatment, String loopTreatment, String queryTreatment,
            String splittingOption, boolean addNamesToServices) throws ProofInputException {
        // Execute side proof
        ApplyStrategyInfo<Proof, Goal> info =
            startSideProof(proof, sideProofEnvironment, sequentToProve,
                methodTreatment, loopTreatment, queryTreatment, splittingOption);
        try {
            // Extract results and conditions from side proof
            List<Pair<JTerm, Node>> conditionsAndResultsMap = new LinkedList<>();
            for (Goal resultGoal : info.getProof().openGoals()) {
                if (SymbolicExecutionUtil.hasApplicableRules(resultGoal)) {
                    throw new IllegalStateException("Not all applicable rules are applied.");
                }
                Sequent sequent = resultGoal.sequent();
                List<JTerm> results = new LinkedList<>();
                for (SequentFormula sf : sequent.antecedent()) {
                    final JTerm result = (JTerm) sf.formula();
                    if (result.containsLabel(label)) {
                        results.add(services.getTermBuilder().not(result));
                    }
                }
                for (SequentFormula sf : sequent.succedent()) {
                    final JTerm result = (JTerm) sf.formula();
                    if (result.containsLabel(label)) {
                        results.add(result);
                    }
                }
                final JTerm result;
                if (results.isEmpty()) {
                    result = services.getTermBuilder().tt();
                } else {
                    result = services.getTermBuilder().or(results);
                }
                conditionsAndResultsMap.add(new Pair<>(result, resultGoal.node()));
            }
            return conditionsAndResultsMap;
        } finally {
            disposeOrStore(description, info);
        }
    }

    /**
     * <p>
     * Starts the side proof and extracts the result {@link JTerm} and conditions.
     * </p>
     * <p>
     * New used names are automatically added to the {@link Namespace} of the {@link Services}.
     * </p>
     *
     * @param services The {@link Services} to use.
     * @param proof The {@link Proof} from on which the side proof si performed.
     * @param sideProofEnvironment The given {@link ProofEnvironment} of the side proof.
     * @param sequentToProve The {@link Sequent} to prove in a side proof.
     * @param operator The {@link Operator} which is used to compute the result.
     * @param description The side proof description.
     * @param splittingOption The splitting options to use.
     * @param addNamesToServices {@code true} defines that used names in result and conditions are
     *        added to the namespace of the given {@link Services}, {@code false} means that names
     *        are not added.
     * @return The found result {@link JTerm} and the conditions.
     * @throws ProofInputException Occurred Exception.
     */
    public static List<ResultsAndCondition> computeResultsAndConditions(
            Services services,
            Proof proof, ProofEnvironment sideProofEnvironment, Sequent sequentToProve,
            Operator operator, String description, String methodTreatment, String loopTreatment,
            String queryTreatment, String splittingOption, boolean addNamesToServices)
            throws ProofInputException {
        // Execute side proof
        ApplyStrategyInfo<Proof, Goal> info =
            startSideProof(proof, sideProofEnvironment, sequentToProve,
                methodTreatment, loopTreatment, queryTreatment, splittingOption);
        try {
            // Extract relevant things
            Set<org.key_project.logic.op.Operator> relevantThingsInSequentToProve =
                extractRelevantThings(info.getProof().getServices(), sequentToProve);
            // Extract results and conditions from side proof
            List<ResultsAndCondition> conditionsAndResultsMap = new LinkedList<>();
            for (Goal resultGoal : info.getProof().openGoals()) {
                if (SymbolicExecutionUtil.hasApplicableRules(resultGoal)) {
                    throw new IllegalStateException("Not all applicable rules are applied.");
                }
                Sequent sequent = resultGoal.sequent();
                boolean newPredicateIsSequentFormula = isOperatorASequentFormula(sequent, operator);
                Set<JTerm> resultConditions = new LinkedHashSet<>();
                JTerm result = null;
                for (SequentFormula sf : sequent.antecedent()) {
                    final JTerm formula = (JTerm) sf.formula();
                    if (newPredicateIsSequentFormula) {
                        if (JavaDLOperatorUtil.opEquals(formula.op(), operator)) {
                            throw new IllegalStateException(
                                "Result predicate found in antecedent.");
                        } else {
                            JTerm constructedResult =
                                constructResultIfContained(services, sf, operator);
                            if (constructedResult != null) {
                                throw new IllegalStateException(
                                    "Result predicate found in antecedent.");
                            }
                        }
                    }
                    if (!isIrrelevantCondition(services, sequentToProve,
                        relevantThingsInSequentToProve, sf)) {
                        if (resultConditions.add(formula) && addNamesToServices) {
                            addNewNamesToNamespace(services, formula);
                        }
                    }
                }
                for (SequentFormula sf : sequent.succedent()) {
                    final JTerm formula = (JTerm) sf.formula();
                    if (newPredicateIsSequentFormula) {
                        if (JavaDLOperatorUtil.opEquals(formula.op(), operator)) {
                            if (result != null) {
                                throw new IllegalStateException(
                                    "Result predicate found multiple times in succedent.");
                            }
                            result = formula.sub(0);
                        }
                    } else {
                        JTerm constructedResult =
                            constructResultIfContained(services, sf, operator);
                        if (constructedResult != null) {
                            if (result != null) {
                                throw new IllegalStateException(
                                    "Result predicate found multiple times in succedent.");
                            }
                            result = constructedResult;
                        }
                    }
                    if (result == null) {
                        if (!isIrrelevantCondition(services, sequentToProve,
                            relevantThingsInSequentToProve, sf)) {
                            if (resultConditions.add(services.getTermBuilder().not(formula))
                                    && addNamesToServices) {
                                addNewNamesToNamespace(services, formula);
                            }
                        }
                    }
                }
                if (result == null) {
                    result = services.getTermBuilder().ff();
                }
                conditionsAndResultsMap
                        .add(new ResultsAndCondition(result, resultConditions, resultGoal.node()));
            }
            return conditionsAndResultsMap;
        } finally {
            disposeOrStore(description, info);
        }
    }

    private static JTerm constructResultIfContained(Services services,
            SequentFormula sf,
            Operator operator) {
        return constructResultIfContained(services, (JTerm) sf.formula(), operator);
    }

    private static JTerm constructResultIfContained(Services services, JTerm term,
            Operator operator) {
        if (JavaDLOperatorUtil.opEquals(term.op(), operator)) {
            return term.sub(0);
        } else {
            JTerm result = null;
            int i = 0;
            while (result == null && i < term.arity()) {
                result = constructResultIfContained(services, term.sub(i), operator);
                i++;
            }
            if (result != null) {
                List<JTerm> newSubs = new LinkedList<>();
                for (int j = 0; j < term.arity(); j++) {
                    if (j == i - 1) {
                        newSubs.add(result);
                    } else {
                        newSubs.add(term.sub(j));
                    }
                }
                result = services.getTermFactory().createTerm(term.op(),
                    new ImmutableArray<>(newSubs), term.boundVars(),
                    term.getLabels());
            }
            return result;
        }
    }

    private static boolean isOperatorASequentFormula(Sequent sequent, final Operator operator) {
        return CollectionUtil.search(sequent,
            element -> JavaDLOperatorUtil.opEquals(element.formula().op(),
                operator)) != null;
    }

    /**
     * Makes sure that all used {@link Name}s in the given {@link JTerm} are registered in the
     * {@link Namespace}s of the given {@link Services}.
     *
     * @param services The {@link Services} to use.
     * @param term The {@link JTerm} to check its {@link Name}s.
     */
    public static void addNewNamesToNamespace(Services services, JTerm term) {
        final Namespace<Function> functions = services.getNamespaces().functions();
        final Namespace<IProgramVariable> progVars = services.getNamespaces().programVariables();
        // LogicVariables are always local bound
        term.execPreOrder((DefaultVisitor) visited -> {
            if (visited.op() instanceof Function function) {
                functions.add(function);
            } else if (visited.op() instanceof IProgramVariable progVar) {
                progVars.add(progVar);
            }
        });
    }

    /**
     * Checks if the given {@link SequentFormula} contains a modality or query.
     *
     * @param sf The {@link SequentFormula} to check.
     * @return {@code true} contains at least one modality or query, {@code false} contains no
     *         modalities and no queries.
     */
    public static boolean containsModalityOrQuery(
            SequentFormula sf) {
        return containsModalityOrQuery(sf.formula());
    }

    /**
     * Checks if the given {@link JTerm} contains a modality or query.
     *
     * @param term The {@link JTerm} to check.
     * @return {@code true} contains at least one modality or query, {@code false} contains no
     *         modalities and no queries.
     */
    public static boolean containsModalityOrQuery(Term term) {
        ContainsModalityOrQueryVisitor visitor = new ContainsModalityOrQueryVisitor();
        term.execPostOrder(visitor);
        return visitor.isContainsModalityOrQuery();
    }

    /**
     * Utility method used by {@link #containsModalityOrQuery(Term)}
     *
     * @author Martin Hentschel
     */
    protected static class ContainsModalityOrQueryVisitor implements DefaultVisitor {
        /**
         * The result.
         */
        boolean containsModalityOrQuery = false;

        /**
         * {@inheritDoc}
         */
        @Override
        public void visit(Term visited) {
            if (visited.op() instanceof JModality) {
                containsModalityOrQuery = true;
            } else if (visited.op() instanceof IProgramMethod) {
                containsModalityOrQuery = true;
            }
        }

        /**
         * Returns the result.
         *
         * @return {@code true} contains at least one modality or query, {@code false} contains no
         *         modalities and no queries.
         */
        public boolean isContainsModalityOrQuery() {
            return containsModalityOrQuery;
        }
    }

    /**
     * Extracts all {@link Operator}s from the given {@link Sequent} which represents relevant
     * things.
     *
     * @param services The {@link Services} to use.
     * @param sequentToProve The {@link Sequent} to extract relevant things from.
     * @return The found relevant things.
     */
    public static Set<org.key_project.logic.op.Operator> extractRelevantThings(
            final Services services,
            Sequent sequentToProve) {
        final Set<org.key_project.logic.op.Operator> result = new HashSet<>();
        for (SequentFormula sf : sequentToProve) {
            sf.formula().execPreOrder((DefaultVisitor) visited -> {
                if (isRelevantThing(services, visited)) {
                    result.add(visited.op());
                }
            });
        }
        return result;
    }

    /**
     * Checks if the given {@link JTerm} describes a relevant thing. Relevant things are:
     * <ul>
     * <li>IProgramVariable</li>
     * <li>Functions of type Heap</li>
     * <li>Functions of a Java type</li>
     * </ul>
     *
     * @param services The {@link Services} to use.
     * @param term The {@link JTerm} to check.
     * @return {@code true} is relevant thing, {@code false} is not relevant.
     */
    private static boolean isRelevantThing(Services services, Term term) {
        if (term.op() instanceof IProgramVariable) {
            return true;
        } else if (term.op() instanceof Function) {
            HeapLDT heapLDT = services.getTypeConverter().getHeapLDT();
            if (SymbolicExecutionUtil.isHeap(term.op(), heapLDT)) {
                return true;
            } else {
                KeYJavaType kjt = services.getJavaInfo().getKeYJavaType(term.sort());
                return kjt != null;
            }
        } else {
            return false;
        }
    }

    /**
     * Checks if the given {@link SequentFormula} is a relevant condition.
     *
     * @param services The {@link Services} to use.
     * @param initialSequent The initial {@link Sequent} of the side proof.
     * @param relevantThingsInSequentToProve The relevant things found in the initial
     *        {@link Sequent}.
     * @param sf The {@link SequentFormula} to check.
     * @return {@code true} {@link SequentFormula} is relevant condition, {@code false}
     *         {@link SequentFormula} is not a relevant condition.
     */
    public static boolean isIrrelevantCondition(Services services, Sequent initialSequent,
            Set<org.key_project.logic.op.Operator> relevantThingsInSequentToProve,
            SequentFormula sf) {
        return initialSequent.antecedent().contains(sf) || initialSequent.succedent().contains(sf)
                || containsModalityOrQuery(sf) // isInOrOfAntecedent(initialSequent, sf) ||
                || containsIrrelevantThings(services, sf, relevantThingsInSequentToProve);
    }

    // public static boolean isInOrOfAntecedent(Sequent initialSequent, SequentFormula sf) {
    // Term term = sf.formula();
    // boolean result = false;
    // Iterator<SequentFormula> iter = initialSequent.antecedent().iterator();
    // while (!result && iter.hasNext()) {
    // SequentFormula next = iter.next();
    // if (isInOr(next.formula(), term)) {
    // result = true;
    // }
    // }
    // return result;
    // }
    //
    // public static boolean isInOr(Term term, Term toCheck) {
    // if (term.op() == Junctor.OR) {
    // boolean result = false;
    // Iterator<Term> iter = term.subs().iterator();
    // while (!result && iter.hasNext()) {
    // result = isInOr(iter.next(), toCheck);
    // }
    // return result;
    // }
    // else {
    // return term == toCheck;
    // }
    // }

    /**
     * Checks if the given {@link SequentFormula} contains irrelevant things (things which are not
     * contained in the relevantThingsToProve and are no Java types)
     *
     * @param services The {@link Services} to use.
     * @param sf The {@link SequentFormula} to check.
     * @param relevantThings The relevant things.
     * @return {@code true} The {@link SequentFormula} contains irrelevant things, {@code false} the
     *         {@link SequentFormula} contains no irrelevant things.
     */
    public static boolean containsIrrelevantThings(Services services,
            SequentFormula sf,
            Set<org.key_project.logic.op.Operator> relevantThings) {
        ContainsIrrelevantThingsVisitor visitor =
            new ContainsIrrelevantThingsVisitor(services, relevantThings);
        sf.formula().execPostOrder(visitor);
        return visitor.isContainsIrrelevantThings();
    }

    /**
     * Utility class used by
     * {@link #containsIrrelevantThings(Services, SequentFormula, Set)}.
     *
     * @author Martin Hentschel
     */
    protected static class ContainsIrrelevantThingsVisitor implements DefaultVisitor {
        /**
         * The {@link Services} to use.
         */
        private final Services services;

        /**
         * The relevant things.
         */
        private final Set<org.key_project.logic.op.Operator> relevantThings;

        /**
         * The result.
         */
        boolean containsIrrelevantThings = false;

        /**
         * Constructor.
         *
         * @param services The {@link Services} to use.
         * @param relevantThings The relevant things.
         */
        public ContainsIrrelevantThingsVisitor(Services services,
                Set<org.key_project.logic.op.Operator> relevantThings) {
            this.services = services;
            this.relevantThings = relevantThings;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void visit(Term visited) {
            if (isRelevantThing(services, visited)) {
                if (!SymbolicExecutionUtil.isSelect(services, visited)
                        && !SymbolicExecutionUtil.isBoolean(services, visited.op())
                        && !SymbolicExecutionUtil.isNumber(visited.op())) {
                    if (!relevantThings.contains(visited.op())) {
                        containsIrrelevantThings = true;
                    }
                }
            }
        }

        /**
         * Returns the result.
         *
         * @return The {@link SequentFormula} contains irrelevant things, {@code false} the
         *         {@link SequentFormula} contains no irrelevant things.
         */
        public boolean isContainsIrrelevantThings() {
            return containsIrrelevantThings;
        }
    }

    /**
     * Starts a site proof for the given {@link Sequent}.
     *
     * @param proof The parent {@link Proof} of the site proof to do.
     * @param sideProofEnvironment The given {@link ProofEnvironment} of the side proof.
     * @param sequentToProve The {@link Sequent} to prove.
     * @return The proof result represented as {@link ApplyStrategyInfo} instance.
     * @throws ProofInputException Occurred Exception
     */
    public static ApplyStrategyInfo<@NonNull Proof, Goal> startSideProof(Proof proof,
            ProofEnvironment sideProofEnvironment, Sequent sequentToProve)
            throws ProofInputException {
        return startSideProof(proof, sideProofEnvironment, sequentToProve,
            StrategyProperties.METHOD_NONE, StrategyProperties.LOOP_NONE,
            StrategyProperties.QUERY_OFF, StrategyProperties.SPLITTING_OFF);
    }

    /**
     * Starts a site proof for the given {@link Sequent}.
     *
     * @param proof The parent {@link Proof} of the site proof to do.
     * @param sideProofEnvironment The given {@link ProofEnvironment} of the side proof.
     * @param sequentToProve The {@link Sequent} to prove.
     * @return The proof result represented as {@link ApplyStrategyInfo} instance.
     * @throws ProofInputException Occurred Exception
     */
    public static ApplyStrategyInfo<@NonNull Proof, Goal> startSideProof(Proof proof,
            ProofEnvironment sideProofEnvironment, Sequent sequentToProve, String methodTreatment,
            String loopTreatment, String queryTreatment, String splittingOption)
            throws ProofInputException {
        ProofStarter starter = createSideProof(sideProofEnvironment, sequentToProve, null);
        return startSideProof(proof, starter, methodTreatment, loopTreatment, queryTreatment,
            splittingOption);
    }

    /**
     * Creates a new {@link ProofStarter} which contains a new site proof of the given
     * {@link Proof}.
     *
     * @param sideProofEnvironment The given {@link ProofEnvironment} of the side proof.
     * @param sequentToProve The {@link Sequent} to proof in a new site proof.
     * @param proofName An optional name for the newly created {@link Proof}.
     * @return The created {@link ProofStarter} with the site proof.
     * @throws ProofInputException Occurred Exception.
     */
    public static ProofStarter createSideProof(ProofEnvironment sideProofEnvironment,
            Sequent sequentToProve, String proofName) throws ProofInputException {
        return SideProofUtil.createSideProof(sideProofEnvironment, sequentToProve, proofName);
    }

    /**
     * Starts a site proof.
     *
     * @param proof The original {@link Proof}.
     * @param starter The {@link ProofStarter} with the site proof.
     * @param splittingOption The splitting option to use.
     * @return The site proof result.
     */
    public static ApplyStrategyInfo<Proof, Goal> startSideProof(Proof proof, ProofStarter starter,
            String methodTreatment, String loopTreatment, String queryTreatment,
            String splittingOption) {
        assert starter != null;
        starter.setMaxRuleApplications(10000);
        StrategyProperties sp = proof != null && !proof.isDisposed()
                ? proof.getSettings().getStrategySettings().getActiveStrategyProperties()
                : // Is a clone that can be modified
                new StrategyProperties();
        StrategyProperties.setDefaultStrategyProperties(sp, false, true, true, false, false, false);
        sp.setProperty(StrategyProperties.METHOD_OPTIONS_KEY, methodTreatment);
        sp.setProperty(StrategyProperties.LOOP_OPTIONS_KEY, loopTreatment);
        sp.setProperty(StrategyProperties.QUERY_OPTIONS_KEY, queryTreatment);
        sp.setProperty(StrategyProperties.SPLITTING_OPTIONS_KEY, splittingOption);
        sp.setProperty(StrategyProperties.QUANTIFIERS_OPTIONS_KEY,
            StrategyProperties.QUANTIFIERS_NON_SPLITTING);
        starter.setStrategyProperties(sp);
        // Execute proof in the current thread
        return (ApplyStrategyInfo<Proof, Goal>) starter.start();
    }

    /**
     * Extracts the value for the formula with the given {@link Operator} from the given
     * {@link Goal}.
     *
     * @param goal The {@link Goal} to search the {@link Operator} in.
     * @param operator The {@link Operator} for the formula which should be extracted.
     * @return The value of the formula with the given {@link Operator}.
     */
    public static JTerm extractOperatorValue(Goal goal, final Operator operator) {
        assert goal != null;
        return extractOperatorValue(goal.node(), operator);
    }

    /**
     * Extracts the value for the formula with the given {@link Operator} from the given
     * {@link Node}.
     *
     * @param node The {@link Node} to search the {@link Operator} in.
     * @param operator The {@link Operator} for the formula which should be extracted.
     * @return The value of the formula with the given {@link Operator}.
     */
    public static JTerm extractOperatorValue(Node node, final Operator operator) {
        JTerm operatorTerm = extractOperatorTerm(node, operator);
        return operatorTerm != null ? operatorTerm.sub(0) : null;
    }

    /**
     * Extracts the operator term for the formula with the given {@link Operator} from the site
     * proof result ({@link ApplyStrategyInfo}).
     *
     * @param info The site proof result.
     * @param operator The {@link Operator} for the formula which should be extracted.
     * @return The operator term of the formula with the given {@link Operator}.
     * @throws ProofInputException Occurred Exception.
     */
    public static JTerm extractOperatorTerm(ApplyStrategyInfo<Proof, Goal> info, Operator operator)
            throws ProofInputException {
        // Make sure that valid parameters are given
        assert info != null;
        if (info.getProof().openGoals().size() != 1) {
            throw new ProofInputException(
                "Assumption that return value extraction has one goal does not hold because "
                    + info.getProof().openGoals().size() + " goals are available.");
        }
        // Get node of open goal
        return extractOperatorTerm(info.getProof().openGoals().head(), operator);
    }

    /**
     * Extracts the operator term for the formula with the given {@link Operator} from the given
     * {@link Goal}.
     *
     * @param goal The {@link Goal} to search the {@link Operator} in.
     * @param operator The {@link Operator} for the formula which should be extracted.
     * @return The operator term of the formula with the given {@link Operator}.
     */
    public static JTerm extractOperatorTerm(Goal goal, final Operator operator) {
        assert goal != null;
        return extractOperatorTerm(goal.node(), operator);
    }

    /**
     * Extracts the operator term for the formula with the given {@link Operator} from the given
     * {@link Node}.
     *
     * @param node The {@link Node} to search the {@link Operator} in.
     * @param operator The {@link Operator} for the formula which should be extracted.
     * @return The operator term of the formula with the given {@link Operator}.
     */
    public static JTerm extractOperatorTerm(Node node, final Operator operator) {
        assert node != null;
        // Search formula with the given operator in sequent (or in some cases below the updates)
        SequentFormula sf =
            CollectionUtil.search(node.sequent(), element -> {
                JTerm term = (JTerm) element.formula();
                term = TermBuilder.goBelowUpdates(term);
                return Objects.equals(term.op(), operator);
            });
        if (sf != null) {
            JTerm term = (JTerm) sf.formula();
            term = TermBuilder.goBelowUpdates(term);
            return term;
        } else {
            return null;
        }
    }

    /**
     * Creates a copy of the {@link ProofEnvironment} of the given {@link Proof} which has his own
     * {@link OneStepSimplifier} instance. Such copies are required for instance during parallel
     * usage of site proofs because {@link OneStepSimplifier} has an internal state.
     *
     * @param source The {@link Proof} to copy its {@link ProofEnvironment}.
     * @return The created {@link ProofEnvironment} which is a copy of the environment of the given
     *         {@link Proof} but with its own {@link OneStepSimplifier} instance.
     */
    public static ProofEnvironment cloneProofEnvironmentWithOwnOneStepSimplifier(final Proof source,
            final boolean useSimplifyTermProfile) {
        assert source != null;
        assert !source.isDisposed();
        return cloneProofEnvironmentWithOwnOneStepSimplifier(source.getInitConfig(),
            useSimplifyTermProfile);
    }

    /**
     * Creates a copy of the {@link ProofEnvironment} of the given {@link Proof} which has his own
     * {@link OneStepSimplifier} instance. Such copies are required for instance during parallel
     * usage of site proofs because {@link OneStepSimplifier} has an internal state.
     *
     * @param sourceInitConfig The {@link InitConfig} to copy its {@link ProofEnvironment}.
     * @return The created {@link ProofEnvironment} which is a copy of the environment of the given
     *         {@link Proof} but with its own {@link OneStepSimplifier} instance.
     */
    @SuppressWarnings("unchecked")
    public static ProofEnvironment cloneProofEnvironmentWithOwnOneStepSimplifier(
            final InitConfig sourceInitConfig, final boolean useSimplifyTermProfile) {
        // Get required source instances
        final RuleJustificationInfo sourceJustiInfo = sourceInitConfig.getJustifInfo();
        // Create new profile which has separate OneStepSimplifier instance
        JavaProfile profile;
        if (useSimplifyTermProfile) {
            profile = new SimplifyTermProfile() {
                @Override
                protected ImmutableList<TermLabelConfiguration> computeTermLabelConfiguration() {
                    Profile sourceProfile = sourceInitConfig.getProfile();
                    if (sourceProfile instanceof SymbolicExecutionJavaProfile) {
                        ImmutableList<TermLabelConfiguration> result =
                            super.computeTermLabelConfiguration();
                        // Make sure that the term labels of symbolic execution are also supported
                        // by the new environment.
                        result = result.prepend(SymbolicExecutionJavaProfile
                                .getSymbolicExecutionTermLabelConfigurations(
                                    SymbolicExecutionJavaProfile
                                            .isTruthValueEvaluationEnabled(sourceInitConfig)));
                        return result;
                    } else {
                        return super.computeTermLabelConfiguration();
                    }
                }
            };
        } else {
            profile = new JavaProfile() {
                @Override
                protected ImmutableList<TermLabelConfiguration> computeTermLabelConfiguration() {
                    Profile sourceProfile = sourceInitConfig.getProfile();
                    if (sourceProfile instanceof SymbolicExecutionJavaProfile) {
                        ImmutableList<TermLabelConfiguration> result =
                            super.computeTermLabelConfiguration();
                        // Make sure that the term labels of symbolic execution are also supported
                        // by the new environment.
                        result = result.prepend(SymbolicExecutionJavaProfile
                                .getSymbolicExecutionTermLabelConfigurations(
                                    SymbolicExecutionJavaProfile
                                            .isTruthValueEvaluationEnabled(sourceInitConfig)));
                        return result;
                    } else {
                        return super.computeTermLabelConfiguration();
                    }
                }
            };
        }
        // Create new InitConfig
        final InitConfig initConfig =
            new InitConfig(sourceInitConfig.getServices().copy(profile, false));
        // Set modified taclet options in which runtime exceptions are banned.
        Choice runtimeExceptionTreatment = new Choice("ban", "runtimeExceptions");
        ImmutableSet<Choice> choices = SideProofUtil
                .activateChoice(sourceInitConfig.getActivatedChoices(), runtimeExceptionTreatment);
        initConfig.setActivatedChoices(choices);
        // Initialize InitConfig with settings from the original InitConfig.
        final ProofSettings clonedSettings = sourceInitConfig.getSettings() != null
                ? new ProofSettings(sourceInitConfig.getSettings())
                : null;
        initConfig.setSettings(clonedSettings);
        initConfig.setTaclet2Builder(
            (HashMap<Taclet, TacletBuilder<? extends Taclet>>) sourceInitConfig.getTaclet2Builder()
                    .clone());
        initConfig.setTaclets(sourceInitConfig.getTaclets());
        // Create new ProofEnvironment and initialize it with values from initial one.
        ProofEnvironment env = new ProofEnvironment(initConfig);
        for (Taclet taclet : initConfig.activatedTaclets()) {
            initConfig.getJustifInfo().addJustification(taclet,
                sourceJustiInfo.getJustification(taclet));
        }
        for (BuiltInRule rule : initConfig.builtInRules()) {
            RuleJustification origJusti = sourceJustiInfo.getJustification(rule);
            if (origJusti == null) {
                assert rule instanceof OneStepSimplifier;
                origJusti = AxiomJustification.INSTANCE;
            }
            initConfig.getJustifInfo().addJustification(rule, origJusti);
        }
        return env;
    }

    /**
     * <p>
     * Stores or disposes the {@link Proof} of the {@link ApplyStrategyInfo} in
     * {@link SideProofStore#DEFAULT_INSTANCE}.
     * </p>
     * <p>
     * This method should be called whenever a side proof is no longer needed and should be disposed
     * or stored for debugging purposes.
     * </p>
     *
     * @param description The description.
     * @param info The {@link ApplyStrategyInfo} to store or dispose its {@link Proof}.
     */
    public static void disposeOrStore(String description, ApplyStrategyInfo<Proof, Goal> info) {
        if (info != null) {
            if (SideProofStore.DEFAULT_INSTANCE.isEnabled()) {
                SideProofStore.DEFAULT_INSTANCE.addProof(description, info.getProof());
            } else {
                info.getProof().dispose();
            }
        }
    }
}
