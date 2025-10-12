package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class MulExp extends ASTNode {
    public enum MulOperator {
        MUL, DIV, MOD
    }

    private final UnaryExp LUnaryExp;
    private final ArrayList<MulOperator> operators = new ArrayList<>();
    private final ArrayList<UnaryExp> RUnaryExps = new ArrayList<>();

    public MulExp(UnaryExp LUnaryExp) {
        this.LUnaryExp = LUnaryExp;
    }

    public void addRUnaryExp(MulOperator operator, UnaryExp RUnaryExp) {
        operators.add(operator);
        RUnaryExps.add(RUnaryExp);
    }

    public static MulExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        MulExp mulExp = new MulExp(UnaryExp.parse(tokenStream,errors));
        while (tokenStream.check(TokenType.Mul, TokenType.Div, TokenType.Mod)) {
            tokenStream.logParse("<MulExp>");
            Token type = tokenStream.poll();
            MulOperator operator = type.ofType(TokenType.Mul) ? MulOperator.MUL :
                                   type.ofType(TokenType.Div) ? MulOperator.DIV :
                                   MulOperator.MOD;
            mulExp.addRUnaryExp(
                operator,
                UnaryExp.parse(tokenStream, errors)
            );
        }
        tokenStream.logParse("<MulExp>");
        return mulExp;
    }

}