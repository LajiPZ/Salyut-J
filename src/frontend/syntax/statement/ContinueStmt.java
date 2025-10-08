package frontend.syntax.statement;

import frontend.token.Token;

public class ContinueStmt extends Stmt {
    private Token label;

    public ContinueStmt(Token label) {
        super(Type.Continue);
        this.label = label;
    }
}
