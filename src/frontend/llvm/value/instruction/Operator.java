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

    Operator(String operation) {
        this.operation = operation;
    }

    public String getOperation() {
        return operation;
    }
}
