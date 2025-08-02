/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty;

import java.util.HashMap;
import java.util.Map;

import org.key_project.logic.sort.Sort;
import org.key_project.rusty.logic.sort.MRefSort;

public class RefSortManager {
    private final Map<Sort, MRefSort> mRefMap = new HashMap<>();

    private final Services services;

    public RefSortManager(Services services) {
        this.services = services;
    }

    public Sort getRefSort(Sort sort) {
        return mRefMap.computeIfAbsent(sort,
            s -> {
                var ms = new MRefSort(s);
                services.getNamespaces().sorts().add(ms);
                return ms;
            });
    }
}
