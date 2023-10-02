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

    private ImmutableSet<Sort> ext;

    /** Information of the origin of this sort */
    private String origin;

    public AbstractSort(Name name, ImmutableSet<Sort> ext, boolean isAbstract) {
        super(name, isAbstract);
        this.ext = ext;
    }


    @Override
    public final ImmutableSet<Sort> extendsSorts() {
        if (this == Sort.FORMULA || this == Sort.UPDATE || this == Sort.ANY) {
            return DefaultImmutableSet.nil();
        } else {
            if (ext.isEmpty()) {
                ext = DefaultImmutableSet.<Sort>nil().add(Sort.ANY);
            }
            return ext;
        }
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

    @Nullable
    @Override
    public String getOrigin() {
        return origin;
    }

    public void setOrigin(@Nullable String origin) {
        this.origin = origin;
    }
}
