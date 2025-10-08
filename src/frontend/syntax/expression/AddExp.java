package frontend.syntax.expression;

import frontend.syntax.ASTNode;

import java.util.ArrayList;

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

    static public AddExp parse() {

        return null;
    }

}
