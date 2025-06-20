/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.logic;

import org.key_project.logic.sort.Sort;

public interface Sorted {
    /// the sort of the entity
    ///
    /// @return the [Sort] of the sorted entity
    Sort sort();
}
