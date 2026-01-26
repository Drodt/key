/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir;

import org.jspecify.annotations.Nullable;

public interface DefKind {
    record Mod() implements DefKind {
    }

    record Struct() implements DefKind {
    }

    record Union() implements DefKind {
    }

    record Enum() implements DefKind {
    }

    record Variant() implements DefKind {
    }

    record Trait() implements DefKind {
    }

    record TyAlias() implements DefKind {
    }

    record ForeignTy() implements DefKind {
    }

    record TraitAlias() implements DefKind {
    }

    record AssocTy() implements DefKind {
    }

    record TyParam() implements DefKind {
    }

    record Fn() implements DefKind {
    }

    record Const() implements DefKind {
    }

    record ConstParam() implements DefKind {
    }

    record Static(boolean safety, boolean mutability, boolean nested) implements DefKind {
    }

    record Constructor(Ctor ctor) implements DefKind {
    }

    record AssocFn() implements DefKind {
    }

    record AssocConst() implements DefKind {
    }

    record Macro(MacroKind kind) implements DefKind {
    }

    record ExternCrate() implements DefKind {
    }

    record Use() implements DefKind {
    }

    record ForeignMod() implements DefKind {
    }

    record AnonConst() implements DefKind {
    }

    record InlineConst() implements DefKind {
    }

    record OpaqueTy() implements DefKind {
    }

    record Field() implements DefKind {
    }

    record LifetimeParam() implements DefKind {
    }

    record GlobalAsm() implements DefKind {
    }

    record Impl(boolean ofTrait) implements DefKind {
    }

    record Closure() implements DefKind {
    }

    record SyntheticCoroutineBody() implements DefKind {
    }

    class Adapter extends HirAdapter<DefKind> {
        @Override
        public @Nullable Class<? extends DefKind> getType(String tag) {
            return switch (tag) {
                case "Mod" -> Mod.class;
                case "Struct" -> Struct.class;
                case "Union" -> Union.class;
                case "Enum" -> Enum.class;
                case "Variant" -> Variant.class;
                case "Trait" -> Trait.class;
                case "TyAlias" -> TyAlias.class;
                case "ForeignTy" -> ForeignTy.class;
                case "TraitAlias" -> TraitAlias.class;
                case "AssocTy" -> AssocTy.class;
                case "TyParam" -> TyParam.class;
                case "Fn" -> Fn.class;
                case "Const" -> Const.class;
                case "ConstParam" -> ConstParam.class;
                case "Static" -> Static.class;
                case "Ctor" -> Constructor.class;
                case "AssocFn" -> AssocFn.class;
                case "AssocConst" -> AssocConst.class;
                case "Macro" -> Macro.class;
                case "ExternCrate" -> ExternCrate.class;
                case "Use" -> Use.class;
                case "ForeignMod" -> ForeignMod.class;
                case "AnonConst" -> AnonConst.class;
                case "InlineConst" -> InlineConst.class;
                case "OpaqueTy" -> OpaqueTy.class;
                case "Field" -> Field.class;
                case "GlobalAsm" -> GlobalAsm.class;
                case "Impl" -> Impl.class;
                case "Closure" -> Closure.class;
                case "SyntheticCoroutineBody" -> SyntheticCoroutineBody.class;
                default -> null;
            };
        }
    }
}
