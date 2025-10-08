package frontend.syntax.expression;

import frontend.syntax.misc.FuncRParams;
import frontend.token.Token;

public class UnaryCallExp extends UnaryExp {
    private Token ident;
    private FuncRParams params;

    public UnaryCallExp(Token ident, FuncRParams params) {
        super(Type.Call);
        this.ident = ident;
        this.params = params;
    }

}
