/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import java.math.BigInteger;
import java.util.List;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Monomial;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Polynomial;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.rule.metaconstruct.arith.RPolynomial;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableMap;
import org.key_project.util.collection.ImmutableSet;

/// Support for the integer theory.
///
/// Forbids the (non-strict) comparisons as triggers (matching on them has not been observed to help
/// instantiation) and provides no derived triggers. For cost prediction it decides an arithmetic
/// comparison from itself or from an assumed comparison, through [HandleArith].
final class IntegerTheorySupport implements QuantifierTheorySupport {
    /// Forbids the non-strict comparisons `<=` and `>=` as triggers.
    ///
    /// @param candidate a trigger candidate that contains the quantified variables
    /// @param enclosing the term the candidate is an argument of, null at the top of a literal
    /// @param services access to the integer theory operators
    /// @return the verdict
    @Override
    public CandidateVerdict verdictOn(Term candidate, Term enclosing, Services services) {
        final Operator op = candidate.op();
        final IntLDT intLDT = services.getLDTs().getIntLDT();
        return op == intLDT.getLessOrEquals() || op == intLDT.getGreaterOrEquals()
                ? CandidateVerdict.FORBIDDEN
                : CandidateVerdict.ACCEPTABLE;
    }

    /// Provides no derived triggers.
    ///
    /// @param term an accepted trigger term
    /// @param clauseVariables the quantified variables of the clause the trigger belongs to
    /// @param services access to the integer theory operators
    /// @return the empty list
    @Override
    public List<Term> provideTriggers(Term term,
            ImmutableSet<LogicVariable> clauseVariables, Services services,
            MetavariableFactory metavariableFactory) {
        return List.of();
    }

    /// Checks whether an arithmetic comparison holds on its own, through [HandleArith].
    ///
    /// @param strippedLiteral a literal without leading negations
    /// @param services access to the integer theory operators
    /// @return the arithmetic verdict, or `UNKNOWN` if undecided
    @Override
    public LiteralDecision decideStrippedSelf(Term strippedLiteral, Services services) {
        final IntLDT intLDT = services.getLDTs().getIntLDT();
        return TheoryReasoning
                .fromTruthTerm(HandleArith.provedByArith(strippedLiteral, intLDT, services));
    }

    /// Checks whether an arithmetic comparison follows from an assumed comparison, through
    /// [HandleArith].
    ///
    /// @param literal a literal to decide
    /// @param axiom a literal assumed to be true
    /// @param services access to the integer theory operators
    /// @return the arithmetic verdict, or `UNKNOWN` if undecided
    @Override
    public LiteralDecision decideFromAxiom(Term literal, Term axiom, Services services) {
        final IntLDT intLDT = services.getLDTs().getIntLDT();
        return TheoryReasoning
                .fromTruthTerm(HandleArith.provedByArith(literal, axiom, intLDT, services));
    }

    /// Restricts the equality normalization on integer terms to what the arithmetic decisions
    /// tolerate. A number literal is never rewritten away: it is the concrete value the comparisons
    /// in [HandleArith] are decided on. A term built by the polynomial operators is left in
    /// place in both directions: rewriting it away collapses the structure that
    /// [Polynomial] decomposes, and rewriting other
    /// terms into it makes it a class representative, which the one-pass normalization of
    /// `Congruence` does not rewrite further. Rewriting an atom to a number literal or to
    /// another atom stays permitted: that only identifies atoms, which the assumed equality
    /// justifies, and can only add arithmetic decisions.
    ///
    /// @param from the term whose occurrences would be rewritten
    /// @param to the replacement term
    /// @param services access to the integer theory operators
    /// @return whether the rewrite keeps the arithmetic forms intact
    @Override
    public boolean allowsEqualityRewrite(Term from, Term to, Services services) {
        final IntLDT intLDT = services.getLDTs().getIntLDT();
        if (from.op() == intLDT.getNumberSymbol()) {
            return false;
        }
        return !hasPolynomialStructure(from, intLDT) && !hasPolynomialStructure(to, intLDT);
    }

    /// Solves `pattern = instance` for the single variable the pattern is affine in.
    ///
    /// A trigger array index is typically written relative to an offset, as `base + t`, while
    /// the terms a proof produces are absolute. Decomposing both sides as polynomials turns the
    /// match into an equation: with the pattern `k*t + rest` and the instance `s`, the
    /// variable is `(s - rest) / k`. That division has to be exact, since a non-integer
    /// solution cannot reproduce the instance.
    ///
    /// Instantiating a universally quantified formula with any term is sound, so a solution that
    /// does not reproduce the instance costs one instantiation and nothing else.
    ///
    /// Declined when the pattern is affine in more than one variable, because the equation is then
    /// underdetermined; when a variable sits inside a product with another atom, because that is
    /// not linear; and when a variable of the pattern is already bound by this match, which would
    /// require substituting before solving.
    @Override
    public ImmutableMap<LogicVariable, Term> solveForVariable(Term pattern, Term instance,
            ImmutableMap<LogicVariable, Term> varMap, Services services) {
        final IntLDT intLDT = services.getLDTs().getIntLDT();
        final Sort intSort = intLDT.targetSort();
        // The decomposition below allocates, and matching asks after every failed comparison, so
        // the cheap tests come first: only a term built by the polynomial operators can be affine.
        if (pattern.sort() != intSort || instance.sort() != intSort
                || !hasPolynomialStructure(pattern, intLDT)
                || !instance.freeVars().isEmpty() || pattern.freeVars().isEmpty()) {
            return null;
        }
        for (final QuantifiableVariable free : pattern.freeVars()) {
            if (varMap.get((LogicVariable) free) != null) {
                return null;
            }
        }

        final Polynomial patternPoly = RPolynomial.create(pattern, services);
        Polynomial rest = Polynomial.ZERO.add(patternPoly.getConstantTerm());
        Monomial linear = null;
        LogicVariable variable = null;
        for (final Monomial part : patternPoly.getParts()) {
            final ImmutableList<Term> atoms = part.getParts();
            if (atoms.stream().allMatch(a -> a.freeVars().isEmpty())) {
                rest = rest.add(part);
                continue;
            }
            if (atoms.size() != 1 || linear != null
                    || !(atoms.head().op() instanceof LogicVariable qv)) {
                return null;
            }
            linear = part;
            variable = qv;
        }
        if (linear == null) {
            return null;
        }

        final Polynomial solution = divideExactly(
            RPolynomial.create(instance, services).sub(rest), linear.getCoefficient());
        return solution == null ? null
                : varMap.put(variable, RPolynomial.toTerm(solution, services));
    }

    /// Divides every coefficient by the divisor, or returns null when a division is not exact.
    private static Polynomial divideExactly(Polynomial p, BigInteger divisor) {
        if (divisor.signum() == 0 || p.getConstantTerm().remainder(divisor).signum() != 0) {
            return null;
        }
        Polynomial result = Polynomial.ZERO.add(p.getConstantTerm().divide(divisor));
        for (Monomial part : p.getParts()) {
            if (part.getCoefficient().remainder(divisor).signum() != 0) {
                return null;
            }
            result = result.add(part.setCoefficient(part.getCoefficient().divide(divisor)));
        }
        return result;
    }

    /// Whether the term's top operator is such that the polynomial decomposition takes apart.
    /// Besides the
    /// sum and product this covers difference and negation, which the proof search rewrites into
    /// them.
    ///
    /// @param t a term
    /// @param intLDT the integer theory operators
    /// @return whether the top operator carries polynomial structure
    private static boolean hasPolynomialStructure(Term t, IntLDT intLDT) {
        final Operator op = t.op();
        return op == intLDT.getAdd() || op == intLDT.getMul()
                || op == intLDT.getSub() || op == intLDT.getNeg();
    }
}
