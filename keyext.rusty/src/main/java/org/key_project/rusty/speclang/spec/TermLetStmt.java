package org.key_project.rusty.speclang.spec;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.pat.Pat;
import org.key_project.rusty.parser.hir.ty.Ty;

public record TermLetStmt(Pat pat, @Nullable Ty ty, Term init, HirId hirId, Span span) {
}
