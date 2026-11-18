/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.strategy.quantifierHeuristics.theory;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.logic.op.Operator;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Polynomial;
import org.key_project.rusty.ServiceCaches;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.op.Equality;
import org.key_project.rusty.logic.op.Junctor;
import org.key_project.rusty.rule.metaconstruct.arith.RPolynomial;
import org.key_project.util.GenerationalCache;
import org.key_project.util.collection.Pair;


/// This class is used to prove some simple arithmetic problem which are `a==b`, `a>=b`,
/// `a<=b`; Besides it can be used to prove that `a>=b` or `a<=b` by
/// knowing that `c>=d` or `c<=d;`
class HandleArith {
    private HandleArith() {}

    /// The operator of `t` after stripping leading negations.
    private static Operator strippedOp(Term t) {
        Operator op = t.op();
        while (op == Junctor.NOT) {
            t = t.sub(0);
            op = t.op();
        }
        return op;
    }

    /// Whether `t` (ignoring leading negations) is an integer `>=` or `<=`.
    private static boolean isArithComparison(Term t, IntLDT ig) {
        final Operator op = strippedOp(t);
        return op == ig.getGreaterOrEquals() || op == ig.getLessOrEquals()
                || op == ig.getLessThan() || op == ig.getGreaterThan();
    }

    /// try to prove atom by using polynomial
    ///
    /// @param problem
    /// @return `trueT` if formula is proved to true, `falseT` if false, and
    /// `problem` if it can't be proved.
    public static Term provedByArith(Term problem, Services services) {
        return provedByArith(problem, services.getLDTs().getIntLDT(), services);
    }

    /// The variant of [#provedByArith(Term, Services)] taking the integer theory, which
    /// one decision consults several times.
    static Term provedByArith(Term problem, IntLDT integerLDT, Services services) {
        if (!isArithComparison(problem, integerLDT) && strippedOp(problem) != Equality.EQUALS) {
            // neither an (in)equality nor an equality: formatArithTerm yields false and
            // provedArithEqual returns the problem unchanged -- bail before locking the cache.
            return problem;
        }
        final GenerationalCache<Term, Term> provedByArithCache =
            services.getCaches().getProvedByArithFstCache();
        // ConcurrentLruCache is internally synchronized; get/put are individually atomic, so no
        // external lock is needed (the get and put here are already separate critical sections --
        // two racing misses may both compute and put, last write wins, exactly as before).
        Term result = provedByArithCache.get(problem);
        if (result != null) {
            return result;
        }

        TermBuilder tb = services.getTermBuilder();

        final Term trueT = tb.tt();
        final Term falseT = tb.ff();

        final Term arithTerm = formatArithTerm(problem, tb, integerLDT, services.getCaches());
        if (arithTerm.equals(falseT)) {
            result = provedArithEqual(problem, tb, services);
            putInTermCache(provedByArithCache, problem, result);
            return result;
        }
        Polynomial poly1 = RPolynomial.create(arithTerm.sub(0), services);
        Polynomial poly2 = RPolynomial.create(arithTerm.sub(1), services);

        if (poly2.valueLeq(poly1)) {
            putInTermCache(provedByArithCache, problem, trueT);
            return trueT;
        }
        if (poly1.valueLess(poly2)) {
            putInTermCache(provedByArithCache, problem, falseT);
            return falseT;
        }
        putInTermCache(provedByArithCache, problem, problem);
        return problem;
    }

    private static void putInTermCache(final GenerationalCache<Term, Term> provedByArithCache,
            final Term key, final Term value) {
        provedByArithCache.putIfAbsent(key, value);
    }

    /// @param problem
    /// @return true if atom.sub(0) is euqual to atom.sub(1), false if not equal, else return atom
    private static Term provedArithEqual(Term problem, TermBuilder tb, Services services) {
        final Term trueT = tb.tt();
        final Term falseT = tb.ff();

        boolean temp = true;
        Term pro = problem;
        Operator op = pro.op();
        // may be here we should check wehre sub0 and sub1 is integer.
        while (op == Junctor.NOT) {
            pro = pro.sub(0);
            op = pro.op();
            temp = !temp;
        }
        if (op == Equality.EQUALS) {
            Term sub0 = pro.sub(0);
            Term sub1 = pro.sub(1);
            Polynomial poly1 = RPolynomial.create(sub0, services);
            Polynomial poly2 = RPolynomial.create(sub1, services);
            boolean gt = poly2.valueLeq(poly1);
            boolean lt = poly1.valueLeq(poly2);
            if (gt && lt) {
                return temp ? trueT : falseT;
            }
            if (gt || lt) {
                return temp ? falseT : trueT;
            }
        }
        return problem;
    }

    /// Try to prove problem by know that axiom is true. The idea is that we know a>=b(axiom),we
    /// want
    /// to prove c>=d(problem). It is enough to prove that c+b>=a+d which means c>=d is true. or we
    /// prove that !(c>=d) which is d>=c+1 is true. This means c>= d is false;
    ///
    /// @param problem
    /// @param axiom
    /// @return trueT if true, falseT if false, and atom if it can't be proven;
    public static Term provedByArith(Term problem, Term axiom, Services services) {
        return provedByArith(problem, axiom, services.getLDTs().getIntLDT(),
            services);
    }

    /// The variant of [#provedByArith(Term, Term, Services)] taking the integer theory,
    /// which one decision consults several times.
    static Term provedByArith(Term problem, Term axiom, IntLDT integerLDT,
            Services services) {
        if (!isArithComparison(problem, integerLDT) || !isArithComparison(axiom, integerLDT)) {
            // not an arithmetic implication: formatArithTerm would yield false for one side and
            // this method returns the unproved problem -- bail before allocating the key and
            // taking the cache lock.
            return problem;
        }
        final Pair<Term, Term> key = new Pair<>(problem, axiom);
        final GenerationalCache<Pair<Term, Term>, Term> provedByArithCache =
            services.getCaches().getProvedByArithSndCache();
        Term result = provedByArithCache.get(key);
        if (result != null) {
            return result;
        }

        final TermBuilder tb = services.getTermBuilder();
        final ServiceCaches caches = services.getCaches();

        final Term cd = formatArithTerm(problem, tb, integerLDT, caches);
        final Term ab = formatArithTerm(axiom, tb, integerLDT, caches);
        final Term trueT = tb.tt();
        final Term falseT = tb.ff();

        if (cd.op() == Junctor.FALSE || ab.op() == Junctor.FALSE) {
            provedByArithCache.putIfAbsent(key, problem);
            return problem;
        }
        Function addfun = integerLDT.getAdd();
        Term arithTerm =
            tb.geq(tb.func(addfun, cd.sub(0), ab.sub(1)), tb.func(addfun, ab.sub(0), cd.sub(1)));
        Term res = provedByArith(arithTerm, services);
        if (res.op() == Junctor.TRUE) {
            provedByArithCache.putIfAbsent(key, trueT);
            return trueT;
        }
        Term t0 = formatArithTerm(tb.not(problem), tb, integerLDT, caches);
        arithTerm =
            tb.geq(tb.func(addfun, t0.sub(0), ab.sub(1)), tb.func(addfun, ab.sub(0), t0.sub(1)));
        res = provedByArith(arithTerm, services);
        if (res.op() == Junctor.TRUE) {
            provedByArithCache.putIfAbsent(key, falseT);
            return falseT;
        }
        provedByArithCache.putIfAbsent(key, problem);
        return problem;
    }

    /// Format literal to a form of "geq",linke a>=b;For example, a <=b to b>=a; a>b to a>=b+1;
    /// !(a>=b)
    /// to b>=a+1.
    ///
    /// @param problem
    /// @return falseT if `term`'s operator is not >= or <=
    private static Term formatArithTerm(final Term problem, TermBuilder tb, IntLDT ig,
            ServiceCaches caches) {
        final GenerationalCache<Term, Term> formattedTermCache = caches.getFormattedTermCache();
        Term pro = formattedTermCache.get(problem);
        if (pro != null) {
            return pro;
        }

        pro = problem;
        Operator op = pro.op();
        boolean opNot = false;
        while (op == Junctor.NOT) {
            opNot = !opNot;
            pro = pro.sub(0);
            op = pro.op();
        }
        final Function geq = ig.getGreaterOrEquals();
        final Function leq = ig.getLessOrEquals();
        final Function lt = ig.getLessThan();
        final Function gt = ig.getGreaterThan();
        final Function add = ig.getAdd();
        final Term falseT = tb.ff();

        // A strict comparison is rewritten to a non-strict one over the integers (a < b becomes
        // a <= b - 1), so that all four comparisons reduce to the same >= form and can be
        // compared as polynomials. sub(0) and sub(1) are the two compared terms.
        if (op == geq) {
            if (opNot) {
                pro = tb.geq(pro.sub(1), tb.func(add, pro.sub(0), ig.one()));
            }
        } else if (op == leq) {
            if (opNot) {
                pro = tb.geq(pro.sub(0), tb.func(add, pro.sub(1), ig.one()));
            } else {
                pro = tb.geq(pro.sub(1), pro.sub(0));
            }
        } else if (op == lt) {
            // a < b is b >= a + 1; its negation is a >= b
            if (opNot) {
                pro = tb.geq(pro.sub(0), pro.sub(1));
            } else {
                pro = tb.geq(pro.sub(1), tb.func(add, pro.sub(0), ig.one()));
            }
        } else if (op == gt) {
            // a > b is a >= b + 1; its negation is b >= a
            if (opNot) {
                pro = tb.geq(pro.sub(1), pro.sub(0));
            } else {
                pro = tb.geq(pro.sub(0), tb.func(add, pro.sub(1), ig.one()));
            }
        } else {
            pro = falseT;
        }

        putInTermCache(formattedTermCache, problem, pro);
        return pro;
    }
}
