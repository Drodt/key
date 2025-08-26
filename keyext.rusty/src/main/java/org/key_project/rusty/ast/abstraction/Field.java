/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;


import org.key_project.logic.Name;
import org.key_project.rusty.logic.op.RFunction;

public record Field(Name name, Type type, RFunction fieldConst) {
}
