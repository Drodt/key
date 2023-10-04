/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.logic.sort;

import javax.annotation.Nullable;

import de.uka.ilkd.key.java.Services;
import de.uka.ilkd.key.ldt.JavaDLTheory;
import de.uka.ilkd.key.rule.HasOrigin;

import org.key_project.logic.Name;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

/**
 * Abstract base class for implementations of the Sort interface.
 */
public abstract class Sort extends org.key_project.logic.sort.AbstractSort<Sort>
        implements HasOrigin {

    private ImmutableSet<Sort> ext;

    public Sort(Name name, ImmutableSet<Sort> ext, boolean isAbstract, String origin, String documentation) {
        super(name, isAbstract, documentation, origin);
        this.ext = ext;
    }

    @Override
    public ImmutableSet<Sort> extendsSorts() {
        if (this == JavaDLTheory.FORMULA || this == JavaDLTheory.UPDATE
                || this == JavaDLTheory.ANY) {
            return DefaultImmutableSet.nil();
        } else {
            if (ext.isEmpty()) {
                ext = DefaultImmutableSet.<Sort>nil().add(JavaDLTheory.ANY);
            }
            return ext;
        }
    }

    public ImmutableSet<Sort> extendsSorts(Services services) {
        return extendsSorts();
    }

    @Override
    public boolean extendsTrans(Sort sort) {
        if (sort == this) {
            return true;
        } else if (this == JavaDLTheory.FORMULA || this == JavaDLTheory.UPDATE) {
            return false;
        } else if (sort == JavaDLTheory.ANY) {
            return true;
        }

        return extendsSorts()
                .exists((Sort superSort) -> superSort == sort || superSort.extendsTrans(sort));
    }
}
