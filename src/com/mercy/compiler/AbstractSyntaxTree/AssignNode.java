package com.mercy.compiler.AbstractSyntaxTree;

import com.mercy.compiler.Type.Type;

/**
 * Created by mercy on 17-3-18.
 */
public class AssignNode extends ExprNode {
    private ExprNode lhs, rhs;

    public AssignNode(ExprNode lhs, ExprNode rhs) {
        super();
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public Type type() {
        return lhs.type();
    }

    public ExprNode lhs() {
        return lhs;
    }

    public ExprNode rhs() {
        return rhs;
    }

    public void setRHS(ExprNode expr) {
        this.rhs = expr;
    }

    @Override
    public Location location() {
        return lhs.location();
    }

    @Override
    public <S, E> E accept(ASTVisitor<S, E> visitor) {
        return visitor.visit(this);
    }
}
