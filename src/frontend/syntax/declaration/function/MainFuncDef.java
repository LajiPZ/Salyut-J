package frontend.syntax.declaration.function;

import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;

public class MainFuncDef extends ASTNode {
    private Token ident;
    private Block block;

    public MainFuncDef(Token ident, Block block) {
        this.ident = ident;
        this.block = block;
    }
}
