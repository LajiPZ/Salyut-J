package frontend.syntax.misc;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.token.Token;

public class LVal extends ASTNode {
    private Token ident;
    private Exp index;

    public LVal(Token ident) {
        this.ident = ident;
        this.index = null;
    }

    public LVal(Token ident, Exp index) {
        this.ident = ident;
        this.index = index;
    }
}
