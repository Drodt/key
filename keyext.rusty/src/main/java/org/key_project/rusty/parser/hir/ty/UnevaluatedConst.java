package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.DefId;

public record UnevaluatedConst(DefId defId, GenericTyArgKind[] args) {
}
