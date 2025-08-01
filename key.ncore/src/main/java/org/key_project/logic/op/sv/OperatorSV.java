/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.logic.op.sv;

import org.key_project.logic.op.Operator;
import org.key_project.logic.sort.Sort;

public interface OperatorSV extends SchemaVariable, Operator {
    /// returns the target sort of the operator
    Sort sort();
}
