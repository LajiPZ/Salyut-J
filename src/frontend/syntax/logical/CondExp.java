package frontend.syntax.logical;

import frontend.syntax.ASTNode;

final public class CondExp extends ASTNode {
    private final LOrExp lOrExp;

    public CondExp(LOrExp lOrExp) {
        this.lOrExp = lOrExp;
    }

    public LOrExp getLOrExp() {
        return lOrExp;
    }
}
