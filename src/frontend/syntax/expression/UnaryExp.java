package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.misc.FuncRParams;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public abstract class UnaryExp extends ASTNode {
    public enum Type {
        Call, Op, Primary
    }

    protected Type type;

    protected UnaryExp (Type type) {
        this.type = type;
    }

    public static UnaryExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        if (tokenStream.check(TokenType.Ident) && tokenStream.check(1, TokenType.LeftParen)) {
            Token ident = tokenStream.poll();
            tokenStream.poll(); // (
            FuncRParams params = FuncRParams.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.RightParen)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.peek(-1).getFileLoc())
                );
            }
            return new UnaryCallExp(ident, params);
        } else if (tokenStream.check(TokenType.Plus, TokenType.Minus, TokenType.Not)) {
            Token opToken = tokenStream.poll();
            UnaryOpExp.UnaryOp op = opToken.ofType(TokenType.Plus) ? UnaryOpExp.UnaryOp.PLUS :
                opToken.ofType(TokenType.Minus) ? UnaryOpExp.UnaryOp.MINUS : UnaryOpExp.UnaryOp.NOT;
            return new UnaryOpExp(op, UnaryExp.parse(tokenStream, errors));
        } else {
            return new UnaryPrimaryExp(PrimaryExp.parse(tokenStream, errors));
        }
    }
}
