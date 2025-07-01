/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only 
package org.key_project.rusty.ast.expr;*/

import java.util.Objects;

import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.pat.Pattern;
import org.key_project.rusty.ast.ty.RustType;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.ExtList;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;


public class GhostLetExpression implements Expr {

    private final Pattern pat;
    private final RustType type;
    private final Expr init;
    private final Expr body;

    private int hashCode = -1;

    public GhostLetExpression(Pattern pat, @Nullable RustType type, Expr init, Expr body) {
        this.pat = pat;
        this.type = type;
        this.init = init;
        this.body = body;
    }

    public GhostLetExpression(ExtList changeList) {
        pat = changeList.removeFirstOccurrence(Pattern.class);
        type = changeList.removeFirstOccurrence(RustType.class);
        init = changeList.removeFirstOccurrence(Expr.class);
        body = changeList.removeFirstOccurrence(Expr.class);
    }

    public Pattern getPattern() {
        return pat;
    }

    public @Nullable RustType getType() {
        return type;
    }

    public Expr getInit() {
        return init;
    }

    public Expr getBody() {
        return body;
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n == 0) return pat;
        --n;
        if (n == 0 && type != null) return type;
        if (type != null) --n;
        if (n == 0) return init;
        if (--n == 0) return body;
        throw new IndexOutOfBoundsException("GhostLetExpression has " + getChildCount() + " children");
    }

    @Override
    public int getChildCount() {
        int count = 2; // pat, init, body
        if (type != null) count++;
        return count;
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnGhostLetExpression(this);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder();
        sb.append("ghost let ").append(pat);
        if (type != null) {
            sb.append(": ").append(type);
        }
        sb.append(" = ").append(init).append("; ");
        sb.append(body);
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        GhostLetExpression that = (GhostLetExpression) obj;
        return pat.equals(that.pat) &&
               Objects.equals(type, that.type) &&
               init.equals(that.init) &&
               body.equals(that.body);
    }

    @Override
    public int hashCode() {
        if (hashCode == -1) {
            hashCode = computeHashCode();
        }
        return hashCode;
    }

    private int computeHashCode() {
        return Objects.hash(pat, type, init, body);
    }
}
 