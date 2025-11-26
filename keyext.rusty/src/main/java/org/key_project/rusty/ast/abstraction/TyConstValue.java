package org.key_project.rusty.ast.abstraction;

public record TyConstValue(int value) implements TyConst {
    @Override
    public ArrayLen toLength() {
        return new IntArrayLen(value);
    }
}
