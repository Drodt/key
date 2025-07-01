/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.pat;

import org.key_project.logic.Name;
import org.key_project.logic.Named;
import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.rusty.logic.op.ProgramVariable;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class IdentPattern implements Pattern, Named {
    private final boolean reference;
    private final boolean mutable;
    private final ProgramVariable pv;

    public IdentPattern(boolean reference, boolean mutable, ProgramVariable pv) {
        this.reference = reference;
        this.mutable = mutable;
        this.pv = pv;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) {
            return pv;
        }
        throw new IndexOutOfBoundsException("IdentPattern has only one child");
    }

    public boolean isMutable() {
        return mutable;
    }

    public boolean isReference() {
        return reference;
    }

    @Override
    public @NonNull Name name() {
        return pv.name();
    }

    @Override
    public int getChildCount() {
        return 1;
    }

    public ProgramVariable programVariable() {
        return pv;
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        if (reference) {
            sb.append("&");
        }
        if (mutable)
            sb.append("mut ");
        return sb.append(pv).toString();
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnIdentPattern(this);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }
        final var other = (IdentPattern) obj;
        return reference == other.reference && mutable == other.mutable
                && pv == other.pv;
    }

    @Override
    public int hashCode() {
        int hashcode = 5;
        hashcode = 31 * hashcode + Boolean.hashCode(reference);
        hashcode = 31 * hashcode + Boolean.hashCode(mutable);
        hashcode = 31 * hashcode + pv.hashCode();
        return hashcode;
    }
}
