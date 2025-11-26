package org.key_project.rusty.speclang.spec;

import org.key_project.rusty.parser.hir.HirId;
import org.key_project.rusty.parser.hir.Ident;
import org.key_project.rusty.parser.hir.Span;
import org.key_project.rusty.parser.hir.hirty.HirTy;

public record QuantorParam(HirId hirId, Ident ident, HirTy ty, Span span, Span tySpan) {
}
