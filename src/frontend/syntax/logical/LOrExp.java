package frontend.syntax.logical;

import frontend.syntax.ASTNode;

import java.util.ArrayList;

final public class LOrExp extends ASTNode {
    private final LAndExp LLAndExp;
    private final ArrayList<LAndExp> RLAndExps = new ArrayList<>();

    public LOrExp(LAndExp LLAndExp) {
        this.LLAndExp = LLAndExp;
    }

    public void addRLAndExp(LAndExp LAndExp) {
        this.RLAndExps.add(LAndExp);
    }
}
