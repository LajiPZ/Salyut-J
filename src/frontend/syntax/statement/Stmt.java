package frontend.syntax.statement;

import frontend.syntax.ASTNode;

abstract public class Stmt extends ASTNode {
    public enum Type {
        Assign, Exp, Block, If, For, Break, Continue, Return, Printf
    }

    private Type type;

    public Stmt(Type type) {
        this.type = type;
    }
}
