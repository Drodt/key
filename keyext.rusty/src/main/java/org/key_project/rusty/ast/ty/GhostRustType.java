package org.key_project.rusty.ast.ty;

import org.jspecify.annotations.NonNull;
import org.key_project.logic.SyntaxElement;
import org.key_project.rusty.ast.abstraction.GhostType;
import org.key_project.rusty.ast.abstraction.Type;
import org.key_project.rusty.ast.visitor.Visitor;

public record GhostRustType(RustType inner) implements RustType{
    @Override
    public Type type() {
        return GhostType.get(inner.type());
    }

    @Override
    public void visit(Visitor v) {
        v.performActionOnGhostRustType(this);
    }

    @Override
    public @NonNull SyntaxElement getChild(int n) {
        if (n==0) return inner;
        throw new IndexOutOfBoundsException("GhostRustType has only one child");
    }

    @Override
    public int getChildCount() {
        return 1;
    }
}
