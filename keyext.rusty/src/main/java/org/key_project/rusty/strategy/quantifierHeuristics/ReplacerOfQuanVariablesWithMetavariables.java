/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.strategy.quantifierHeuristics;

import java.util.ArrayList;
import java.util.List;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.Quantifier;
import org.key_project.rusty.logic.op.RFunction;
import org.key_project.util.collection.DefaultImmutableMap;
import org.key_project.util.collection.ImmutableMap;

import org.jspecify.annotations.NonNull;

/// This class is used to create metavariables for every universal variables in quantified formula
/// <code>allTerm</code> and create constant functions for all existential variables. The variables
/// with new created metavariables or constant functions are store to a map <code>mapQM</code>.
class ReplacerOfQuanVariablesWithMetavariables {
    private ReplacerOfQuanVariablesWithMetavariables() {}

    public static Substitution createSubstitutionForVars(Term allTerm, Services services) {
        List<Term> quants = new ArrayList<>();
        Term t = allTerm;
        var op = t.op();
        while (op instanceof Quantifier) {
            quants.add(t);
            t = t.sub(0);
            op = t.op();
        }

        ImmutableMap<@NonNull LogicVariable, Term> res =
            DefaultImmutableMap.nilMap();
        int size = quants.size();
        for (int i = 0; i < size; i++) {
            var quant = quants.get(i);
            var sort = quant.varsBoundHere(0).get(0).sort();
            Term m;
            if (quant.op() == Quantifier.ALL) {
                var mv = new Metavariable(ARBITRARY_NAME, sort);
                m = services.getTermBuilder().var(mv);
            } else {
                var f = new RFunction(ARBITRARY_NAME, sort, new Sort[0]);
                m = services.getTermBuilder().func(f);
            }
            var lv = LogicVariable.create(size - i, sort);
            res = res.put(lv, m);
        }
        return new Substitution(res);
    }

    private final static Name ARBITRARY_NAME = new Name("unifier");
}
