package frontend.syntax.declaration.object;

import frontend.syntax.ASTNode;

abstract public class Decl extends ASTNode {
    public enum Type {
        ConstDecl, VarDecl
    }

    private Type type;

    public Decl(Type type) {
        this.type = type;
    }
}
