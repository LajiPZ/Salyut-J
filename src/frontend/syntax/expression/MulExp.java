package frontend.syntax.expression;

import frontend.syntax.ASTNode;

import java.util.ArrayList;

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

}