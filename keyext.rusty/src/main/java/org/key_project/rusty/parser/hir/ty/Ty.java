/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.ty;

import org.key_project.rusty.parser.hir.DefId;
import org.key_project.rusty.parser.hir.FloatTy;
import org.key_project.rusty.parser.hir.HirAdapter;
import org.key_project.rusty.parser.hir.hirty.IntTy;
import org.key_project.rusty.parser.hir.hirty.UintTy;
import org.key_project.rusty.parser.hir.item.FnHeader;

import org.jspecify.annotations.Nullable;

public interface Ty {
    record Bool() implements Ty {
    }

    record Char() implements Ty {
    }

    record Int(IntTy ty) implements Ty {
    }

    record Uint(UintTy ty) implements Ty {
    }

    record Float(FloatTy ty) implements Ty {
    }

    record Adt(AdtDef def, GenericTyArgKind[] args) implements Ty {
    }

    record Foreign(DefId defId) implements Ty {
    }

    record Str() implements Ty {
    }

    record Array(Ty ty, TyConst len) implements Ty {
    }

    record Slice(Ty ty) implements Ty {
    }

    record RawPtr(Ty ty, boolean mut) implements Ty {
    }

    record Ref(Ty ty, boolean mut) implements Ty {
    }

    record FnDef(DefId defId, GenericTyArgKind[] args) implements Ty {
    }

    record FnPtr(Binder<FnSigTys> binder, FnHeader header) implements Ty {
    }

    record Dynamic(Binder<ExistentialPredicate>[] binders, DynKind kind) implements Ty {
    }

    record Closure(DefId defId, GenericTyArgKind[] args) implements Ty {
    }

    record Never() implements Ty {
    }

    record Tuple(Ty[] tys) implements Ty {
    }

    record Alias(AliasTyKind kind, AliasTy ty) implements Ty {
    }

    record Param(ParamTy ty) implements Ty {
    }

    record Bound(int idx, BoundTy ty) implements Ty {
    }

    class Adapter extends HirAdapter<Ty> {
        @Override
        public @Nullable Class<? extends Ty> getType(String tag) {
            return switch (tag) {
                case "Bool" -> Bool.class;
                case "Char" -> Char.class;
                case "Int" -> Int.class;
                case "Uint" -> Uint.class;
                case "Float" -> Float.class;
                case "Adt" -> Adt.class;
                case "Foreign" -> Foreign.class;
                case "Str" -> Str.class;
                case "Array" -> Array.class;
                case "Slice" -> Slice.class;
                case "RawPtr" -> RawPtr.class;
                case "Ref" -> Ref.class;
                case "FnDef" -> FnDef.class;
                case "FnPtr" -> FnPtr.class;
                case "Dynamic" -> Dynamic.class;
                case "Closure" -> Closure.class;
                case "Never" -> Never.class;
                case "Tuple" -> Tuple.class;
                case "Alias" -> Alias.class;
                case "Param" -> Param.class;
                case "Bound" -> Bound.class;

                default -> null;
            };
        }
    }
}
