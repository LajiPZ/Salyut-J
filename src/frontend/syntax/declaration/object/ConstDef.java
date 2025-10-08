package frontend.syntax.declaration.object;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;

public class ConstDef extends ASTNode {
    private Token ident;
    private ConstExp indexExp;
    private ConstInitVal initVal;

    public ConstDef(Token ident, ConstExp indexExp, ConstInitVal initVal) {
        this.ident = ident;
        this.indexExp = indexExp;
        this.initVal = initVal;
    }

    public ConstDef(Token ident, ConstInitVal initVal) {
        this.ident = ident;
        this.initVal = initVal;
        this.indexExp = null;
    }
}
