/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.prover.sequent;

import org.key_project.util.collection.ImmutableList;
import org.key_project.util.collection.ImmutableSLList;

public class SemisequentChangeInfo {
    /// contains the added formulas to the semisequent
    private ImmutableList<SequentFormula> added = ImmutableSLList.nil();
    /// contains the removed formulas from the semisequent
    private ImmutableList<SequentFormula> removed = ImmutableSLList.nil();
    /// contains the modified formulas from the semisequent
    private ImmutableList<FormulaChangeInfo> modified = ImmutableSLList.nil();
    /// stores the redundance free formula list of the semisequent
    private ImmutableList<SequentFormula> modifiedSemisequent = ImmutableSLList.nil();
    /// contains formulas that have been tried to add, but which have been rejected due to already
    /// existing formulas in the sequent subsuming these formulas
    private ImmutableList<SequentFormula> rejected = ImmutableSLList.nil();

    ///
    private int lastFormulaIndex = -1;

    public SemisequentChangeInfo() {
    }

    public SemisequentChangeInfo(ImmutableList<SequentFormula> formulas) {
        this.modifiedSemisequent = formulas;
    }

    protected SemisequentChangeInfo(SemisequentChangeInfo o) {
        this.added = o.added;
        this.removed = o.removed;
        this.modified = o.modified;
        this.modifiedSemisequent = o.modifiedSemisequent;
        this.rejected = o.rejected;
        this.lastFormulaIndex = o.lastFormulaIndex;
    }

    public SemisequentChangeInfo copy() {
        return new SemisequentChangeInfo(this);
    }

    protected ImmutableList<SequentFormula> modifiedSemisequent() {
        return modifiedSemisequent;
    }

    /// returns true if the semisequent has changed
    public boolean hasChanged() {
        return !added.isEmpty() || !removed.isEmpty() || !modified.isEmpty();
    }

    /// sets the list of constrained formula containing all formulas of the semisequent after the
    /// operation
    public void setFormulaList(ImmutableList<SequentFormula> list) {
        modifiedSemisequent = list;
    }

    /// returns the list of constrained formula of the new semisequent
    public ImmutableList<SequentFormula> getFormulaList() {
        return modifiedSemisequent;
    }

    /// logs an added formula at position idx
    public void addedFormula(int idx, SequentFormula cf) {
        added = added.prepend(cf);
        lastFormulaIndex = idx;
    }

    /// logs a modified formula at position idx
    public void modifiedFormula(int idx, FormulaChangeInfo fci) {
        // This information can overwrite older records about removed
        // formulas
        removed = removed.removeAll(fci.positionOfModification().sequentFormula());
        modified = modified.prepend(fci);
        lastFormulaIndex = idx;
    }

    /// returns the list of all added constrained formulas
    ///
    /// @return IList<SequentFormula> added to the semisequent
    public ImmutableList<SequentFormula> addedFormulas() {
        return added;
    }

    /// returns the list of all removed constrained formulas
    ///
    /// @return IList<SequentFormula> removed from the semisequent
    public ImmutableList<SequentFormula> removedFormulas() {
        return removed;
    }

    /// returns a list of formulas that have been tried to add to the semisequent but got rejected
    /// as
    /// they were redundant
    ///
    /// @return list of formulas rejected due to redundancy
    public ImmutableList<SequentFormula> rejectedFormulas() {
        return this.rejected;
    }


    /// adding formula <tt>f</tt> to the semisequent failed due to a redundance check. This means an
    /// equal or stronger formula is already present in the semisequent
    ///
    /// @param f the SequentFormula
    public void rejectedFormula(SequentFormula f) {
        this.rejected = this.rejected.append(f);
    }

    /// returns the list of all modification positions
    ///
    /// @return IList<SequentFormula> modified within the semisequent
    public ImmutableList<FormulaChangeInfo> modifiedFormulas() {
        return modified;
    }

    /// logs an added formula at position idx
    public void removedFormula(int idx, SequentFormula cf) {
        removed = removed.prepend(cf);

        lastFormulaIndex = (lastFormulaIndex == idx) ? -1
                : lastFormulaIndex > idx ? lastFormulaIndex - 1 : lastFormulaIndex;

        if (lastFormulaIndex < -1) {
            lastFormulaIndex = -1;
        }

    }

    /// This method combines this change information from this info and its successor. ATTENTION: it
    /// takes over ownership over <code>succ</code> and does not release it. This means when
    /// invoking the method it must be snsured that succ is never used afterwards.
    ///
    /// @param succ the SemisequentChangeInfo to combine with
    public void combine(SemisequentChangeInfo succ) {
        final SemisequentChangeInfo predecessor = this;
        if (succ == predecessor) {
            return;
        }

        for (var sf : succ.removed) {
            predecessor.added = predecessor.added.removeAll(sf);

            boolean skip = false;
            for (var fci : predecessor.modified) {
                if (fci.newFormula() == sf) {
                    predecessor.modified = predecessor.modified.removeAll(fci);
                    if (!predecessor.removed.contains(fci.getOriginalFormula())) {
                        predecessor.removed =
                            predecessor.removed.append(fci.getOriginalFormula());
                    }
                    skip = true;
                    break;
                }
            }
            if (!skip) {
                predecessor.removedFormula(succ.lastFormulaIndex, sf);
            }
        }

        for (var fci : succ.modified) {
            if (predecessor.addedFormulas().contains(fci.getOriginalFormula())) {
                predecessor.added = predecessor.added.removeAll(fci.getOriginalFormula());
                predecessor.addedFormula(succ.lastFormulaIndex, fci.newFormula());
            } else {
                predecessor.modifiedFormula(succ.lastFormulaIndex, fci);
            }
        }

        for (SequentFormula sf : succ.added) {
            predecessor.removed = predecessor.removed.removeAll(sf);
            if (!predecessor.added.contains(sf)) {
                predecessor.addedFormula(succ.lastFormulaIndex, sf);
            }
        }

        for (SequentFormula sf : succ.rejected) {
            if (!predecessor.rejected.contains(sf)) {
                predecessor.rejectedFormula(sf);
            }
        }

        predecessor.lastFormulaIndex = succ.lastFormulaIndex;
        predecessor.modifiedSemisequent = succ.modifiedSemisequent;
    }

    /// returns the index of the last added formula
    public int getIndex() {
        return lastFormulaIndex;
    }

    /// returns the semisequent that is the result of the change operation
    // public Semisequent semisequent() {
    // final Semisequent semisequent;
    // if (modifiedSemisequent().isEmpty()) {
    // semisequent = SequentKit.emptySemisequent();
    // } else {
    // semisequent = new Semisequent(modifiedSemisequent());
    // }
    // return semisequent;
    // }

    /// toString
    public String toString() {
        return "changed:" + hasChanged() + "\n  added (pos):" + added + "(" + lastFormulaIndex + ")"
            + "\n  removed:" + removed + "\n  modified:" + modified + "\n  rejected:" + rejected
            + "\n  new semisequent:" + modifiedSemisequent;
    }
}
