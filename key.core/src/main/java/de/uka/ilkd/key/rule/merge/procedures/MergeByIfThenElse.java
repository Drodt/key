/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.rule.merge.procedures;

import java.util.LinkedHashSet;
import java.util.Optional;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermBuilder;
import de.uka.ilkd.key.logic.op.LocationVariable;
import de.uka.ilkd.key.rule.merge.MergeProcedure;
import de.uka.ilkd.key.rule.merge.MergeRule;
import de.uka.ilkd.key.util.mergerule.SymbolicExecutionState;

import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.Pair;

import static de.uka.ilkd.key.util.mergerule.MergeRuleUtils.countAtoms;
import static de.uka.ilkd.key.util.mergerule.MergeRuleUtils.getDistinguishingFormula;
import static de.uka.ilkd.key.util.mergerule.MergeRuleUtils.getUpdateRightSideFor;
import static de.uka.ilkd.key.util.mergerule.MergeRuleUtils.trySimplify;

/**
 * Rule that merges two sequents based on the if-then-else construction: If two locations are
 * assigned different values in the states, the value in the merged state is chosen based on the
 * path condition. This rule retains total precision. The if-then-else distinction is realized by
 * the respective construct for the update / symbolic state of the symbolic execution state. Note:
 * Doing this not with updates, but in the antecedent / path condition can be much more efficient:
 * See {@link MergeIfThenElseAntecedent}.
 *
 * @author Dominic Scheurer
 * @see MergeIfThenElseAntecedent
 * @see MergeRule
 */
public class MergeByIfThenElse extends MergeProcedure implements UnparametricMergeProcedure {
    private static MergeByIfThenElse INSTANCE = null;

    /**
     * Time in milliseconds after which a simplification attempt of a distinguishing formula times
     * out.
     */
    private static final int SIMPLIFICATION_TIMEOUT_MS = 1000;

    public static MergeByIfThenElse instance() {
        if (INSTANCE == null) {
            INSTANCE = new MergeByIfThenElse();
        }
        return INSTANCE;
    }

    private static final String DISPLAY_NAME = "MergeByIfThenElse";
    static final int MAX_UPDATE_TERM_DEPTH_FOR_CHECKING = 8;

    /*
     * (non-Javadoc)
     *
     * @see de.uka.ilkd.key.rule.merge.MergeProcedure#complete()
     */
    @Override
    public boolean complete() {
        return true;
    }

    @Override
    public ValuesMergeResult mergeValuesInStates(JTerm v, SymbolicExecutionState state1,
            JTerm valueInState1, SymbolicExecutionState state2, JTerm valueInState2,
            JTerm distinguishingFormula, Services services) {

        return new ValuesMergeResult(DefaultImmutableSet.nil(),
            createIfThenElseTerm(state1, state2, valueInState1, valueInState2,
                distinguishingFormula, services),
            new LinkedHashSet<>(), new LinkedHashSet<>());

    }

    @Override
    public boolean requiresDistinguishablePathConditions() {
        return true;
    }

    /**
     * Creates an if-then-else term for the variable v. If t1 is the right side for v in state1, and
     * t2 is the right side in state1, the resulting term corresponds to
     * <code>\if (c1) \then (t1) \else (t2)</code>, where c1 is the path condition of state1.
     * However, the method also tries an optimization: The path condition c2 of state2 could be used
     * if it is shorter than c1. Moreover, equal parts of c1 and c2 could be omitted, since the
     * condition shall only distinguish between the states.
     *
     * @param state1 First state to evaluate.
     * @param state2 Second state to evaluate.
     * @param ifTerm The term t1 (in the context of state1).
     * @param elseTerm The term t2 (in the context of state2).
     * @param distinguishingFormula The user-specified distinguishing formula. May be null (for
     *        automatic generation).
     * @param services The services object.
     * @return An if then else term like <code>\if (c1) \then (t1) \else (t2)</code>, where the cI
     *         are the path conditions of stateI.
     */
    public static JTerm createIfThenElseTerm(final SymbolicExecutionState state1,
            final SymbolicExecutionState state2, final JTerm ifTerm, final JTerm elseTerm,
            JTerm distinguishingFormula, final Services services) {

        TermBuilder tb = services.getTermBuilder();

        JTerm cond, ifForm, elseForm;

        if (distinguishingFormula == null) {
            DistanceFormRightSide distFormAndRightSidesForITEUpd =
                createDistFormAndRightSidesForITEUpd(state1, state2, ifTerm, elseTerm, services);

            cond = distFormAndRightSidesForITEUpd.distinguishingFormula();
            ifForm = distFormAndRightSidesForITEUpd.ifTerm();
            elseForm = distFormAndRightSidesForITEUpd.elseTerm();
        } else {
            cond = distinguishingFormula;
            ifForm = ifTerm;
            elseForm = elseTerm;
        }

        // Construct the update for the symbolic state
        return tb.ife(cond, ifForm, elseForm);

    }

    /**
     * Creates the input for an if-then-else update for the variable v. If t1 is the right side for
     * v in state1, and t2 is the right side in state1, the elements of the resulting quadruple can
     * be used to construct an elementary update corresponding to
     * <code>{ v := \if (c1) \then (t1) \else (t2) }</code>, where c1 is the path condition of
     * state1. However, the method also tries an optimization: The path condition c2 of state2 could
     * be used if it is shorter than c1. Moreover, equal parts of c1 and c2 could be omitted, since
     * the condition shall only distinguish between the states. The first element of the triple is
     * the discriminating condition, the second and third elements are the respective parts for the
     * if and else branch.
     *
     * @param v Variable to return the update for.
     * @param state1 First state to evaluate.
     * @param state2 Second state to evaluate.
     * @param services The services object.
     * @return Input to construct an elementary update like
     *         <code>{ v := \if (first) \then (second) \else (third) }</code>, where first, second
     *         and third are the respective components of the returned triple. The fourth component
     *         indicates whether the path condition of the first (fourth component = false) or the
     *         second (fourth component = true) state was used as a basis for the condition (first
     *         component).
     */
    static DistanceFormRightSide createDistFormAndRightSidesForITEUpd(
            LocationVariable v, SymbolicExecutionState state1, SymbolicExecutionState state2,
            Services services) {

        TermBuilder tb = services.getTermBuilder();

        JTerm rightSide1 = getUpdateRightSideFor(state1.first, v);
        JTerm rightSide2 = getUpdateRightSideFor(state2.first, v);

        if (rightSide1 == null) {
            rightSide1 = tb.var(v);
        }

        if (rightSide2 == null) {
            rightSide2 = tb.var(v);
        }

        return createDistFormAndRightSidesForITEUpd(state1, state2, rightSide1, rightSide2,
            services);
    }

    /**
     * Creates the input for an if-then-else update. The elements of the resulting
     * {@link DistanceFormRightSide} can be
     * used to construct an elementary update corresponding to
     * <code>{ v := \if (c1) \then (ifTerm) \else (elseTerm) }</code>, where c1 is the path
     * condition of state1. However, the method also tries an optimization: The path condition c2 of
     * state2 could be used if it is shorter than c1. Moreover, equal parts of c1 and c2 could be
     * omitted, since the condition shall only distinguish between the states. The first element of
     * the triple is the discriminating condition, the second and third elements are the respective
     * parts for the if and else branch.
     *
     * @param state1 First state to evaluate.
     * @param state2 Second state to evaluate.
     * @param ifTerm The if term.
     * @param elseTerm The else term.
     * @param services The services object.
     * @return Input to construct an elementary update like
     *         <code>{ v := \if (first) \then (second) \else (third) }</code>, where first, second
     *         and third are the respective components of the returned triple. The fourth component
     *         indicates whether the path condition of the first (fourth component = false) or the
     *         second (fourth component = true) state was used as a basis for the condition (first
     *         component).
     */
    static DistanceFormRightSide createDistFormAndRightSidesForITEUpd(
            SymbolicExecutionState state1, SymbolicExecutionState state2, JTerm ifTerm,
            JTerm elseTerm, Services services) {

        // We only need the distinguishing subformula; the equal part
        // is not needed. For soundness, it suffices that the "distinguishing"
        // formula is implied by the original path condition; for completeness,
        // we add the common subformula in the new path condition, if it
        // is not already implied by that.
        Optional<Pair<JTerm, JTerm>> distinguishingAndEqualFormula1 =
            getDistinguishingFormula(state1.second, state2.second, services);
        JTerm distinguishingFormula = distinguishingAndEqualFormula1
                .map(termTermPair -> termTermPair.first).orElse(null);

        Optional<Pair<JTerm, JTerm>> distinguishingAndEqualFormula2 =
            getDistinguishingFormula(state2.second, state1.second, services);
        JTerm distinguishingFormula2 = distinguishingAndEqualFormula2
                .map(termTermPair -> termTermPair.first).orElse(null);

        // NOTE (DS): This assertion does not prevent the merging of states with
        // equal
        // Symbolic State. This is intended behavior: In some proofs we have two
        // identical
        // nodes which we want to merge (possibly after a hide right / hide
        // left); this
        // should be allowed (although they are of course indistinguishable).
        assert distinguishingFormula != null || distinguishingFormula2 != null
                : String.format(
                    """

                            A computed distinguishing formula is trivial ("true"); please verify that everything is OK. Symbolic execution states were:

                            --- State 1 ---
                            %s

                            ---State 2---
                            %s
                            """,
                    state1, state2);

        boolean commuteSides = false;
        if (distinguishingFormula == null) {
            distinguishingFormula = distinguishingFormula2;
            commuteSides = true;
        } else if (distinguishingFormula2 != null) {
            // Choose the shorter distinguishing formula
            if (countAtoms(distinguishingFormula2) < countAtoms(distinguishingFormula)) {
                distinguishingFormula = distinguishingFormula2;
                commuteSides = true;
            }
        }

        // Try an automatic simplification
        distinguishingFormula = trySimplify(services.getProof(), distinguishingFormula, true,
            SIMPLIFICATION_TIMEOUT_MS);

        return new DistanceFormRightSide(distinguishingFormula,
            commuteSides ? elseTerm : ifTerm, commuteSides ? ifTerm : elseTerm, commuteSides);

    }

    @Override
    public String toString() {
        return DISPLAY_NAME;
    }

    /**
     * Represents the distance between formulas for an if-then-else update.
     * Input to construct an elementary update like
     * <code>{ v := \if (distinguishingFormula) \then (ifTerm) \else (elseTerm) }</code>, where
     * distinguishingFormula, ifTerm
     * and elseTerm are the respective components of the returned triple. The sideCommuted component
     * indicates whether the path condition of the distinguishingFormula (sideCommuted component =
     * false) or the
     * ifTerm (sideCommuted component = true) state was used as a basis for the condition
     * (distinguishingFormula
     * component).
     *
     * @param distinguishingFormula a formula
     * @param ifTerm a term
     * @param elseTerm a term
     * @param sideCommuted true if ifTerm and elseTerm have been swapped.
     * @see #createDistFormAndRightSidesForITEUpd(SymbolicExecutionState, SymbolicExecutionState,
     *      JTerm, JTerm, Services)
     */
    public record DistanceFormRightSide(JTerm distinguishingFormula, JTerm ifTerm, JTerm elseTerm,
            boolean sideCommuted) {
    }
}
