package frontend.syntax.logical;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class EqExp extends ASTNode {
    public enum EqOperator {
        EQ, NE
    }

    private final RelationExp LRelExp;
    private final ArrayList<EqOperator> operators = new ArrayList<>();
    private final ArrayList<RelationExp> RRelExps = new ArrayList<>();

    public EqExp(RelationExp LRelExp) {
        this.LRelExp = LRelExp;
    }

    public void addRRelExp(EqOperator EqOperator, RelationExp RRelExp) {
        this.operators.add(EqOperator);
        this.RRelExps.add(RRelExp);
    }

    public static EqExp parse(TokenStream ts, List<ErrorEntry> errors) {
        EqExp exp = new EqExp(RelationExp.parse(ts, errors));
        while (ts.check(TokenType.EQ, TokenType.NE)) {
            // log
            Token operator = ts.poll();
            exp.addRRelExp(
                operator.ofType(TokenType.EQ) ? EqOperator.EQ : EqOperator.NE,
                RelationExp.parse(ts,errors)
            );
        }
        // log
        return exp;
    }

}
