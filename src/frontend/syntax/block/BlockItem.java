package frontend.syntax.block;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.object.Decl;
import frontend.syntax.statement.Stmt;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

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

    public static BlockItem parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        BlockItem blockItem;
        if (tokenStream.check(
            TokenType.Const, TokenType.Static,
            TokenType.Int // BType可用的所有类型
        )) {
            blockItem = new BlockItem(Decl.parse(tokenStream, errors));
        } else {
            blockItem = new BlockItem(Stmt.parse(tokenStream, errors));
        }
        // tokenStream.logParse("<BlockItem>");
        return blockItem;
    }

    public void visit() {
        if (type == Type.Decl) {
            decl.visit();
        } else {
            stmt.visit();
        }
    }
}
