package frontend.syntax.expression;

import frontend.IrBuilder;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICalc;
import frontend.llvm.value.instruction.Operator;

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

    public Value build(IrBuilder builder) {
        Value val = exp.build(builder);
        // TODO: 转换类型？
        switch (op) {
            case PLUS -> {
                return val;
            }
            // TODO: const
            case MINUS -> {
                return new ICalc(
                    Operator.SUB,
                    IntConstant.zero,
                    val
                );
            }
            case NOT -> {
                // TODO
            }
        }
    }
}
