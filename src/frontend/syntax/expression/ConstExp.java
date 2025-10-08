package frontend.syntax.expression;

import frontend.syntax.ASTNode;

public class ConstExp extends ASTNode {
    // TODO: 使用的 Ident 必须是常量

    private final AddExp addExp;

    public ConstExp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return addExp;
    }
}
