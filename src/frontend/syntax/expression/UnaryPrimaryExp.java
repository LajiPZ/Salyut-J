package frontend.syntax.expression;

import frontend.datatype.DataType;

public class UnaryPrimaryExp extends UnaryExp {
    private final PrimaryExp exp;

    public UnaryPrimaryExp(PrimaryExp exp) {
        super(Type.Primary);
        this.exp = exp;
    }

    public void visit() {
        exp.visit();
    }

    public int calc() {
        return exp.calc();
    }

    public DataType calcType() {
        return exp.calcType();
    }
}
