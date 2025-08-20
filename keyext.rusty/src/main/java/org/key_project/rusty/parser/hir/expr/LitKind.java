/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.HirAdapter;

import com.google.gson.annotations.SerializedName;
import org.jspecify.annotations.Nullable;

public interface LitKind {
    record Str(String symbol, StrStyle style) implements LitKind {
    }

    record ByteStr(char[] bytes, StrStyle style) implements LitKind {
    }

    record Byte(@SerializedName("byte") char _byte) implements LitKind {
    }

    record Char(@SerializedName("char") char _char) implements LitKind {
    }

    record Int(int value, LitIntTy ty) implements LitKind {
    }

    record Float(String symbol, LitFloatTy ty) implements LitKind {
    }

    record Bool(boolean value) implements LitKind {
    }

    class Adapter extends HirAdapter<LitKind> {
        @Override
        public @Nullable Class<? extends LitKind> getType(String tag) {
            return switch (tag) {
                case "Str" -> Str.class;
                case "ByteStr" -> ByteStr.class;
                case "Byte" -> Byte.class;
                case "Char" -> Char.class;
                case "Int" -> Int.class;
                case "Float" -> Float.class;
                case "Bool" -> Bool.class;
                default -> null;
            };
        }
    }
}
