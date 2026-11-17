/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.rule.metaconstruct.arith;

import java.math.BigInteger;
import java.util.Iterator;

import org.key_project.logic.LogicServices;
import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.prover.strategy.costbased.feature.helper.arith.Monomial;
import org.key_project.rusty.Services;
import org.key_project.rusty.ldt.IntLDT;
import org.key_project.rusty.logic.op.AbstractTermTransformer;
import org.key_project.util.ConcurrentLruCache;
import org.key_project.util.collection.ImmutableList;

/// Class for analyzing and modifying monomial expressions over the integers
public class RMonomial {
    private RMonomial() {}

    public static Monomial create(Term monoTerm, LogicServices p_services) {
        final Services services = (Services) p_services;
        final ConcurrentLruCache<Term, Monomial> monomialCache =
            services.getCaches().getMonomialCache();
        Monomial res;

        synchronized (monomialCache) {
            res = monomialCache.get(monoTerm);
        }

        if (res == null) {
            res = createHelp(monoTerm, services);
            synchronized (monomialCache) {
                monomialCache.put(monoTerm, res);
            }
        }
        return res;
    }

    public static Term toTerm(Monomial m, Services services) {
        final Operator mul = services.getLDTs().getIntLDT().getMul();
        Term res = null;

        final Iterator<Term> it = m.getParts().iterator();
        if (it.hasNext()) {
            res = it.next();
            while (it.hasNext()) {
                res = services.getTermFactory().createTerm(mul, res,
                    it.next());
            }
        }

        final var cTerm = services.getTermBuilder().zTerm(m.getCoefficient().toString());

        if (res == null) {
            res = cTerm;
        } else if (!BigInteger.ONE.equals(m.getCoefficient())) {
            res = services.getTermFactory().createTerm(mul,
                res, cTerm);
        }

        return res;
    }

    private static Monomial createHelp(Term monomial, Services services) {
        final Analyser a = new Analyser(services);
        a.analyse(monomial);
        return new Monomial(a.parts, a.coeff);
    }

    private static class Analyser {
        public BigInteger coeff = BigInteger.ONE;
        public ImmutableList<Term> parts = ImmutableList.nil();
        private final Services services;
        private final Operator numbers, mul;

        public Analyser(final Services services) {
            this.services = services;
            final IntLDT integerLDT = services.getLDTs().getIntLDT();
            numbers = integerLDT.getNumberSymbol();
            mul = integerLDT.getMul();
        }

        public void analyse(Term monomial) {
            if (monomial.op() == mul) {
                analyse(monomial.sub(0));
                analyse(monomial.sub(1));
            } else if (monomial.op() == numbers) {
                final BigInteger c = new BigInteger(
                    AbstractTermTransformer.convertToDecimalString(monomial, services));
                coeff = coeff.multiply(c);
            } else {
                parts = parts.prepend(monomial);
            }
        }
    }
}
