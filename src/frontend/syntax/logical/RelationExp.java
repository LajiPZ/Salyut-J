package frontend.syntax.logical;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.AddExp;

import java.util.ArrayList;

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

}
