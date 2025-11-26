/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.abstraction;

import org.key_project.logic.Term;
import org.key_project.logic.op.Function;
import org.key_project.rusty.Services;

public record ConstArrayLen(Function fn) implements ArrayLen {
    @Override
    public Term toTerm(Services services) {
        return services.getTermBuilder().func(fn);
    }
}
