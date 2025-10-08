package frontend.syntax.expression;

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
}
