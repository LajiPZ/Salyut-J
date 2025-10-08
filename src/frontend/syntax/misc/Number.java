package frontend.syntax.misc;

import frontend.syntax.ASTNode;

public class Number extends ASTNode {
    private int value;

    public Number(int value) {
        this.value = value;
    }
}
