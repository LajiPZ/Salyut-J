package frontend.syntax.statement;

import frontend.syntax.expression.Exp;

public class ExpStmt extends Stmt {
    private Exp exp;

    public ExpStmt() {
        super(Type.Exp);
        exp = null;
    }

    public void setExp(Exp exp) {
        this.exp = exp;
    }
}
