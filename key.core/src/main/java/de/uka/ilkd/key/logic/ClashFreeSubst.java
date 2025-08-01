/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.logic;

import de.uka.ilkd.key.logic.op.LogicVariable;

import org.key_project.logic.Name;
import org.key_project.logic.Term;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableSet;

public class ClashFreeSubst {
    protected final QuantifiableVariable v;
    protected final JTerm s;
    protected final ImmutableSet<QuantifiableVariable> svars;
    protected final TermBuilder tb;

    public ClashFreeSubst(QuantifiableVariable v, JTerm s, TermBuilder tb) {
        this.v = v;
        this.s = s;
        this.tb = tb;
        svars = s.freeVars();
    }

    protected QuantifiableVariable getVariable() {
        return v;
    }

    protected JTerm getSubstitutedTerm() {
        return s;
    }

    /**
     * substitute <code>s</code> for <code>v</code> in <code>t</code>, avoiding collisions by
     * replacing bound variables in <code>t</code> if necessary.
     */
    public JTerm apply(JTerm t) {
        if (!t.freeVars().contains(v)) {
            return t;
        } else {
            return apply1(t);
        }
    }

    /**
     * substitute <code>s</code> for <code>v</code> in <code>t</code>, avoiding collisions by
     * replacing bound variables in <code>t</code> if necessary. It is assumed, that <code>t</code>
     * contains a free occurrence of <code>v</code>.
     */
    protected JTerm apply1(JTerm t) {
        if (t.op() == v) {
            return s;
        } else {
            return applyOnSubterms(t);
        }
    }

    // XXX
    protected static ImmutableArray<QuantifiableVariable> getSingleArray(
            ImmutableArray<QuantifiableVariable>[] bv) {
        if (bv == null) {
            return null;
        }
        ImmutableArray<QuantifiableVariable> result = null;
        for (ImmutableArray<QuantifiableVariable> arr : bv) {
            if (arr != null && !arr.isEmpty()) {
                if (result == null) {
                    result = arr;
                } else {
                    assert arr.equals(result) : "expected: " + result + "\nfound: " + arr;
                }
            }
        }
        return result;
    }

    /**
     * substitute <code>s</code> for <code>v</code> in every subterm of <code>t</code>, and build a
     * new term. It is assumed, that one of the subterms contains a free occurrence of
     * <code>v</code>, and that the case <code>v==t<code> is already handled.
     */
    private JTerm applyOnSubterms(JTerm t) {
        final int arity = t.arity();
        final JTerm[] newSubterms = new JTerm[arity];
        @SuppressWarnings("unchecked")
        final ImmutableArray<QuantifiableVariable>[] newBoundVars = new ImmutableArray[arity];
        for (int i = 0; i < arity; i++) {
            applyOnSubterm(t, i, newSubterms, newBoundVars);
        }
        return tb.tf().createTerm(t.op(), newSubterms, getSingleArray(newBoundVars), t.getLabels());
    }

    /**
     * Apply the substitution of the subterm <code>subtermIndex</code> of term/formula
     * <code>completeTerm</code>. The result is stored in <code>newSubterms</code> and
     * <code>newBoundVars</code> (at index <code>subtermIndex</code>)
     */
    protected void applyOnSubterm(JTerm completeTerm, int subtermIndex, JTerm[] newSubterms,
            ImmutableArray<QuantifiableVariable>[] newBoundVars) {
        if (subTermChanges(completeTerm.varsBoundHere(subtermIndex),
            completeTerm.sub(subtermIndex))) {
            final QuantifiableVariable[] nbv =
                new QuantifiableVariable[completeTerm.varsBoundHere(subtermIndex).size()];
            applyOnSubterm(0, completeTerm.varsBoundHere(subtermIndex), nbv, subtermIndex,
                completeTerm.sub(subtermIndex), newSubterms);
            newBoundVars[subtermIndex] = new ImmutableArray<>(nbv);
        } else {
            newBoundVars[subtermIndex] = completeTerm.varsBoundHere(subtermIndex);
            newSubterms[subtermIndex] = completeTerm.sub(subtermIndex);
        }
    }

    /**
     * Perform the substitution on <code>subTerm</code> bound by the variables in
     * <code>boundVars</code>, starting with the variable at index <code>varInd</code>. Put the
     * resulting bound variables (which might be new) into <code>newBoundVars</code>, starting from
     * position <code>varInd</code>, and the resulting subTerm into
     * <code>newSubterms[subInd]</code>.
     * <P>
     * It is assumed that <code>v</code> occurrs free in in this quantified subterm, i.e. it occurrs
     * free in <code>subTerm</code>, but does not occurr in <code>boundVars</code> from
     * <code>varInd</code> upwards..
     */
    private void applyOnSubterm(int varInd, ImmutableArray<QuantifiableVariable> boundVars,
            QuantifiableVariable[] newBoundVars, int subInd, JTerm subTerm, JTerm[] newSubterms) {
        if (varInd >= boundVars.size()) {
            newSubterms[subInd] = apply1(subTerm);
        } else {
            QuantifiableVariable qv = boundVars.get(varInd);
            if (svars.contains(qv)) {
                /* Here is the clash case all this is about! Hurrah! */

                // Determine Variable names to avoid
                VariableCollectVisitor vcv = new VariableCollectVisitor();
                ImmutableSet<QuantifiableVariable> usedVars;
                subTerm.execPostOrder(vcv);
                usedVars = svars;
                usedVars = usedVars.union(vcv.vars());
                for (int i = varInd + 1; i < boundVars.size(); i++) {
                    usedVars = usedVars.add(boundVars.get(i));
                }
                // Get a new variable with a fitting name.
                LogicVariable qv1 = newVarFor(qv, usedVars);

                // Substitute that for the old one.
                newBoundVars[varInd] = qv1;
                new ClashFreeSubst(qv, tb.var(qv1), tb).applyOnSubterm1(varInd + 1, boundVars,
                    newBoundVars, subInd, subTerm, newSubterms);
                // then continue recursively, on the result.
                applyOnSubterm(varInd + 1, new ImmutableArray<>(newBoundVars),
                    newBoundVars, subInd, newSubterms[subInd], newSubterms);
            } else {
                newBoundVars[varInd] = qv;
                applyOnSubterm(varInd + 1, boundVars, newBoundVars, subInd, subTerm, newSubterms);
            }
        }
    }

    /**
     * Same as applyOnSubterm, but v doesn't have to occurr free in the considered quantified
     * subterm. It is however assumed that no more clash can occurr.
     */
    private void applyOnSubterm1(int varInd, ImmutableArray<QuantifiableVariable> boundVars,
            QuantifiableVariable[] newBoundVars, int subInd, JTerm subTerm, JTerm[] newSubterms) {
        if (varInd >= boundVars.size()) {
            newSubterms[subInd] = apply(subTerm);
        } else {
            QuantifiableVariable qv = boundVars.get(varInd);
            newBoundVars[varInd] = qv;
            if (qv == v) {
                newSubterms[subInd] = subTerm;
                for (int i = varInd; i < boundVars.size(); i++) {
                    newBoundVars[i] = boundVars.get(varInd);
                }
            } else {
                applyOnSubterm1(varInd + 1, boundVars, newBoundVars, subInd, subTerm, newSubterms);
            }
        }
    }

    /**
     * returns true if <code>subTerm</code> bound by <code>boundVars</code> would change under
     * application of this substitution. This is the case, if <code>v</code> occurrs free in
     * <code>subTerm</code>, but does not occurr in <code>boundVars</code>.
     *
     * @returns true if <code>subTerm</code> bound by <code>boundVars</code> would change under
     *          application of this substitution
     */
    protected boolean subTermChanges(ImmutableArray<QuantifiableVariable> boundVars,
            JTerm subTerm) {
        if (!subTerm.freeVars().contains(v)) {
            return false;
        } else {
            for (int i = 0; i < boundVars.size(); i++) {
                if (v == boundVars.get(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * returns a new variable that has a name derived from that of <code>var</code>, that is
     * different from any of the names of variables in <code>usedVars</code>.
     * <P>
     * Assumes that <code>var</code> is a @link{LogicVariable}.
     */
    protected LogicVariable newVarFor(QuantifiableVariable var,
            ImmutableSet<QuantifiableVariable> usedVars) {
        LogicVariable lv = (LogicVariable) var;
        String stem = var.name().toString();
        int i = 1;
        while (!nameNewInSet((stem + i), usedVars)) {
            i++;
        }
        return new LogicVariable(new Name(stem + i), lv.sort());
    }

    /**
     * returns true if there is no object named <code>n</code> in the set <code>s</code>
     */
    private boolean nameNewInSet(String n, ImmutableSet<QuantifiableVariable> qvars) {
        for (QuantifiableVariable qvar : qvars) {
            if (qvar.name().toString().equals(n)) {
                return false;
            }
        }
        return true;
    }

    // This helper is used in other places as well. Perhaps make it toplevel one
    // day.
    /**
     * A Visitor class to collect all (not just the free) variables occurring in a term.
     */
    public static class VariableCollectVisitor implements DefaultVisitor {
        /** the collected variables */
        private ImmutableSet<QuantifiableVariable> vars;

        /** creates the Variable collector */
        public VariableCollectVisitor() {
            vars = DefaultImmutableSet.nil();
        }

        @Override
        public void visit(Term t) {
            if (t.op() instanceof QuantifiableVariable qv) {
                vars = vars.add(qv);
            } else {
                for (int i = 0; i < t.arity(); i++) {
                    var vbh = t.varsBoundHere(i);
                    for (int j = 0; j < vbh.size(); j++) {
                        vars = vars.add(vbh.get(j));
                    }
                }
            }
        }

        /** the set of all occurring variables. */
        public ImmutableSet<QuantifiableVariable> vars() {
            return vars;
        }
    }

}
