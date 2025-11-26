package org.key_project.rusty.speclang.spec;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.HirAdapter;

public interface TermStmtKind {
    record Let(TermLetStmt let) implements TermStmtKind {}

    // Item

    record Term(Term term) implements TermStmtKind {}

    record Semi(Term term) implements TermStmtKind {}

class Adapter extends HirAdapter<TermStmtKind> {
        @Override
        public @Nullable Class<? extends TermStmtKind> getType(String tag) {
            return switch (tag) {
                case "Let" -> Let.class;
                case "Term" -> Term.class;
                case "Semi" -> Semi.class;
                default -> null;
            };
        }
    }
}
