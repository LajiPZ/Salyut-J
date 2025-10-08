package frontend.syntax.misc;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;

import java.util.ArrayList;

public class FuncRParams extends ASTNode {
    private Exp lExp;
    private ArrayList<Exp> rExps = new ArrayList<>();

    public FuncRParams(Exp lExp) {
        this.lExp = lExp;
    }

    public void addExp (Exp exp) {
        rExps.add(exp);
    }

}
