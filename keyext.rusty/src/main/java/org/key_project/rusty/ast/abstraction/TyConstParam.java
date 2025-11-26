package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.op.Function;

public record TyConstParam(Function fn) implements TyConst{
    @Override
    public ArrayLen toLength() {
        return new ConstArrayLen(fn);
    }
}
