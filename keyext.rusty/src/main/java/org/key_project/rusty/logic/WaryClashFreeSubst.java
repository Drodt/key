/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic;

import org.key_project.logic.Term;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.logic.op.TermTransformer;
import org.key_project.rusty.logic.op.UpdateApplication;
import org.key_project.util.collection.ImmutableArray;

/// De Bruijn substitution that does **not** push the replacement into "state-dependent" positions
/// — the post-formula of a modality, the target of an update application, or an operand of a
/// transformer. Occurrences in such positions are kept (and [#wasBlocked] is set), so the caller
/// can wrap the result in a residual substitution; occurrences elsewhere are substituted normally.
///
/// Used by [org.key_project.rusty.logic.op.WarySubstOp] for non-rigid replacements. The
/// replacement is assumed ground (no free logic variables, e.g. a program variable), so no de
/// Bruijn shifting of the replacement is needed.
public class WaryClashFreeSubst {
    private final Term s;
    private final TermBuilder tb;
    private boolean blocked = false;

    public WaryClashFreeSubst(Term s, TermBuilder tb) {
        this.s = s;
        this.tb = tb;
    }

    /// Whether at least one occurrence was left unsubstituted because it sat below a protected
    /// operator.
    public boolean wasBlocked() {
        return blocked;
    }

    public Term apply(Term body) {
        return apply1(body, 1, false);
    }

    private Term apply1(Term t, int index, boolean below) {
        if (t.op() instanceof LogicVariable lv && lv.getIndex() == index) {
            if (below) {
                // keep the variable below a modality/update/transformer; the caller re-wraps the
                // substitution as a residual
                blocked = true;
                return t;
            }
            return s;
        }
        final int arity = t.arity();
        final Term[] newSubterms = new Term[arity];
        for (int i = 0; i < arity; i++) {
            int subIndex = index;
            if (t.op().bindVarsAt(i)) {
                subIndex += t.varsBoundHere(i).size();
            }
            newSubterms[i] = apply1(t.sub(i), subIndex, below || isProtected(t.op()));
        }
        return tb.tf().createTerm(t.op(), newSubterms,
            (ImmutableArray<QuantifiableVariable>) t.boundVars());
    }

    /// Whether the subterms of an `op`-rooted term are state dependent: a modality, an update
    /// application, or a transformer. This is conservative for updates — the replacement is not
    /// pushed into an update application at all (neither into the update nor behind it); the
    /// update-simplification taclets take care of propagating into the update where sound.
    static boolean isProtected(Operator op) {
        return op instanceof RModality
                || op == UpdateApplication.UPDATE_APPLICATION
                || op instanceof TermTransformer;
    }
}
