package frontend.syntax.expression;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class AddExp extends ASTNode {
    public enum AddOperator {
        ADD, SUB
    }

    private final MulExp LMulExp;
    private final ArrayList<AddOperator> operators = new ArrayList<>();
    private final ArrayList<MulExp> RMulExps = new ArrayList<>();

    public AddExp(MulExp LMulExp) {
        this.LMulExp = LMulExp;
    }

    public void addRMulExp(AddOperator operator, MulExp RMulExp) {
        this.operators.add(operator);
        this.RMulExps.add(RMulExp);
    }

    public static AddExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        AddExp exp = new AddExp(MulExp.parse(tokenStream, errors));
        while (tokenStream.check(TokenType.Plus, TokenType.Minus)) {
            tokenStream.logParse("<AddExp>");
            Token type = tokenStream.poll();
            AddOperator operator = type.ofType(TokenType.Plus) ? AddOperator.ADD : AddOperator.SUB;
            exp.addRMulExp(
                operator, MulExp.parse(tokenStream,errors)
            );
        }
        tokenStream.logParse("<AddExp>");
        return exp;
    }


    public void visit() {
        LMulExp.visit();
        for (MulExp exp : RMulExps) {
            exp.visit();
        }
    }
}
