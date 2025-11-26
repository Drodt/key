package org.key_project.rusty.speclang.spec;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Span;

public record TermBlock(TermStmt stmts, @Nullable Term term, HirId hirId, Span span) {
}
