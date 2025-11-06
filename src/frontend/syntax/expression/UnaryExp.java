package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.datatype.DataType;
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
        UnaryExp retExp;
        if (tokenStream.check(TokenType.Ident) && tokenStream.check(1, TokenType.LeftParen)) {
            Token ident = tokenStream.poll();
            retExp = new UnaryCallExp(ident);
            tokenStream.poll(); // (
            // 可能出现无可选项，但缺右括号的情况
            // 此时parse必定出例外，故套个catch，交给下面括号匹配检查即可
            if (!tokenStream.check(TokenType.RightParen)) {
                try {
                    FuncRParams params = FuncRParams.parse(tokenStream, errors);
                    ((UnaryCallExp)retExp).setFuncRParams(params);
                } catch (Exception e) {}
            }
            if (!tokenStream.checkPoll(TokenType.RightParen)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
                );
            }

        } else if (tokenStream.check(TokenType.Plus, TokenType.Minus, TokenType.Not)) {
            Token opToken = tokenStream.poll();
            UnaryOpExp.UnaryOp op = opToken.ofType(TokenType.Plus) ? UnaryOpExp.UnaryOp.PLUS :
                opToken.ofType(TokenType.Minus) ? UnaryOpExp.UnaryOp.MINUS : UnaryOpExp.UnaryOp.NOT;
            tokenStream.logParse("<UnaryOp>"); // 懒得再给UnaryOp写了，就这样吧
            retExp = new UnaryOpExp(op, UnaryExp.parse(tokenStream, errors));
        } else {
            retExp = new UnaryPrimaryExp(PrimaryExp.parse(tokenStream, errors));
        }
        tokenStream.logParse("<UnaryExp>");
        return retExp;
    }

    abstract public void visit();

    abstract public int calc();

    abstract public DataType calcType();
}
