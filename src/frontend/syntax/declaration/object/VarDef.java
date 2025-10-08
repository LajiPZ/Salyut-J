package frontend.syntax.declaration.object;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;

public class VarDef extends ASTNode {
    private Token ident;
    private ConstExp indexExp;
    private InitVal initVal;

    public VarDef(Token ident, ConstExp indexExp, InitVal initVal) {
        this.ident = ident;
        this.indexExp = indexExp;
        this.initVal = initVal;
    }

    public VarDef(Token ident, ConstExp indexExp) {
        this.ident = ident;
        this.indexExp = indexExp;
        this.initVal = null;
    }

    public VarDef(Token ident) {
        this.ident = ident;
        this.indexExp = null;
        this.initVal = null;
    }

    public VarDef(Token ident, InitVal initVal) {
        this.ident = ident;
        this.indexExp = null;
        this.initVal = initVal;
    }
}
