/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.logic.sort;

import org.jspecify.annotations.NonNull;

public record GenericSortParam(GenericSort gs) implements ParamSortParam {
    @Override
    public @NonNull String toString() {
        return gs.toString();
    }
}
