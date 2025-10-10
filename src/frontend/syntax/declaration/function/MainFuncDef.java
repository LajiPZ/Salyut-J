package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.block.Block;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class MainFuncDef extends ASTNode {
    private Token ident;
    private Block block = null;

    public MainFuncDef(Token ident) {
        this.ident = ident;
    }

    public void setBlock(Block block) {
        this.block = block;
    }

    public static MainFuncDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        tokenStream.next(TokenType.Int);
        MainFuncDef mainFuncDef = new MainFuncDef(tokenStream.next(TokenType.Main));
        tokenStream.next(TokenType.LeftParen);
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.peek(-1).getFileLoc())
            );
        }
        mainFuncDef.setBlock(Block.parse(tokenStream, errors));
        return mainFuncDef;
    }
}
