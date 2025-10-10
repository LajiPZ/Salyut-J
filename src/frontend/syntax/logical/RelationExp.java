package frontend.syntax.logical;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.AddExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

final public class RelationExp extends ASTNode {
    public enum RelationOperator {
        LT, GT, LE, GE
    }

    private final AddExp LAddExp;
    private final ArrayList<RelationOperator> operators = new ArrayList<>();
    private final ArrayList<AddExp> RAddExps = new ArrayList<>();

    public RelationExp(AddExp LAddExp) {
        this.LAddExp = LAddExp;
    }

    public void addRAddExp(RelationOperator operator, AddExp RAddExp) {
        operators.add(operator);
        RAddExps.add(RAddExp);
    }

    public static RelationExp parse(TokenStream ts, List<ErrorEntry> errors) {
        RelationExp exp = new RelationExp(AddExp.parse(ts, errors));
        while (ts.check(TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
            // log
            Token t = ts.poll();
            RelationOperator operator = t.ofType(TokenType.LT) ? RelationOperator.LT :
                                        t.ofType(TokenType.GT) ? RelationOperator.GT :
                                        t.ofType(TokenType.LE) ? RelationOperator.LE :
                                        RelationOperator.GE;
            exp.addRAddExp(operator, AddExp.parse(ts, errors));
        }
        // log
        return exp;
    }

}
