/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Named;
import org.key_project.util.collection.ImmutableArray;


public interface HasGenerics extends Named {
    ImmutableArray<GenericParam> params();
}
