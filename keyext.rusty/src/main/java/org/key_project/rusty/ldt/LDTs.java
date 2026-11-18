/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.ldt;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.key_project.logic.Name;
import org.key_project.logic.sort.Sort;
import org.key_project.rusty.Services;
import org.key_project.rusty.logic.RustyDLTheory;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class LDTs implements Iterable<AbstractLDT> {
    private final BoolLDT boolLDT;
    private final IntLDT intLDT;
    private final CharLDT charLDT;
    private final StrLDT strLDT;
    private final NeverLDT neverLDT;
    private final ArrayLDT arrayLDT;
    private final FieldLDT fieldLDT;
    private final TupleLDT[] tupleLDTs;
    private final SRefLDT sRefLDT;
    private final MRefLDT mRefLDT;
    private final GhostLDT ghostLDT;
    private final RustyDLTheory rustyDLTheory;
    private final SeqLDT seqLDT;
    private final Map<Name, AbstractLDT> map;

    public LDTs(Services services) {
        map = new HashMap<>();
        map.putAll(LDT.getNewLDTInstances(services));
        rustyDLTheory = (RustyDLTheory) map.get(RustyDLTheory.NAME);
        boolLDT = (BoolLDT) map.get(BoolLDT.NAME);
        intLDT = (IntLDT) map.get(IntLDT.NAME);
        charLDT = (CharLDT) map.get(CharLDT.NAME);
        strLDT = (StrLDT) map.get(StrLDT.NAME);
        neverLDT = (NeverLDT) map.get(NeverLDT.NAME);

        map.putAll(ParametricLDT.getNewLDTInstances(services));
        arrayLDT = (ArrayLDT) map.get(ArrayLDT.NAME);
        fieldLDT = (FieldLDT) map.get(FieldLDT.NAME);
        tupleLDTs = new TupleLDT[12];
        for (int i = 0; i < 12; ++i) {
            tupleLDTs[i] = (TupleLDT) map.get(new Name(TupleLDT.NAME.toString() + (i + 1)));
        }
        sRefLDT = (SRefLDT) map.get(SRefLDT.NAME);
        mRefLDT = (MRefLDT) map.get(MRefLDT.NAME);
        ghostLDT = (GhostLDT) map.get(GhostLDT.NAME);
        seqLDT = (SeqLDT) map.get(SeqLDT.NAME);
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

    public TupleLDT getTupleLDT(int i) {
        return tupleLDTs[i - 1];
    }

    public SRefLDT getsRefLDT() {
        return sRefLDT;
    }

    public MRefLDT getmRefLDT() {
        return mRefLDT;
    }

    public GhostLDT getGhostLDT() {
        return ghostLDT;
    }

    public SeqLDT getSeqLDT() {
        return seqLDT;
    }

    public RustyDLTheory getRustyDLTheory() {
        return rustyDLTheory;
    }

    public @Nullable AbstractLDT get(Name name) {
        return map.get(name);
    }

    @Override
    public @NonNull Iterator<AbstractLDT> iterator() {
        return map.values().iterator();
    }

    public @Nullable LDT getLDTFor(Sort s) {
        for (AbstractLDT aldt : this) {
            if (aldt instanceof LDT ldt && s.equals(ldt.targetSort())) {
                return ldt;
            }
        }
        return null;
    }
}
