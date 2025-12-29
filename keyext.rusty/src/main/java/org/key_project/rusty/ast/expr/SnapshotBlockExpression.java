package org.key_project.rusty.ast.expr;

import org.jspecify.annotations.Nullable;
import org.key_project.rusty.ast.stmt.Statement;
import org.key_project.rusty.ast.visitor.Visitor;
import org.key_project.util.ExtList;
import org.key_project.util.collection.ImmutableList;
/*
public class SnapshotBlockExpression extends AbstractBlockExpression{

    public SnapshotBlockExpression(ImmutableList<Statement> statements, @Nullable Expr value) {
        super(statements, value);
    }

    public SnapshotBlockExpression(ExtList children) {
        super(children);
    }


    @Override
    public void visit(Visitor v) {
        v.performActionOnSnapshotBlockExpression(this);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("snapshot! {");
        for (int i = 0; i < getStatements().size(); i++) {
            if (i > 0)
                sb.append("; ");
            sb.append(getStatements().get(i));
        }
        if (getValue() != null) {
            if (!getStatements().isEmpty())
                sb.append("; ");
            sb.append(getValue());
        }
        sb.append("}");
        return sb.toString();
    }
}*/