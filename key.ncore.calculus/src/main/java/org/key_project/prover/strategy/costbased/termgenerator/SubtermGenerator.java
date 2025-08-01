/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.prover.strategy.costbased.termgenerator;

import java.util.Iterator;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Term;
import org.key_project.prover.proof.ProofGoal;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.TopRuleAppCost;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.prover.strategy.costbased.termfeature.TermFeature;
import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

import org.jspecify.annotations.NonNull;

/// Term generator that enumerates the sub-terms or sub-formulas of a given term. Similarly to
/// [org.key_project.prover.strategy.costbased.termfeature.RecSubTermFeature], a term feature
/// can be given that determines when traversal should
/// be stopped, i.e., when one should not descend further into a term.
public abstract class SubtermGenerator<Goal extends ProofGoal<@NonNull Goal>>
        implements TermGenerator<Goal> {

    private final TermFeature cond;
    private final ProjectionToTerm<Goal> completeTerm;

    private SubtermGenerator(ProjectionToTerm<Goal> completeTerm, TermFeature cond) {
        this.cond = cond;
        this.completeTerm = completeTerm;
    }

    /// Left-traverse the subterms of a term in depth-first order. Each term is returned before its
    /// proper subterms.
    public static <Goal extends ProofGoal<@NonNull Goal>> TermGenerator<Goal> leftTraverse(
            ProjectionToTerm<Goal> cTerm, TermFeature cond) {
        return new SubtermGenerator<>(cTerm, cond) {
            public Iterator<Term> generate(RuleApp app,
                    PosInOccurrence pos, Goal goal,
                    MutableState mState) {
                return new LeftIterator(getTermInst(app, pos, goal, mState), mState,
                    goal.proof().getServices());
            }
        };
    }

    /// Right-traverse the subterms of a term in depth-first order. Each term is returned before its
    /// proper subterms.
    public static <Goal extends ProofGoal<@NonNull Goal>> TermGenerator<Goal> rightTraverse(
            ProjectionToTerm<Goal> cTerm, TermFeature cond) {
        return new SubtermGenerator<>(cTerm, cond) {
            public Iterator<Term> generate(RuleApp app,
                    PosInOccurrence pos, Goal goal,
                    MutableState mState) {
                return new RightIterator(getTermInst(app, pos, goal, mState), mState,
                    goal.proof().getServices());
            }
        };
    }

    protected Term getTermInst(RuleApp app, PosInOccurrence pos, Goal goal, MutableState mState) {
        final Term completeTermInst = completeTerm.toTerm(app, pos, goal, mState);
        assert completeTermInst != null : "@AssumeAssertion(nullness): Term should not be null";
        return completeTermInst;
    }

    private boolean descendFurther(Term t, MutableState mState, LogicServices services) {
        return !(cond.compute(t, mState, services) instanceof TopRuleAppCost);
    }

    abstract static class SubIterator implements Iterator<Term> {
        protected ImmutableList<Term> termStack;
        protected final MutableState mState;
        protected final LogicServices services;

        protected SubIterator(Term t, MutableState mState, LogicServices services) {
            termStack = ImmutableSLList.<Term>nil().prepend(t);
            this.mState = mState;
            this.services = services;
        }

        public boolean hasNext() {
            return !termStack.isEmpty();
        }
    }

    class LeftIterator extends SubIterator {
        public LeftIterator(Term t, MutableState mState, LogicServices services) {
            super(t, mState, services);
        }

        public Term next() {
            final Term res = termStack.head();
            termStack = termStack.tail();

            if (descendFurther(res, mState, services)) {
                for (int i = res.arity() - 1; i >= 0; --i) {
                    termStack = termStack.prepend(res.sub(i));
                }
            }

            return res;
        }

        /// throw an unsupported operation exception as generators do not remove
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    class RightIterator extends SubIterator {
        public RightIterator(Term t, MutableState mState, LogicServices services) {
            super(t, mState, services);
        }

        public Term next() {
            final Term res = termStack.head();
            termStack = termStack.tail();

            if (descendFurther(res, mState, services)) {
                for (int i = 0; i != res.arity(); ++i) {
                    termStack = termStack.prepend(res.sub(i));
                }
            }

            return res;
        }

        /// throw an unsupported operation exception as generators do not remove
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
}
