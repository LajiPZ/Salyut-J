package frontend.llvm.value.instruction;

public enum Operator {

    ADD("add"),
    SUB("sub"),
    MUL("mul"),
    DIV("sdiv"),
    MOD("srem"),
    ;

    private String operation;

    Operator(String operation) {
        this.operation = operation;
    }
}
