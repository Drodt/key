/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.logic.sort;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;

import org.jspecify.annotations.NonNull;

public record ConstParam(Name name, Sort sort) implements GenericParameter {
    @Override
    public @NonNull String toString() {
        return "const " + name + ": " + sort;
    }
}
