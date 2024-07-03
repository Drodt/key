/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty;

import org.key_project.rusty.ldt.LDTs;
import org.key_project.rusty.logic.NamespaceSet;
import org.key_project.rusty.logic.TermBuilder;
import org.key_project.rusty.logic.TermFactory;

public class Services {
    /**
     * proof specific namespaces (functions, predicates, sorts, variables)
     */
    private final NamespaceSet namespaces = new NamespaceSet();
    private final LDTs ldts;

    private final TermFactory tf;
    private final TermBuilder tb;

    public Services() {
        this.tf = new TermFactory();
        this.tb = new TermBuilder(tf, this);
        this.ldts = new LDTs(this);
    }

    public NamespaceSet getNamespaces() {
        return namespaces;
    }

    public TermBuilder getTermBuilder() {
        return tb;
    }

    public TermFactory getTermFactory() {
        return tf;
    }

    public LDTs getLDTs() {
        return ldts;
    }
}
