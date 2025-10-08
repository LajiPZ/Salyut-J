package frontend.syntax.expression;

import frontend.syntax.ASTNode;

final public class Exp extends ASTNode {
    private final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return this.addExp;
    }
}
