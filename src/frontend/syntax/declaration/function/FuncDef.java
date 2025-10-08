package frontend.syntax.declaration.function;

import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;

public class FuncDef extends ASTNode {
    private FuncType type;
    private Token ident;
    private FuncFParams fParams;
    private Block block;

    public FuncDef(FuncType type, Token ident, FuncFParams fParams, Block block) {
        this.type = type;
        this.ident = ident;
        this.fParams = fParams;
        this.block = block;
    }

    public FuncDef(FuncType type, Token ident, Block block) {
        this.type = type;
        this.ident = ident;
        this.block = block;
        this.fParams = null;
    }
}
