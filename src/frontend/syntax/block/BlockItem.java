package frontend.syntax.block;

import frontend.syntax.ASTNode;
import frontend.syntax.declaration.object.Decl;
import frontend.syntax.statement.Stmt;

final public class BlockItem extends ASTNode {
    public enum Type {
        Decl, Stmt
    }
    private Type type;
    private Decl decl;
    private Stmt stmt;

    public BlockItem(Decl decl) {
        this.type = Type.Decl;
        this.decl = decl;
        this.stmt = null;
    }

    public BlockItem(Stmt stmt) {
        this.type = Type.Stmt;
        this.stmt = stmt;
        this.decl = null;
    }
}
