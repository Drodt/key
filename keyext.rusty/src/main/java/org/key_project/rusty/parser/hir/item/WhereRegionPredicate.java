/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.parser.hir.item;

import org.key_project.rusty.parser.hir.GenericBound;
import org.key_project.rusty.parser.hir.Lifetime;

public record WhereRegionPredicate(boolean inWhereClause, Lifetime lifetime,
        GenericBound[] bounds) {
}
