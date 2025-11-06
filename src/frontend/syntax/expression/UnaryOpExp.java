package frontend.syntax.expression;

import frontend.datatype.DataType;

// I can't come up with a better name for it...
public class UnaryOpExp extends UnaryExp{
    enum UnaryOp {
        PLUS, MINUS, NOT // TODO：NOT仅出现在条件表达式
    }

    private final UnaryOp op;
    private final UnaryExp exp;

    public UnaryOpExp(UnaryOp op, UnaryExp exp) {
        super(UnaryExp.Type.Op);
        this.op = op;
        this.exp = exp;
    }

    public void visit() {
        exp.visit();
    }

    public int calc() {
        switch (op) {
            case PLUS -> {
                return exp.calc();
            }
            case MINUS -> {
                return -exp.calc();
            }
            default -> {
                throw new Error("Unsupported op in calc(): " + op);
            }
        }
    }

    public DataType calcType() {
        return exp.calcType();
    }

}
