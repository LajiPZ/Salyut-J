package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;
import frontend.token.TokenStream;

import java.util.List;

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

    public static FuncDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {

        return null;
    }
}
