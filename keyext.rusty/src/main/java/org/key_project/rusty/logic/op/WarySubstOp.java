/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.op;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.rusty.logic.Subst;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.WaryClashFreeSubst;

/// Wary first-order substitution operator (de Bruijn). Like [SubstOp] it replaces the bound
/// variable by the substituted term, but it does **not** push a *non-rigid* term across a modality,
/// behind an update, or into a transformer — doing so would be unsound, because the value of a
/// non-rigid term (e.g. a program variable) depends on the state.
///
/// This mirrors KeY-Java's `WarySubstOp` / `WaryClashFreeSubst`, adapted to de Bruijn indices.
/// Capture avoidance is automatic under de Bruijn, so the only "wary" concern left is state
/// dependence: when the substituted term is non-rigid and the variable occurs below a protected
/// operator, the substitution is left unreduced (a residual `{\subst …}` term). The `subst_to_eq`
/// rule can then introduce a fresh rigid Skolem constant capturing the term's current value, after
/// which the (now rigid) substitution is pushed through soundly.
public final class WarySubstOp extends SubstOp {

    /// the wary substitution operator; the one actually used by the parser
    public static final SubstOp SUBST = new WarySubstOp(new Name("subst"));

    private WarySubstOp(Name name) {
        super(name);
    }

    @Override
    public Term apply(Term term, TermBuilder tb) {
        final Term substTerm = term.sub(0);
        final Term origTerm = term.sub(1);
        if (substTerm.isRigid()) {
            // a rigid term may be pushed anywhere
            return new Subst(substTerm, tb).apply(origTerm);
        }
        // a non-rigid term is pushed into ordinary (state-independent) positions, e.g. the update
        // of an update application, but NOT across a modality / behind an update / into a
        // transformer; occurrences there are kept and wrapped in a residual substitution
        final WaryClashFreeSubst cfs = new WaryClashFreeSubst(substTerm, tb);
        final Term res = cfs.apply(origTerm);
        if (cfs.wasBlocked()) {
            return tb.subst(this, term.varsBoundHere(1).get(0), substTerm, res);
        }
        return res;
    }
}
