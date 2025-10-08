package frontend.syntax.logical;

import frontend.syntax.ASTNode;

import java.util.ArrayList;

final public class LAndExp extends ASTNode {
    private final EqExp LEqExp;
    private final ArrayList<EqExp> REqExps = new ArrayList<>();

    public LAndExp(EqExp LEqExp) {
        this.LEqExp = LEqExp;
    }

    public void addREqExp(EqExp EqExp) {
        this.REqExps.add(EqExp);
    }
}
