package frontend.syntax.statement;

import frontend.token.Token;

public class BreakStmt extends Stmt {
    private Token label;

    public BreakStmt(Token label) {
        super(Type.Break);
        this.label = label;
    }
}
