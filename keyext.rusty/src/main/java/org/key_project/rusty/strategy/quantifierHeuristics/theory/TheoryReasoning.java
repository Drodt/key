/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import org.key_project.logic.Term;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.util.collection.ImmutableMap;

/// A theory's own reasoning about terms and literals, as the heuristic needs it: solving an
/// equation for a variable while matching, deciding a literal when a cost is predicted, and
/// permitting a rewrite of the equality reasoning.
///
/// These questions are about terms alone. A front end whose terms are built from the same theory
/// reuses an implementation unchanged, whatever the programs behind the terms are, which is not so
/// for [TriggerSupport].
public interface TheoryReasoning {
    /// The outcome of judging a literal for cost prediction.
    enum LiteralDecision {
        PROVED, REFUTED, UNKNOWN;

        /// @return the decision for the negation of the judged literal
        public LiteralDecision negate() {
            return switch (this) {
                case PROVED -> REFUTED;
                case REFUTED -> PROVED;
                case UNKNOWN -> UNKNOWN;
            };
        }
    }

    /// Maps a truth term, as the theory reasoning returns it, to a decision: the true constant is
    /// [LiteralDecision#PROVED], the false constant [LiteralDecision#REFUTED], and any
    /// other term (an undecided literal) [LiteralDecision#UNKNOWN].
    ///
    /// @param t a truth term
    /// @return the matching decision
    static LiteralDecision fromTruthTerm(Term t) {
        if (t.op() == Junctor.TRUE) {
            return LiteralDecision.PROVED;
        }
        if (t.op() == Junctor.FALSE) {
            return LiteralDecision.REFUTED;
        }
        return LiteralDecision.UNKNOWN;
    }

    /// Solves a trigger subterm against a ground instance when syntactic matching has failed.
    /// Basic matching compares the two structures, so a trigger whose array index is written
    /// against an offset never matches an instance written absolutely: a read of
    /// `base + t` does not match one of `x`, since `x - base` occurs nowhere in
    /// the proof. A theory that can invert its own index expressions solves the equation for the
    /// variable instead.
    /// Instantiating a universally quantified formula with any term is sound, so a solution that
    /// turns out not to reproduce the instance costs an instantiation and nothing else.
    ///
    /// @param pattern a trigger subterm, containing at least one variable not yet bound in
    /// `varMap`
    ///
    /// @param instance the ground term it should match
    /// @param varMap the bindings established so far
    /// @param services access to the theory's operators
    /// @return the extended bindings, or null when this theory cannot solve the equation
    default ImmutableMap<LogicVariable, Term> solveForVariable(Term pattern, Term instance,
            ImmutableMap<LogicVariable, Term> varMap, Services services) {
        return null;
    }

    /// Checks whether the literal holds on its own, for cost prediction. The literal is passed
    /// already stripped of its leading negations; the caller re-applies them to the returned
    /// decision, so an implementation reasons about the positive form only.
    ///
    /// @param strippedLiteral a literal without leading negations
    /// @param services access to the theory operators
    /// @return whether this theory proves the literal true, false, or cannot decide it
    default LiteralDecision decideStrippedSelf(Term strippedLiteral, Services services) {
        return LiteralDecision.UNKNOWN;
    }

    /// Checks whether the literal follows from an assumed-true `axiom`, for cost prediction.
    /// Leading negations of both the literal and the axiom are handled by the implementation.
    ///
    /// @param literal a literal to decide
    /// @param axiom a literal assumed to be true
    /// @param services access to the theory operators
    /// @return whether the axiom proves the literal true, false, or cannot decide it
    default LiteralDecision decideFromAxiom(Term literal, Term axiom, Services services) {
        return LiteralDecision.UNKNOWN;
    }

    /// Whether the equality-based normalization of the cost prediction (see `Congruence`) may
    /// rewrite occurrences of `from` to `to`, justified by an assumed equality between
    /// the two. The proof search keeps the terms of a theory in a normal form of the theory's own
    /// rules, integer terms in polynomial form for example. A theory vetoes here when the rewrite
    /// would replace such a normal form, so the decisions of [#decideStrippedSelf] and
    /// [#decideFromAxiom] still see the forms they understand. When a rewrite is vetoed the
    /// congruence tries the opposite direction, and leaves the equality out entirely if that is
    /// vetoed too.
    ///
    /// @param from the term whose occurrences would be rewritten
    /// @param to the replacement term
    /// @param services access to the theory operators
    /// @return whether this theory permits the rewrite
    default boolean allowsEqualityRewrite(Term from, Term to, Services services) {
        return true;
    }
}

