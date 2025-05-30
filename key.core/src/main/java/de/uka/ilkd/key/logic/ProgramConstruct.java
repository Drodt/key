/* This file is part of KeY - https://key-project.org
 * KeY is licensed under the GNU General Public License Version 2
 * SPDX-License-Identifier: GPL-2.0-only */
package de.uka.ilkd.key.logic;

import de.uka.ilkd.key.java.Label;
import de.uka.ilkd.key.java.expression.ExpressionStatement;
import de.uka.ilkd.key.java.reference.IExecutionContext;
import de.uka.ilkd.key.java.reference.MethodName;
import de.uka.ilkd.key.java.reference.TypeReference;
import de.uka.ilkd.key.java.statement.Branch;
import de.uka.ilkd.key.java.statement.IForUpdates;
import de.uka.ilkd.key.java.statement.IGuard;
import de.uka.ilkd.key.java.statement.ILoopInit;
import de.uka.ilkd.key.logic.op.IProgramMethod;
import de.uka.ilkd.key.logic.op.IProgramVariable;

import org.key_project.logic.SyntaxElement;

/**
 * A type that implement this interface can be used in all java programs instead of an expression or
 * statement. For example class SchemaVariable implements this interface to be able to stand for
 * program constructs.
 */
public interface ProgramConstruct extends ILoopInit, IForUpdates, IGuard,
        Label, ExpressionStatement, TypeReference, IProgramVariable,
        IProgramMethod, Branch, IExecutionContext, MethodName {
    @Override
    SyntaxElement getChild(int n);

    @Override
    int getChildCount();
}
