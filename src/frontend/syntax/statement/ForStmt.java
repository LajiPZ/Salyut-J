package frontend.syntax.statement;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.syntax.misc.LVal;

import java.util.ArrayList;

final public class ForStmt extends ASTNode {
    private LVal lVal;
    private Exp exp;
    private ArrayList<LVal> extraLVals = new ArrayList<>();
    private ArrayList<Exp> extraExps = new ArrayList<>();

    public ForStmt(LVal lVal, Exp exp) {
        this.lVal = lVal;
        this.exp = exp;
    }

    public void addExtraStmt(LVal lVal, Exp exp) {
        extraLVals.add(lVal);
        extraExps.add(exp);
    }
}
