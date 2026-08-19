/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic;

import java.util.concurrent.atomic.AtomicInteger;

import org.key_project.logic.Term;
import org.key_project.logic.Visitor;
import org.key_project.logic.op.Modality;
import org.key_project.logic.op.Operator;
import org.key_project.logic.op.QuantifiableVariable;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.op.LogicVariable;
import org.key_project.rusty.logic.op.ParametricFunctionInstance;
import org.key_project.rusty.logic.op.RModality;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.ParametricSortInstance;
import org.key_project.rusty.logic.sort.SortArg;
import org.key_project.rusty.logic.sort.TermArg;
import org.key_project.util.Strings;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableArray;
import org.key_project.util.collection.ImmutableSet;

import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

// TODO: Basically everything here can be moved to ncore.
public class TermImpl implements Term {
    /// A static empty list of terms used for memory reasons.
    private static final ImmutableArray<Term> EMPTY_TERM_LIST = new ImmutableArray<>();

    /// A static empty list of quantifiable variables used for memory reasons.
    private static final ImmutableArray<QuantifiableVariable> EMPTY_VAR_LIST =
        new ImmutableArray<>();

    private static final AtomicInteger serialNumberCounter = new AtomicInteger();
    private final int serialNumber = serialNumberCounter.incrementAndGet();

    // content
    private final Operator op;
    private final ImmutableArray<Term> subs;
    private final ImmutableArray<QuantifiableVariable> boundVars;

    private @MonotonicNonNull Sort sort;
    private int depth = -1;

    private enum ThreeValuedTruth {
        TRUE, FALSE, UNKNOWN
    }

    /// Cached [#hashCode()] value.
    private int hashcode = -1;

    /// Cached [#nameHash()] value. `-1` = not yet computed.
    private int nameHash = -1;

    /// Cached [#labelAgnosticHash()] value. `-1` = not yet computed.
    private int labelAgnosticHash = -1;

    private int maxDebruijnIndex = -1;

    /// A cached value for computing the term's rigidness.
    private ThreeValuedTruth rigid = ThreeValuedTruth.UNKNOWN;
    private ThreeValuedTruth containsCodeBlockRecursive = ThreeValuedTruth.UNKNOWN;
    private @MonotonicNonNull ImmutableSet<LogicVariable> freeVars = null;

    /// Constructs a term for the given operator, with the given sub terms, bounded variables and
    /// (if
    /// applicable) the code block on this term.
    ///
    /// @param op the operator of the term, e.g., some arithmetic operation
    /// @param subs the sub terms of the constructed term (whose type is constrained by the used
    /// operator)
    /// @param boundVars the bounded variables (if applicable), e.g., for quantifiers
    public TermImpl(Operator op, ImmutableArray<Term> subs,
            @Nullable ImmutableArray<QuantifiableVariable> boundVars) {
        assert op != null;
        assert subs != null;
        this.op = op;
        this.subs = subs.isEmpty() ? EMPTY_TERM_LIST : subs;
        this.boundVars = boundVars == null ? EMPTY_VAR_LIST : boundVars;
    }

    // TODO Remove
    @Deprecated
    private ImmutableSet<LogicVariable> determineFreeVars() {
        ImmutableSet<LogicVariable> localFreeVars =
            DefaultImmutableSet.nil();

        if (op instanceof LogicVariable lv) {
            localFreeVars = localFreeVars.add(lv);
        } else {
            for (int i = 0, ar = arity(); i < ar; i++) {
                var subFreeVars =
                    (ImmutableSet<LogicVariable>) sub(i).freeVars();
                var sz = varsBoundHere(i).size();
                for (var fv : subFreeVars) {
                    if (fv.getIndex() > sz) {
                        localFreeVars =
                            localFreeVars.add(LogicVariable.create(fv.getIndex() - sz, fv.sort()));
                    }
                }
            }
        }
        return localFreeVars;
    }



    @Override
    public @NonNull Operator op() {
        return op;
    }


    @Override
    public <T> @NonNull T op(@NonNull Class<T> opClass) throws IllegalArgumentException {
        if (!opClass.isInstance(op)) {
            throw new IllegalArgumentException("Operator does not match the expected type:\n"
                + "Operator type was: " + op.getClass() + "\n" + "Expected type was: " + opClass);
        }
        return opClass.cast(op);
    }

    @Override
    public @NonNull ImmutableArray<Term> subs() {
        return subs;
    }


    @Override
    public @NonNull Term sub(int nr) {
        return subs.get(nr);
    }


    @Override
    public @NonNull ImmutableArray<QuantifiableVariable> boundVars() {
        return boundVars;
    }


    @Override
    public @NonNull ImmutableArray<QuantifiableVariable> varsBoundHere(int n) {
        return op.bindVarsAt(n) ? boundVars : EMPTY_VAR_LIST;
    }

    @Override
    public int arity() {
        return op.arity();
    }

    @Override
    public @NonNull Sort sort() {
        if (sort == null) {
            Sort[] sorts = new Sort[subs.size()];
            for (int i = 0; i < sorts.length; i++) {
                sorts[i] = subs.get(i).sort();
            }
            sort = op.sort(sorts);
        }
        return sort;
    }

    @Override
    public int depth() {
        if (depth == -1) {
            int localDepth = -1;
            for (int i = 0, n = arity(); i < n; i++) {
                final int subTermDepth = sub(i).depth();
                if (subTermDepth > depth) {
                    localDepth = subTermDepth;
                }
            }
            ++localDepth;
            depth = localDepth;
        }
        return depth;
    }

    @Override
    public boolean isRigid() {
        if (rigid == ThreeValuedTruth.UNKNOWN) {
            if (!op.isRigid()) {
                rigid = ThreeValuedTruth.FALSE;
            } else {
                ThreeValuedTruth localIsRigid = ThreeValuedTruth.TRUE;
                for (int i = 0, n = arity(); i < n; i++) {
                    if (!sub(i).isRigid()) {
                        localIsRigid = ThreeValuedTruth.FALSE;
                        break;
                    }
                }
                rigid = localIsRigid;
            }
        }

        return rigid == ThreeValuedTruth.TRUE;
    }

    @Override
    public ImmutableSet<LogicVariable> freeVars() {
        if (freeVars == null) {
            freeVars = determineFreeVars();
        }
        return freeVars;
    }

    @Override
    public int serialNumber() {
        return serialNumber;
    }

    @Override
    public void execPostOrder(@NonNull Visitor visitor) {
        visitor.subtreeEntered(this);
        if (visitor.visitSubtree(this)) {
            for (int i = 0, ar = arity(); i < ar; i++) {
                sub(i).execPostOrder(visitor);
            }
        }
        visitor.visit(this);
        visitor.subtreeLeft(this);
    }


    @Override
    public void execPreOrder(@NonNull Visitor visitor) {
        visitor.subtreeEntered(this);
        visitor.visit(this);
        if (visitor.visitSubtree(this)) {
            for (int i = 0, ar = arity(); i < ar; i++) {
                sub(i).execPreOrder(visitor);
            }
        }
        visitor.subtreeLeft(this);
    }

    /// Checks whether the Term is valid on the top level. If this is the case this method returns
    /// the Term unmodified. Otherwise, a TermCreationException is thrown.
    public Term checked() {
        op.validTopLevelException(this);
        return this;
        /*
         * if (op.validTopLevel(this)) { return this; } else { throw new TermCreationException(op,
         * this); }
         */
    }

    public boolean containsCodeBlockRecursive() {
        if (containsCodeBlockRecursive == ThreeValuedTruth.UNKNOWN) {
            ThreeValuedTruth result = ThreeValuedTruth.FALSE;
            if (op instanceof Modality mod && !((RustyBlock) mod.programBlock()).isEmpty()) {
                result = ThreeValuedTruth.TRUE;
            } else {
                for (int i = 0, arity = subs.size(); i < arity; i++) {
                    var sub = (TermImpl) subs.get(i);
                    if (sub.containsCodeBlockRecursive()) {
                        result = ThreeValuedTruth.TRUE;
                        break;
                    }
                }
            }
            this.containsCodeBlockRecursive = result;
        }
        return containsCodeBlockRecursive == ThreeValuedTruth.TRUE;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (op() instanceof Modality mod) {
            if (mod.kind() == RModality.RustyModalityKind.DIA) {
                sb.append("\\<").append(mod.programBlock()).append("\\>");
            } else {
                sb.append("\\[").append(mod.programBlock()).append("\\]");
            }
            sb.append("(").append(sub(0)).append(")");
            return sb.toString();
        } else {
            sb.append(op().name());
            if (!boundVars.isEmpty()) {
                sb.append(Strings.formatAsList(boundVars(), "{", ",", "}"));
            }
            if (arity() == 0) {
                return sb.toString();
            }
            sb.append(Strings.formatAsList(subs(), "(", ",", ")"));
            return sb.toString();
        }
    }

    /// true iff <code>o</code> is syntactically equal to this term
    @Override
    public boolean equals(@Nullable Object o) {
        if (o == this) {
            return true;
        }

        if (o == null || o.getClass() != getClass() || hashCode() != o.hashCode()) {
            return false;
        }

        final TermImpl t = (TermImpl) o;

        return op.equals(t.op) && subs.equals(t.subs)
                && boundVars.equals(t.boundVars);
    }

    @Override
    public int nameHash() {
        if (nameHash == -1) {
            computeHashes();
        }
        return nameHash;
    }

    @Override
    public int labelAgnosticHash() {
        if (labelAgnosticHash == -1) {
            computeHashes();
        }
        return labelAgnosticHash;
    }

    /**
     * Performs the actual computation of the hashcode and can be overwritten by subclasses if
     * necessary
     */
    protected int computeHashCode() {
        int hashcode = 5;
        hashcode = hashcode * 17 + op().hashCode();
        hashcode = hashcode * 17 + subs().hashCode();
        hashcode = hashcode * 17 + boundVars().hashCode();
        if (op instanceof RModality rm)
            hashcode = hashcode * 17 + rm.programBlock().hashCode();

        if (hashcode == -1) {
            hashcode = 0;
        }
        return hashcode;
    }

    /**
     * Computes the three hashcode caches of {@link #hashCode()}, {@link #nameHash()} and
     * {@link #labelAgnosticHash()}.
     */
    private void computeHashes() {
        // Iterate the subterm array, not arity(): the term factory probes hashCode() before it
        // validates that the operator's arity matches the subterm count, so the two can differ.
        final int n = subs.size();
        for (int i = 0; i < n; i++) {
            if (subs.get(i) instanceof TermImpl t
                    && (t.hashcode == -1 || t.nameHash == -1 || t.labelAgnosticHash == -1)) {
                t.computeHashes();
            }
        }
        if (hashcode == -1) {
            this.hashcode = computeHashCode();
        }
        if (nameHash == -1) {
            int h = 5;
            h = h * 31 + computeOperatorNameHash(op);
            h = h * 31 + arity();
            for (int i = 0; i < n; i++) {
                h = h * 31 + subs.get(i).nameHash();
            }
            if (h == -1) {
                h = 0;
            }
            nameHash = h;
        }
        if (labelAgnosticHash == -1) {
            // like the base computeHashCode() (op, bound vars, program, subterms) but recursing
            // through labelAgnosticHash and never adding this term's labels, so it is a full,
            // program-aware structural hash that ignores only term labels. It is computed here
            // instead of through computeHashCode() because the LabeledTermImpl override of that
            // method folds the labels in, which would make this hash label-sensitive.
            // This hash can disappear and be replaced with the normal hashcode once PR 3884 is
            // merged and the normal hashcode becomes label agnostic
            int h = 5;
            h = h * 17 + op.hashCode();
            h = h * 17 + boundVars().hashCode();
            if (op instanceof RModality rm) {
                h = h * 17 + rm.programBlock().hashCode();
            }
            for (int i = 0; i < n; i++) {
                h = h * 17 + subs.get(i).labelAgnosticHash();
            }
            if (h == -1) {
                h = 0;
            }
            labelAgnosticHash = h;
        }
    }

    private int computeOperatorNameHash(Operator op) {
        if (op instanceof ParametricFunctionInstance pfi) {
            // using just pfi's name would introduce a dependency on its
            // concrete syntax impacting robustness
            int h = 7;
            h = h * 31 + pfi.getBase().name().toString().hashCode();
            for (final GenericArgument arg : pfi.getArgs()) {
                if (arg instanceof SortArg(Sort sort1))
                    h = h * 31 + computeSortNameHash(sort1);
                else if (arg instanceof TermArg(Term term))
                    h = h * 31 + term.nameHash();
            }
            return h;
        } else {
            return op.name().toString().hashCode();
        }
    }

    private int computeSortNameHash(Sort sort) {
        if (sort instanceof ParametricSortInstance psi) {
            // using just psi's name would introduce a dependency on its
            // concrete syntax impacting robustness
            int h = 11;
            h = h * 31 + psi.getBase().name().toString().hashCode();
            for (final GenericArgument arg : psi.getArgs()) {
                if (arg instanceof SortArg(Sort sort1))
                    h = h * 31 + computeSortNameHash(sort1);
                else if (arg instanceof TermArg(Term term))
                    h = h * 31 + term.nameHash();
            }
            return h;
        } else {
            return sort.name().toString().hashCode();
        }
    }

    @Override
    public final int hashCode() {
        if (hashcode == -1) {
            computeHashes();
        }
        return hashcode;
    }

    // TODO(DD): Rework this into an interface
    public int getMaxDebruijnIndex() {
        if (maxDebruijnIndex == -1) {
            maxDebruijnIndex = 0;
            if (op instanceof LogicVariable lv) {
                maxDebruijnIndex = lv.getIndex();
            } else {
                for (int i = 0; i < subs.size(); i++) {
                    var ti = (TermImpl) sub(i);
                    int m = ti.getMaxDebruijnIndex();
                    if (op.bindVarsAt(i)) {
                        m -= boundVars.size();
                    }
                    if (m > maxDebruijnIndex) {
                        maxDebruijnIndex = m;
                    }
                }
            }
        }
        return maxDebruijnIndex;
    }

    // TODO(DD): Rework this into an interface
    /// Whether this term contains a logic variable for the Debruijn index `idx` (it is adjusted for
    // nested bound vars).
    public boolean containsLogicVariable(int idx) {
        if (op instanceof LogicVariable lv && lv.getIndex() == idx) {
            return true;
        }
        for (int i = 0, arity = subs.size(); i < arity; i++) {
            int subIdx = idx;
            if (op.bindVarsAt(i)) {
                subIdx += boundVars.size();
            }
            if (((TermImpl) sub(i)).containsLogicVariable(subIdx)) {
                return true;
            }
        }
        return false;
    }
}
