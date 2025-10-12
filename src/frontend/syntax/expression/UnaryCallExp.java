package frontend.syntax.expression;

import frontend.syntax.misc.FuncRParams;
import frontend.token.Token;

public class UnaryCallExp extends UnaryExp {
    private Token ident;
    private FuncRParams params = null;

    public UnaryCallExp(Token ident) {
        super(Type.Call);
        this.ident = ident;
    }

    public void setFuncRParams(FuncRParams params) { this.params = params; }

}
