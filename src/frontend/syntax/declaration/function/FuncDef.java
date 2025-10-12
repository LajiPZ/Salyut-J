package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class FuncDef extends ASTNode {
    private FuncType type;
    private Token ident;
    private FuncFParams fParams = null;
    private Block block = null;

    public FuncDef(FuncType type, Token ident) {
        this.type = type;
        this.ident = ident;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public void setfParams(FuncFParams fParams) {
        this.fParams = fParams;
    }

    public static FuncDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        FuncType type = FuncType.parse(tokenStream, errors);
        Token ident = tokenStream.next(TokenType.Ident);
        FuncDef funcDef = new FuncDef(type, ident);
        tokenStream.next(TokenType.LeftParen);
        if (!tokenStream.check(TokenType.RightParen)) {
            funcDef.setfParams(FuncFParams.parse(tokenStream, errors));
        }
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.peek(-1).getFileLoc())
            );
        }
        funcDef.setBlock(Block.parse(tokenStream, errors));
        tokenStream.logParse("<FuncDef>");
        return funcDef;
    }
}
