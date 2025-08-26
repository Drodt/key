/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ldt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class LDTs implements Iterable<LDT> {
    private final BoolLDT boolLDT;
    private final IntLDT intLDT;;
    private final CharLDT charLDT;
    private final StrLDT strLDT;
    private final NeverLDT neverLDT;
    private final ArrayLDT arrayLDT;
    private final FieldLDT fieldLDT;
    private final Map<Name, LDT> map;

    public LDTs(Services services) {
        boolLDT = new BoolLDT(services);
        intLDT = new IntLDT(services);
        charLDT = new CharLDT(services);
        strLDT = new StrLDT(services);
        neverLDT = new NeverLDT(services);
        arrayLDT = new ArrayLDT(services);
        fieldLDT = new FieldLDT(services);
        map = new HashMap<>();
        map.put(boolLDT.name(), boolLDT);
        map.put(intLDT.name(), intLDT);
        map.put(charLDT.name(), charLDT);
        map.put(strLDT.name(), strLDT);
        map.put(neverLDT.name(), neverLDT);
        map.put(arrayLDT.name(), arrayLDT);
        map.put(fieldLDT.name(), fieldLDT);
    }

    public BoolLDT getBoolLDT() {
        return boolLDT;
    }

    public IntLDT getIntLDT() {
        return intLDT;
    }

    public CharLDT getCharLDT() {
        return charLDT;
    }

    public StrLDT getStrLDT() {
        return strLDT;
    }

    public NeverLDT getNeverLDT() {
        return neverLDT;
    }

    public ArrayLDT getArrayLDT() {
        return arrayLDT;
    }

    public FieldLDT getFieldLDT() {
        return fieldLDT;
    }

    public @Nullable LDT get(Name name) {
        return map.get(name);
    }


    @Override
    public @NonNull Iterator<LDT> iterator() {
        return map.values().iterator();
    }

    public @Nullable LDT getLDTFor(Sort s) {
        for (LDT ldt : this) {
            if (s.equals(ldt.targetSort())) {
                return ldt;
            }
        }
        return null;
    }
}
