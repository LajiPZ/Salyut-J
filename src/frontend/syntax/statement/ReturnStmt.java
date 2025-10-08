package frontend.syntax.statement;

import frontend.syntax.expression.Exp;
import frontend.token.Token;

public class ReturnStmt extends Stmt {
    private Token label;
    private Exp expr;

    public ReturnStmt(Token label, Exp expr) {
        super(Type.Return);
        this.label = label;
        this.expr = expr;
    }

    public ReturnStmt(Token label) {
        super(Type.Return);
        this.label = label;
        this.expr = null;
    }
}
