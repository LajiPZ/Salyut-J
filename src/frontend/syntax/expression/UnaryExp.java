package frontend.syntax.expression;

import frontend.syntax.ASTNode;

public abstract class UnaryExp extends ASTNode {
    public enum Type {
        Call, Op, Primary
    }

    protected Type type;

    protected UnaryExp (Type type) {
        this.type = type;
    }
}
