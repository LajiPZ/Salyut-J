package frontend.llvm.value.instruction;

public enum Operator {

    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    DIV("sdiv"),
    MOD("srem"),
    EQ("icmp eq"),
    NE("icmp ne"),
    LT("icmp slt"),
    GT("icmp sgt"),
    LE("icmp sle"),
    GE("icmp sge")
    ;

    private String operation;

    public int calc(int l, int r) {
        return switch (this) {
            case ADD -> l + r;
            case SUB -> l - r;
            case MUL -> l * r;
            case DIV -> l / r;
            case MOD -> l % r;
            case EQ -> l == r ? 1 : 0;
            case NE -> l != r ? 1 : 0;
            case LT -> l < r ? 1 : 0;
            case GT -> l > r ? 1 : 0;
            case LE -> l <= r ? 1 : 0;
            case GE -> l >= r ? 1 : 0;
            default -> throw new RuntimeException("Unknown operator: " + this);
        };
    }

    Operator(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
