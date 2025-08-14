/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.parser.hir.expr;

import org.key_project.rusty.parser.hir.Span;

public interface CaptureBy {
    record Value(Span moveKw) implements CaptureBy {
    }
    record Ref() implements CaptureBy {
    }
    record Use(Span useKw) implements CaptureBy {
    }
}
