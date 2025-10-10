package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class FuncFParam extends ASTNode {
    private BType type;
    private Token ident;
    private int depth = 0;

    public FuncFParam(BType type, Token ident) {
        this.type = type;
        this.ident = ident;
    }

    public FuncFParam(BType type, Token ident, int depth) {
        this.type = type;
        this.ident = ident;
        this.depth = depth;
    }

    public static FuncFParam parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        BType type = BType.parse(tokenStream, errors);
        Token ident = tokenStream.next(TokenType.Ident);
        if (tokenStream.checkPoll(TokenType.LeftBracket)) {
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.peek(-1).getFileLoc())
                );
            }
            return new FuncFParam(type, ident, 1);
        } else {
            return new FuncFParam(type, ident);
        }
    }
}
