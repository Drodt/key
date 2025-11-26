package org.key_project.rusty.parser.hir.ty;

public record ConstExpr(ConstExprKind kind, GenericTyArgKind[] args) {
}
