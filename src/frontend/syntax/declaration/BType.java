package frontend.syntax.declaration;

import frontend.syntax.ASTNode;
import frontend.token.Token;

final public class BType extends ASTNode {
    enum Type {
        Int
    }

    private Type type;

    public BType(Type type) {
        this.type = type;
    }
}
