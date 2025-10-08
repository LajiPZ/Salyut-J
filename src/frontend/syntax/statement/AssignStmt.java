package frontend.syntax.statement;

import frontend.syntax.expression.Exp;
import frontend.syntax.misc.LVal;

public class AssignStmt extends Stmt {
    private LVal lval;
    private Exp exp;

    public AssignStmt(LVal lval, Exp exp) {
        super(Type.Assign);
        this.lval = lval;
        this.exp = exp;
    }
}
