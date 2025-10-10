package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.misc.LVal;
import frontend.syntax.misc.Number;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class PrimaryExp extends ASTNode {
    public enum Type {
        Exp, LVal, Number
    }

    private final Type type;
    private final Object value;

    public PrimaryExp(Exp exp) {
        this.type = Type.Exp;
        this.value = exp;
    }

    public PrimaryExp(LVal lval) {
        this.type = Type.LVal;
        this.value = lval;
    }

    public PrimaryExp(Number number) {
        this.type = Type.Number;
        this.value = number;
    }

    public static PrimaryExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        if (tokenStream.checkPoll(TokenType.LeftParen)) {
            Exp exp = Exp.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.RightParen)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.peek(-1).getFileLoc())
                );
            }
            return new PrimaryExp(exp);
        }
        if (tokenStream.check(TokenType.Int)) {
            return new PrimaryExp(Number.parse(tokenStream, errors));
        }
        return new PrimaryExp(LVal.parse(tokenStream, errors));
    }

}
