/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.GenericArgument;
import org.key_project.rusty.logic.sort.SortArg;

public record GenericTyArgType(Type type) implements GenericTyArg {
    @Override
    public GenericArgument sortArg(Services services) {
        return new SortArg(type.getSort(services));
    }
}
