package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;

public record ConstArrayLen(Function fn) implements ArrayLen {
    @Override
    public Term toTerm(Services services) {
        return services.getTermBuilder().func(fn);
    }
}
