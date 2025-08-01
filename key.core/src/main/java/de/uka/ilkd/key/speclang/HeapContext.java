/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.speclang;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.logic.JTerm;
import de.uka.ilkd.key.logic.TermServices;
import de.uka.ilkd.key.logic.op.LocationVariable;


/**
 * Heap contexts are various scenarios of what happens to heap variables during PO generation and
 * built-in rule applications, like saving atPre heaps, anonymisation, etc.
 */
public class HeapContext {

    public static List<LocationVariable> getModifiableHeaps(Services services,
            boolean transaction) {
        List<LocationVariable> result = new ArrayList<>();
        final LocationVariable savedHeap = services.getTypeConverter().getHeapLDT().getSavedHeap();
        for (LocationVariable heap : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
            if (savedHeap == heap && !transaction) {
                continue;
            }
            result.add(heap);
        }
        return result;
    }

    public static Map<LocationVariable, LocationVariable> getBeforeAtPreVars(
            List<LocationVariable> heaps, TermServices services, String contextName) {
        Map<LocationVariable, LocationVariable> result =
            new LinkedHashMap<>();
        for (LocationVariable heap : heaps) {
            final LocationVariable atPreVar = services.getTermBuilder()
                    .locationVariable(heap.name() + contextName, heap.sort(), true);
            result.put(heap, atPreVar);
        }
        return result;
    }

    public static Map<LocationVariable, JTerm> getAtPres(
            Map<LocationVariable, LocationVariable> atPreVars, Services services) {
        final Map<LocationVariable, JTerm> result = new LinkedHashMap<>();
        for (LocationVariable heap : services.getTypeConverter().getHeapLDT().getAllHeaps()) {
            final LocationVariable lv = atPreVars.get(heap);
            final JTerm t = lv == null ? null : services.getTermBuilder().var(lv);
            result.put(heap, t);
        }
        return result;
    }

}
