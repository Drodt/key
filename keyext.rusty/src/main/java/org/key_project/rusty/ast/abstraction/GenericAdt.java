/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.rusty.Services;
import org.key_project.rusty.logic.sort.ParametricSortDecl;
import org.key_project.util.collection.ImmutableArray;

import org.jspecify.annotations.Nullable;

public interface GenericAdt extends HasGenerics, Adt {
    @Nullable
    ParametricSortDecl sortDecl();

    Type instantiate(ImmutableArray<GenericTyArg> args, Services services);
}
