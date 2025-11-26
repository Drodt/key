package org.key_project.rusty.speclang.spec;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.pat.Pat;

public record TermArm(HirId hirId, Span span, Pat pat, @Nullable Term guard, Term body) {
}
