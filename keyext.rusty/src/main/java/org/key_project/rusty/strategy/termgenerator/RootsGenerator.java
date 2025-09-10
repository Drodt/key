/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.termgenerator;

import java.math.BigInteger;
import java.util.Iterator;

import org.key_project.logic.Term;
import org.key_project.prover.rules.RuleApp;
import org.key_project.prover.sequent.PosInOccurrence;
import org.key_project.prover.strategy.costbased.MutableState;
import org.key_project.prover.strategy.costbased.termProjection.ProjectionToTerm;
import org.key_project.prover.strategy.costbased.termgenerator.TermGenerator;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.AbstractTermTransformer;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.proof.Goal;
import org.key_project.rusty.rule.metaconstruct.arith.Monomial;
import org.key_project.util.collection.ImmutableSLList;

/// Term generator for inferring the range of values that a variable can have from a given
/// non-linear
/// (in)equation. The generator may only be called on formulas of the form `v^n = l`,
/// `v^n <= l`, `v^n >= l`, where `v` is an atomic term (does not start with
/// addition or multiplication) and `l` is a literal. The generator will then produce at most
/// one formula that describes the solutions of the formula using linear (in)equations.
public class RootsGenerator implements TermGenerator<Goal> {
    private final ProjectionToTerm<Goal> powerRelation;

    private final TermBuilder tb;
    private final BigInteger one = BigInteger.ONE;
    private final BigInteger two = BigInteger.valueOf(2);

    public static TermGenerator<Goal> create(ProjectionToTerm<Goal> powerRelation,
            Services services) {
        return new RootsGenerator(powerRelation, services.getTermBuilder());
    }

    private RootsGenerator(ProjectionToTerm<Goal> powerRelation, TermBuilder tb) {
        this.powerRelation = powerRelation;
        this.tb = tb;
    }

    @Override
    public Iterator<Term> generate(RuleApp app, PosInOccurrence pos, Goal goal,
            MutableState mState) {
        final Services services = goal.proof().getServices();
        final IntLDT numbers = services.getLDTs().getIntLDT();

        final Term powerRel = powerRelation.toTerm(app, pos, goal, mState);

        final var op = powerRel.op();

        assert op.arity() == 2;

        final BigInteger lit = new BigInteger(
            AbstractTermTransformer.convertToDecimalString(powerRel.sub(1), services));

        final Monomial mon = Monomial.create(powerRel.sub(0), services);
        final int pow = mon.getParts().size();
        if (pow <= 1 || !mon.getCoefficient().equals(one)) {
            return emptyIterator();
        }

        final Term var = mon.getParts().head();
        if (!mon.getParts().removeAll(var).isEmpty()) {
            return emptyIterator();
        }

        if (op == numbers.getLessOrEquals()) {
            return toIterator(breakDownLeq(var, lit, pow));
        } else if (op == numbers.getGreaterOrEquals()) {
            return toIterator(breakDownGeq(var, lit, pow));
        } else if (op == Equality.EQUALS) {
            return toIterator(breakDownEq(var, lit, pow));
        }

        return emptyIterator();
    }

    private Iterator<Term> emptyIterator() {
        return ImmutableSLList.<Term>nil().iterator();
    }

    private Iterator<Term> toIterator(Term res) {
        if (res.equals(tb.ff())) {
            return emptyIterator();
        }
        return ImmutableSLList.<Term>nil().prepend(res).iterator();
    }

    private Term breakDownEq(Term var, BigInteger lit, int pow) {
        final var zero = tb.zero();

        if ((pow % 2 == 0)) {
            // the even case
            return switch (lit.signum()) {
                case -1 -> // no solutions
                    tb.ff();
                case 0 -> // exactly one solution
                    tb.equals(var, zero);
                case 1 -> {
                    final BigInteger r = root(lit, pow);
                    if (power(r, pow).equals(lit)) {
                        // two solutions
                        final var rTerm = tb.zTerm(r.toString());
                        final var rNegTerm = tb.zTerm(r.negate().toString());
                        yield tb.or(tb.or(tb.lt(var, rNegTerm), tb.gt(var, rTerm)),
                            tb.and(tb.gt(var, rNegTerm), tb.lt(var, rTerm)));
                    } else {
                        // no solution
                        yield tb.ff();
                    }
                }
                default -> null;
            };
        } else {
            // the odd case
            final BigInteger r = root(lit, pow);
            if (power(r, pow).equals(lit)) {
                // one solution
                final var rTerm = tb.zTerm(r.toString());
                return tb.equals(var, rTerm);
            } else {
                // no solution
                return tb.ff();
            }
        }
    }

    private Term breakDownGeq(Term var, BigInteger lit, int pow) {
        if ((pow % 2 == 0)) {
            // the even case

            return switch (lit.signum()) {
                case -1, 0 -> // the inequation is no restriction
                    tb.ff();
                case 1 -> {
                    final BigInteger r = rootRoundingUpwards(lit, pow);
                    final var rTerm = tb.zTerm(r.toString());
                    final var rNegTerm = tb.zTerm(r.negate().toString());
                    yield tb.or(tb.leq(var, rNegTerm), tb.geq(var, rTerm));
                }
                default -> throw new IllegalStateException("Unexpected value: " + lit.signum());
            };
        } else {
            // the odd case
            return tb.geq(var, tb.zTerm(rootRoundingUpwards(lit, pow).toString()));
        }
    }

    private Term breakDownLeq(Term var, BigInteger lit, int pow) {
        if ((pow % 2 == 0)) {
            // the even case

            return switch (lit.signum()) {
                case -1 -> // no solutions
                    tb.ff();
                case 0 -> tb.equals(var, tb.zero());
                case 1 -> {
                    final BigInteger r = root(lit, pow);
                    final var rTerm = tb.zTerm(r.toString());
                    final var rNegTerm = tb.zTerm(r.negate().toString());
                    yield tb.and(tb.geq(var, rNegTerm), tb.leq(var, rTerm));
                }
                default -> throw new IllegalStateException("Unexpected value: " + lit.signum());
            };
        } else {
            // the odd case
            return tb.leq(var, tb.zTerm(root(lit, pow).toString()));
        }
    }

    /// @return a number <tt>res</tt> with the property <tt>prod in ((res-1)^exp, res^exp]</tt>
    private BigInteger rootRoundingUpwards(BigInteger prod, int exp) {
        final BigInteger res = root(prod, exp);
        if (power(res, exp).compareTo(prod) < 0) {
            return res.add(one);
        }
        return res;
    }

    /// @return a number <tt>res</tt> with the property <tt>prod in [res^exp, (res+1)^exp)</tt>
    private BigInteger root(BigInteger prod, int exp) {
        assert exp > 0;

        if (prod.signum() >= 0) {
            return posRoot(prod, exp);
        } else {
            assert exp % 2 != 0;

            BigInteger res = posRoot(prod.abs(), exp).negate();
            while (power(res, exp).compareTo(prod) > 0) {
                res = res.subtract(one);
            }

            return res;
        }
    }

    private BigInteger posRoot(BigInteger prod, int exp) {
        assert exp > 0;
        assert prod.signum() >= 0;

        // binary search for finding the root

        BigInteger lb = BigInteger.ZERO;
        BigInteger ub = prod;
        while (!power(lb, exp).equals(prod) && ub.subtract(lb).compareTo(one) > 0) {
            final BigInteger mid = ub.add(lb).divide(two);
            if (power(mid, exp).compareTo(prod) <= 0) {
                lb = mid;
            } else {
                ub = mid;
            }
        }
        return lb;
    }

    private BigInteger power(BigInteger base, int exp) {
        assert exp >= 0;

        // shift-multiplier

        BigInteger res = BigInteger.ONE;
        while (true) {
            if (exp % 2 != 0) {
                res = res.multiply(base);
            }

            exp >>= 1;
            if (exp == 0) {
                return res;
            }

            base = base.multiply(base);
        }
    }
}
