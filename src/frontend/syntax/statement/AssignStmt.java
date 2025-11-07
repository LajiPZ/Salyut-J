package frontend.syntax.statement;

import frontend.IrBuilder;
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

    @Override
    public void visit() {
        lval.visit(true);
        exp.visit();
    }

    public void build(IrBuilder builder) {
        builder.doAssign(lval,exp.build(builder));
    }
}
