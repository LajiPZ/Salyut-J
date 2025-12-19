package frontend.llvm.tools;

import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.Inst;

public class UseRecord {
    private Value value;
    private Inst user;
    private int position;

    public Inst getUser() {
        return user;
    }

    public Value getValue() {
        return value;
    }

    public UseRecord(Value value, Inst user, int position) {
        this.value = value;
        this.user = user;
        this.position = position;
    }
}
