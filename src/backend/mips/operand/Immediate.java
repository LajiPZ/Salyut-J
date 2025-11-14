package backend.mips.operand;

public class Immediate extends Operand {
    // tag or actual offset
    private Object value;

    public Immediate(int value) {
        this.value = value;
    }
}
