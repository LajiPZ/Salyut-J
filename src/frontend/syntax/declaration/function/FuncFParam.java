package frontend.syntax.declaration.function;

import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.Token;

public class FuncFParam extends ASTNode {
    private BType type;
    private Token ident;
    private int depth = 0;

    public FuncFParam(BType type, Token ident) {
        this.type = type;
        this.ident = ident;
    }

    public FuncFParam(BType type, Token ident, int depth) {
        this.type = type;
        this.ident = ident;
        this.depth = depth;
    }
}
