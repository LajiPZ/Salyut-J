package frontend.syntax.declaration.function;

import frontend.syntax.ASTNode;

public class FuncType extends ASTNode {
    public enum Type {
        Int, Void
    }

    private Type type;

    public FuncType(Type type) {
        this.type = type;
    }
}
