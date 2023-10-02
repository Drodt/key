/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.logic.sort;

import javax.annotation.Nullable;

import de.uka.ilkd.key.java.Services;

import org.key_project.logic.Name;
import org.key_project.util.collection.DefaultImmutableSet;
import org.key_project.util.collection.ImmutableSet;

/**
 * Abstract base class for implementations of the Sort interface.
 */
public abstract class AbstractSort extends org.key_project.logic.sort.AbstractSort<Sort>
        implements Sort {

    private final ImmutableSet<Sort> ext;


    public AbstractSort(Name name,
            ImmutableSet<Sort> extendedSorts,
            boolean isAbstract,
            String documentation,
            String origin) {
        super(name, isAbstract, documentation, origin);
        if (extendedSorts != null && extendedSorts.isEmpty()) {
            this.ext = DefaultImmutableSet.<Sort>nil().add(ANY);
        } else {
            this.ext = extendedSorts == null ? DefaultImmutableSet.<Sort>nil() : extendedSorts;
        }
    }

    @Override
    public final ImmutableSet<Sort> extendsSorts() {
        return ext;
    }

    @Override
    public final ImmutableSet<Sort> extendsSorts(Services services) {
        return extendsSorts();
    }


    @Override
    public final boolean extendsTrans(Sort sort) {
        if (sort == this) {
            return true;
        } else if (this == Sort.FORMULA || this == Sort.UPDATE) {
            return false;
        } else if (sort == Sort.ANY) {
            return true;
        }

        return extendsSorts()
                .exists((Sort superSort) -> superSort == sort || superSort.extendsTrans(sort));
    }

    public String declarationString() {
        return name().toString();
    }
}
