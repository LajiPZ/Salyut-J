package frontend.syntax.expression;

import frontend.IrBuilder;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;

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

    public Value build(IrBuilder builder) {
        return exp.build(builder);
    }
}
