package frontend.syntax.statement;

import frontend.IrBuilder;
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

    @Override
    public void visit() {
        if (exp != null) exp.visit();
    }

    public void build(IrBuilder builder) {
        if (exp != null) exp.build(builder);
    }
}
