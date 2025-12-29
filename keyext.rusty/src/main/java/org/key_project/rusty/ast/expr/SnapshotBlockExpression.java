/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package org.key_project.rusty.ast.expr;

/*
 * public class SnapshotBlockExpression extends AbstractBlockExpression{
 *
 * public SnapshotBlockExpression(ImmutableList<Statement> statements, @Nullable Expr value) {
 * super(statements, value);
 * }
 *
 * public SnapshotBlockExpression(ExtList children) {
 * super(children);
 * }
 *
 *
 * @Override
 * public void visit(Visitor v) {
 * v.performActionOnSnapshotBlockExpression(this);
 * }
 *
 * @Override
 * public String toString() {
 * StringBuilder sb = new StringBuilder();
 * sb.append("snapshot! {");
 * for (int i = 0; i < getStatements().size(); i++) {
 * if (i > 0)
 * sb.append("; ");
 * sb.append(getStatements().get(i));
 * }
 * if (getValue() != null) {
 * if (!getStatements().isEmpty())
 * sb.append("; ");
 * sb.append(getValue());
 * }
 * sb.append("}");
 * return sb.toString();
 * }
 * }
 */
