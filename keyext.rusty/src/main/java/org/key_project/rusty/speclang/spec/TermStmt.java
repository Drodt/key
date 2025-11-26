package org.key_project.rusty.speclang.spec;

import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Span;

public record TermStmt(HirId hirId, Span span, TermStmtKind kind) {
}
