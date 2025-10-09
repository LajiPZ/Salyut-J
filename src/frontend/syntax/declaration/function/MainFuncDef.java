package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;
import frontend.token.TokenStream;

import java.util.List;

public class MainFuncDef extends ASTNode {
    private Token ident;
    private Block block;

    public MainFuncDef(Token ident, Block block) {
        this.ident = ident;
        this.block = block;
    }

    public static MainFuncDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {

        return null;
    }
}
