/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.speclang.spec;

import com.google.gson.annotations.SerializedName;
import org.key_project.rusty.parser.hir.*;
import org.key_project.rusty.parser.hir.expr.BinOp;
import org.key_project.rusty.parser.hir.expr.Lit;
import org.key_project.rusty.parser.hir.expr.MatchSource;
import org.key_project.rusty.parser.hir.expr.UnOp;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.parser.hir.ty.Ty;

public interface TermKind {
    record Array(Term[] terms) implements TermKind {}

    record Call(Term callee, Term[] args) implements TermKind {}

    record MethodCall(PathSegment path, Term callee, Term[] args, Span span) implements TermKind{}

    record Tup(Term[] terms) implements TermKind {}

    record Binary(BinOp op, Term left, Term right) implements TermKind {
    }

    record Unary(UnOp op, Term child) implements TermKind {
    }

    record Lit(org.key_project.rusty.parser.hir.expr.Lit lit) implements TermKind {
    }

    record Cast(Term term, Ty ty) implements TermKind{}

    record Let(TermLet let) implements TermKind{}

    record If(Term cond, Term then, @SerializedName("else") @Nullable Term els) implements TermKind{}

    record Match(Term term, TermArm[] arms, MatchSource src) implements TermKind{}

    // Closure

    record Block(TermBlock block) implements TermKind{}

    record Field(Term term, Ident field) implements TermKind{}

    record Index(Term term, Term idx, Span span) implements TermKind{}

    record Path(QPath path) implements TermKind {
    }

    // AddrOf

    // Struct

    // Repeat

    record Quantor(QuantorKind kind, QuantorParam param, Term term) implements TermKind{}

    // Model

    class Adapter extends HirAdapter<TermKind> {
        @Override
        public @Nullable Class<? extends TermKind> getType(String tag) {
            return switch (tag) {
                case "Array" -> Array.class;
                case "Call" -> Call.class;
                case "MethodCall" -> MethodCall.class;
                case "Tup" -> Tup.class;
                case "Binary" -> Binary.class;
                case "Unary" -> Unary.class;
                case "Lit" -> Lit.class;
                case "Cast" -> Cast.class;
                case "Let" -> Let.class;
                case "If" -> If.class;
                case "Match" -> Match.class;
                case "Block" -> Block.class;
                case "Field" -> Field.class;
                case "Index" -> Index.class;
                case "Path" -> Path.class;
                case "Quantor" -> Quantor.class;
                default -> null;
            };
        }

    }
}
