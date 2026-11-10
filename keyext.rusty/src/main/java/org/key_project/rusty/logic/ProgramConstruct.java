/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2, 
 * or (at your option) any later version.
 * SPDX-License-Identifier: GPL-2.0-or-later */
package org.key_project.rusty.logic;

import org.key_project.rusty.ast.ElseBranch;
import org.key_project.rusty.ast.expr.Expr;
import org.key_project.rusty.ast.expr.ThenBranch;
import org.key_project.rusty.ast.pat.PatExpr;
import org.key_project.rusty.ast.pat.Pattern;
import org.key_project.rusty.ast.stmt.Statement;

public interface ProgramConstruct
        extends Expr, PatExpr, Statement, Pattern, ThenBranch, ElseBranch {
}
