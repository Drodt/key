/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import java.util.*;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.proof.OpReplacer;
import org.key_project.rusty.strategy.quantifierHeuristics.Constraint;
import org.key_project.rusty.strategy.quantifierHeuristics.EqualityConstraint;
import org.key_project.rusty.strategy.quantifierHeuristics.Metavariable;
import org.key_project.rusty.strategy.quantifierHeuristics.Substitution;
import org.key_project.util.collection.DefaultImmutableMap;
import org.key_project.util.collection.ImmutableSet;

import static org.key_project.rusty.logic.equality.RenamingTermProperty.RENAMING_TERM_PROPERTY;

/// Support for the equality theory.
///
/// Forbids the equality `=` as a trigger (matching on it has not been observed to help
/// instantiation) and provides no derived triggers. For cost prediction it decides a literal that
/// is
/// an equality or equivalence whose two sides are equal up to renaming, and decides an arbitrary
/// literal that equals an assumed one up to renaming (or contradicts it under a negation).
final class EqualityTheorySupport implements QuantifierTheorySupport {
    /// Forbids the equality `=` as a trigger.
    ///
    /// @param candidate a trigger candidate that contains the quantified variables
    /// @param enclosing the term the candidate is an argument of, null at the top of a literal
    /// @param services access to the theory operators
    /// @return the verdict
    @Override
    public CandidateVerdict verdictOn(Term candidate, Term enclosing, Services services) {
        return candidate.op() == Equality.EQUALS ? CandidateVerdict.FORBIDDEN
                : CandidateVerdict.ACCEPTABLE;
    }

    /// Provides no derived triggers.
    ///
    /// @param term an accepted trigger term
    /// @param clauseVariables the quantified variables of the clause the trigger belongs to
    /// @param services access to the theory operators
    /// @return the empty list
    @Override
    public List<Term> provideTriggers(Term term,
            ImmutableSet<LogicVariable> clauseVariables, Services services,
            MetavariableFactory metavariableFactory) {
        return List.of();
    }

    /// Checks whether the literal is an equality or equivalence whose two sides are equal up to
    /// renaming.
    ///
    /// @param strippedLiteral a literal without leading negations
    /// @param services access to the theory operators
    /// @return `PROVED` if the two sides are equal up to renaming, otherwise `UNKNOWN`
    @Override
    public LiteralDecision decideStrippedSelf(Term strippedLiteral, Services services) {
        final Operator op = strippedLiteral.op();
        if (op == Equality.EQUALS || op == Equality.EQV) {
            if (RENAMING_TERM_PROPERTY.equalsModThisProperty(strippedLiteral.sub(0),
                strippedLiteral.sub(1))) {
                return LiteralDecision.PROVED;
            }
        }
        return LiteralDecision.UNKNOWN;
    }

    /// Checks whether the literal follows from the axiom by equality up to renaming. Leading
    /// negations of both are tracked, so an axiom equal to the negated literal refutes it.
    ///
    /// @param literal a literal to decide
    /// @param axiom a literal assumed to be true
    /// @param services access to the theory operators
    /// @return `PROVED` if the axiom equals the literal, `REFUTED` if it equals its
    /// negation, otherwise `UNKNOWN`
    @Override
    public LiteralDecision decideFromAxiom(Term literal, Term axiom, Services services) {
        boolean negated = false;
        Term pro = literal;
        while (pro.op() == Junctor.NOT) {
            pro = pro.sub(0);
            negated = !negated;
        }
        Term ax = axiom;
        while (ax.op() == Junctor.NOT) {
            ax = ax.sub(0);
            negated = !negated;
        }
        if (RENAMING_TERM_PROPERTY.equalsModThisProperty(pro, ax)) {
            return negated ? LiteralDecision.REFUTED : LiteralDecision.PROVED;
        }
        return LiteralDecision.UNKNOWN;
    }

    @Override
    public List<Term> fallbackTriggers(ClauseTriggers selection, Services services,
            MetavariableFactory metavariableFactory) {
        List<Term> fallbackTriggers = new ArrayList<>();
        final ClauseAnalysis clauseInfo = selection.clause();

        final TermBuilder tb = services.getTermBuilder();
        final Map<LogicVariable, Term> qv2mv = new LinkedHashMap<>();
        final Map<Metavariable, QuantifiableVariable> mv2qv = new LinkedHashMap<>();
        for (var var : clauseInfo.clause().freeVars()) {
            final Metavariable mv = metavariableFactory.fresh(var.sort());
            qv2mv.put((LogicVariable) var, tb.var(mv));
            if (clauseInfo.universalVariables().contains(var)) {
                mv2qv.put(mv, var);
            }
        }
        final Substitution qv2mvSubst = new Substitution(DefaultImmutableMap.fromMap(qv2mv));
        final OpReplacer mv2qvReplacer =
            new OpReplacer(mv2qv, services.getTermFactory());
        for (Term lit : clauseInfo.literals()) {
            if (lit.op() == Equality.EQUALS) {
                fallbackTriggers.addAll(
                    solveEquation(mv2qv.keySet(), qv2mvSubst, mv2qvReplacer, lit, services));
            }
        }
        return fallbackTriggers;
    }

    /// solves equation f(u) = f(g(v)) to u = g(MV_V)
    /// @param mvs set of Metavariables used to replace **universal** bound variables
    /// @param qv2mv substitution of the free variables of lit by their meta variables
    /// @param mv2qv OpReplacer to restore universal (not existential) bound variables
    /// @param lit the Term representing an uncovered literal
    /// @param services the Services class provides access to term construction and other services
    /// @return list of solved equations that describe triggers

    // Constraint is deprecated wholesale (quantifierHeuristics.constraint); no replacement API
    // exists.
    @SuppressWarnings("deprecation")
    private List<Term> solveEquation(Set<Metavariable> mvs, Substitution qv2mv,
            OpReplacer mv2qv, Term lit,
            Services services) {
        final TermBuilder tb = services.getTermBuilder();
        final Term litWithMV = (Term) qv2mv.applyWithoutCasts(lit, services);
        final Constraint c =
            EqualityConstraint.BOTTOM.unify(litWithMV.sub(0), litWithMV.sub(1), services);
        List<Term> solvedEquations = new ArrayList<>();
        if (c.isSatisfiable()) {
            for (final Metavariable mv : mvs) {
                final Term solution = c.getInstantiation(mv, services);
                final Operator instOp = solution.op();
                if (instOp instanceof LogicVariable ||
                        instOp instanceof Metavariable) {
                    // solutions that are a variable
                    // and contain no function symbol do not
                    // make useful triggers
                    continue;
                }
                final Term solvedEquation = mv2qv.replace(tb.equals(tb.var(mv), solution));
                solvedEquations.add(solvedEquation);
                solvedEquations.add(tb.equals(solvedEquation.sub(1), solvedEquation.sub(0)));
            }
        }
        return solvedEquations;
    }
}
