/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.util;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.key_project.util.Properties;

public abstract class AbstractProperties implements Properties {
    private final Map<Properties.Property<?>, Set<Properties.PropertyListener>> listenerMap =
        new IdentityHashMap<>();

    private final Set<Properties.PropertyListener> globalListeners =
        new HashSet<>();

    @Override
    public void addPropertyListener(Properties.Property<?> property,
            Properties.PropertyListener listener) {
        if (property == null) {
            globalListeners.add(listener);
        } else {
            Set<Properties.PropertyListener> list =
                listenerMap.computeIfAbsent(property, k -> new HashSet<>());
            list.add(listener);
        }
    }

    @Override
    public void removePropertyListener(Properties.Property<?> property,
            Properties.PropertyListener listener) {
        if (property == null) {
            globalListeners.remove(listener);
        } else {
            Set<Properties.PropertyListener> list = listenerMap.get(property);
            if (list != null) {
                list.remove(listener);
            }
        }
    }

    @Override
    public void removePropertyListener(Properties.PropertyListener listener) {
        globalListeners.remove(listener);
        for (Set<Properties.PropertyListener> list : listenerMap.values()) {
            list.remove(listener);
        }
    }

    protected <T> void firePropertyChange(Properties.Property<T> property, T oldValue, T newValue) {
        if (oldValue == null || !oldValue.equals(newValue)) {
            Set<Properties.PropertyListener> list = listenerMap.get(property);
            if (list != null) {
                for (Properties.PropertyListener listener : list) {
                    listener.propertyChanged(property, oldValue, newValue);
                }
            }
            for (Properties.PropertyListener listener : globalListeners) {
                listener.propertyChanged(property, oldValue, newValue);
            }
        }
    }

    @Override
    public abstract Properties clone();
}
