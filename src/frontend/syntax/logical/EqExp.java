package frontend.syntax.logical;

import frontend.syntax.ASTNode;

import java.util.ArrayList;

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

}
