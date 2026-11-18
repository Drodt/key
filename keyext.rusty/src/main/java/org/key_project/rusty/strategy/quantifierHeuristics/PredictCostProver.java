/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.TermFactory;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.strategy.quantifierHeuristics.theory.TheoryReasoning;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

/// TODO: rewrite, this seems pretty inefficient ...
public class PredictCostProver {
    private final TermBuilder tb;

    private final Term trueT, falseT;

    /// assume that all literal in <code>assertLiterals</code> are true
    private ImmutableSet<Term> assertLiterals;

    /// clauses from <code>instance</code> of CNF
    private Set<Clause> clauses = new LinkedHashSet<>();

    private final Services services;

    /// Equality reasoning over the sequent's assumed equalities, built once and shared across the
    /// cost predictions of all candidate instantiations. Lets a clause literal already satisfied by
    /// an assumed equality be recognized, so the instance is not proposed as a useful step.
    private final Congruence congruence;

    private PredictCostProver(Term instance, ImmutableSet<Term> assertList, Congruence congruence,
            Services services) {
        this.assertLiterals = assertList;
        this.congruence = congruence;
        this.services = services;
        this.tb = services.getTermBuilder();
        this.trueT = tb.tt();
        this.falseT = tb.ff();
        initClauses(instance);
    }

    /// Predicts the cost of instantiating a quantified formula's matrix with a candidate
    /// substitution.
    ///
    /// @param sub a ground substitution for the quantified variables
    /// @param matrix the matrix of the quantified formula
    /// @param assertList the literals assumed to be true on the sequent, already normalised through
    /// `congruence`
    ///
    /// @param congruence equality reasoning over the sequent's assumed equalities
    /// @param services access to the theory operators
    /// @return the predicted cost, or -1 if the substitution is not ground or every clause is
    /// proved
    /// true
    public static long computerInstanceCost(Substitution sub, Term matrix,
            ImmutableSet<Term> assertList, Congruence congruence, Services services) {

        if (!sub.isGround()) {
            // non-ground substitutions not supported yet
            return -1;
        } else {
            final PredictCostProver prover = new PredictCostProver(
                sub.applyWithoutCasts(matrix, services), assertList, congruence, services);
            return prover.cost();
        }
    }

    /// The axioms of one sequent prepared for repeated predictions: the congruence built from
    /// them and the literals normalized by it. Opaque outside this package, so the congruence
    /// type stays internal; build once per sequent and reuse for every candidate instance.
    public static final class PreparedAxioms {
        private final Congruence congruence;
        private final ImmutableSet<Term> normalized;

        private PreparedAxioms(Congruence congruence, ImmutableSet<Term> normalized) {
            this.congruence = congruence;
            this.normalized = normalized;
        }
    }

    /// Prepares the given literals for repeated predictions over one sequent.
    ///
    /// @param assertList the literals assumed to be true on the sequent
    /// @param services access to the theory operators
    /// @return the prepared axioms
    public static PreparedAxioms prepare(ImmutableSet<Term> assertList, Services services) {
        final Congruence congruence = new Congruence(assertList, services);
        ImmutableSet<Term> normalized = assertList;
        if (!congruence.isTrivial()) {
            final TermFactory tf = services.getTermFactory();
            ImmutableSet<Term> res = DefaultImmutableSet.nil();
            for (final Term lit : assertList) {
                res = res.add(congruence.normalize(lit, tf));
            }
            normalized = res;
        }
        return new PreparedAxioms(congruence, normalized);
    }

    /// As the congruence-taking entry point, with the axioms prepared by [#prepare].
    ///
    /// @param sub a ground substitution for the quantified variables
    /// @param matrix the matrix of the quantified formula
    /// @param axioms the prepared axioms of the sequent
    /// @param services access to the theory operators
    /// @return the predicted cost, or -1 if the substitution is not ground or every clause is
    /// proved true
    public static long computerInstanceCost(Substitution sub, Term matrix, PreparedAxioms axioms,
            Services services) {
        return computerInstanceCost(sub, matrix, axioms.normalized, axioms.congruence, services);
    }

    /// Convenience entry point for callers without a shared congruence: builds one from
    /// `assertList` and normalises the literals, then predicts the cost. The quantifier
    /// instantiation path shares one congruence per sequent instead and calls the overload above.
    ///
    /// @param sub a ground substitution for the quantified variables
    /// @param matrix the matrix of the quantified formula
    /// @param assertList the literals assumed to be true on the sequent
    /// @param services access to the theory operators
    /// @return the predicted cost
    public static long computerInstanceCost(Substitution sub, Term matrix,
            ImmutableSet<Term> assertList, Services services) {
        return computerInstanceCost(sub, matrix, prepare(assertList, services), services);
    }

    private void initClauses(Term instance) {
        for (var t : TriggerUtils.setByOperator(instance, Junctor.AND)) {
            for (ImmutableSet<Term> lit : createClause(
                TriggerUtils.setByOperator(t, Junctor.OR))) {
                clauses.add(new Clause(lit));
            }
        }
    }

    private ImmutableSet<ImmutableSet<Term>> createClause(
            ImmutableSet<Term> set) {
        final ImmutableSet<ImmutableSet<Term>> nil = DefaultImmutableSet.nil();
        ImmutableSet<ImmutableSet<Term>> res = nil.add(DefaultImmutableSet.<Term>nil());
        for (var t : set) {
            ImmutableSet<ImmutableSet<Term>> tmp = nil;
            for (ImmutableSet<Term> cl : res) {
                tmp = tmp.add(cl.add(t));
            }
            res = tmp;
        }
        return res;
    }

    /// Checks whether `problem` holds on its own. The literal is stripped of its leading
    /// negations and offered to each theory support; the first support that reaches a verdict wins,
    /// and the stripped negations are re-applied to it.
    ///
    /// @param problem the literal to decide
    /// @return `trueT` if the literal is proved true, `falseT` if proved
    /// false,
    /// and `problem` if undecided
    private Term provedBySelf(Term problem) {
        boolean negated = false;
        Term pro = problem;
        while (pro.op() == Junctor.NOT) {
            negated = !negated;
            pro = pro.sub(0);
        }
        for (final TheoryReasoning support : services.getProfile().getTheorySupports(false)) {
            TheoryReasoning.LiteralDecision decision =
                support.decideStrippedSelf(pro, services);
            if (decision != TheoryReasoning.LiteralDecision.UNKNOWN) {
                if (negated) {
                    decision = decision.negate();
                }
                return decision == TheoryReasoning.LiteralDecision.PROVED ? trueT : falseT;
            }
        }
        return problem;
    }

    /// Checks whether `problem` follows from the assumed-true `axiom` by
    /// offering both to each theory support. The first support that reaches a verdict wins.
    ///
    /// @param problem the literal to decide
    /// @param axiom a literal assumed to be true
    /// @return `trueT` if the axiom proves the problem, `falseT` if it proves
    /// the problem's negation, and `problem` if undecided
    private Term provedByAnother(Term problem, Term axiom) {
        for (final TheoryReasoning support : services.getProfile().getTheorySupports(false)) {
            final TheoryReasoning.LiteralDecision decision =
                support.decideFromAxiom(problem, axiom, services);
            if (decision != TheoryReasoning.LiteralDecision.UNKNOWN) {
                return decision == TheoryReasoning.LiteralDecision.PROVED ? trueT : falseT;
            }
        }
        return problem;
    }

    /// Checks whether `problem` holds, first on its own and then from each assumed
    /// literal
    /// in turn.
    ///
    /// @param problem a literal to decide
    /// @param assertLits literals assumed to be true
    /// @return `trueT` if the literal is proved true, `falseT` if proved
    /// false,
    /// and the literal itself if neither
    private Term proveLiteral(Term problem, Iterable<Term> assertLits) {
        final Term normProblem = congruence.normalize(problem, services.getTermFactory());
        Term res = provedBySelf(normProblem);
        if (TriggerUtils.isTrueOrFalse(res)) {
            return res;
        }
        for (Term t : assertLits) {
            res = provedByAnother(normProblem, t);
            if (TriggerUtils.isTrueOrFalse(res)) {
                return res;
            }
        }
        return problem;
    }

    /// Normalizes the literals of a clause reduced to a single literal, before it is assumed for
    /// the
    /// remaining clauses, so later decisions match it modulo the congruence.
    ///
    /// @param lits the literals to normalize
    /// @return the normalized literals, or `lits` unchanged when the congruence is trivial
    private ImmutableSet<Term> normalizeLiterals(ImmutableSet<Term> lits) {
        if (congruence.isTrivial()) {
            return lits;
        }
        ImmutableSet<Term> res = DefaultImmutableSet.nil();
        for (final Term l : lits) {
            res = res.add(congruence.normalize(l, services.getTermFactory()));
        }
        return res;
    }

    /// do two-step refinement and return the cost
    private long cost() {
        return firstRefine();
    }

    /// Refines every clause against the asserted literals and combines the results into a cost. A
    /// clause refined to false makes the whole instance cost 0. Clauses refined to true are
    /// dropped.
    /// The sizes of the remaining clauses are multiplied. A clause left with a single literal adds
    /// that literal to the asserted literals. When every clause is dropped and nothing was added
    /// the
    /// instance is trivially true and the cost is -1.
    private long firstRefine() {
        long cost = 1;
        boolean assertChanged = false;
        Set<Clause> res = new LinkedHashSet<>();
        for (final Clause c : clauses) {
            c.firstRefine();
            long cCost = c.cost();
            if (cCost == 0) {
                cost = 0;
                res.clear();
                break;
            }
            if (cCost == -1) {
                continue;
            }
            if (c.literals.size() == 1) {
                assertChanged = true;
                assertLiterals = assertLiterals.union(c.literals);
            } else {
                res.add(c);
            }
            cost = cost * cCost;
        }
        clauses = res;
        if (cost == 0) {
            return 0;
        }
        if (res.isEmpty() && !assertChanged) {
            return -1;
        }
        return cost;
    }

    private class Clause implements Iterable<Term> {
        /// all literals contained in this clause
        private ImmutableSet<Term> literals;

        public Clause(ImmutableSet<Term> lits) {
            literals = lits;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Clause other)) {
                return false;
            }
            if (other.literals.size() != literals.size()) {
                return false;
            }
            return literals.equals(other.literals);
        }

        @Override
        public int hashCode() {
            return literals.hashCode();
        }

        @Override
        public Iterator<Term> iterator() {
            return literals.iterator();
        }

        /// @return 0 if this clause is refined to false. 1 if true. Otherwise,return the number of
        /// literals it left.
        public long cost() {
            if (literals.size() == 1 && literals.contains(falseT)) {
                return 0;
            }
            if (literals.contains(trueT)) {
                return -1;
            }
            return literals.size();
        }

        /// Refine this clause in two process, first try to refined by itself, @see selfRefine.
        /// Second refine this clause by assuming assertLiteras are true
        public void firstRefine() {
            literals = this.refine(assertLiterals);
        }

        /// Refine literals in this clause, but it does not change literlas, only return literals
        /// that can't be removed by refining
        public ImmutableSet<Term> refine(Iterable<Term> assertLits) {
            ImmutableSet<Term> res = DefaultImmutableSet.nil();
            for (final Term lit : this) {
                final Operator op = proveLiteral(lit, assertLits).op();
                if (op == Junctor.TRUE) {
                    res = DefaultImmutableSet.<Term>nil().add(trueT);
                    break;
                }
                if (op == Junctor.FALSE) {
                    continue;
                }
                res = res.add(lit);
            }
            if (res.isEmpty()) {
                res = res.add(falseT);
            }
            return res;
        }

        @Override
        public String toString() {
            return literals.toString();
        }
    }
}
