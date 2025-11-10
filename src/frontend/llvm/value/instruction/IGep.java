package frontend.llvm.value.instruction;

import frontend.datatype.PointerType;
import frontend.llvm.value.Value;

public class IGep extends Inst {
    // Get Element Pointer

    private boolean fromArgs;

    public IGep(Value pointer, Value offset, boolean fromArgs) {
        super(
            "%" + Value.counter.get(),
            fromArgs ?
                :

        );
        this.fromArgs = fromArgs;
        addOperand(pointer);
        addOperand(offset);
    }


    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" = getelementptr ");
        sb.append(((PointerType)(getOperand(0).getType())).getBaseType()).append(", ");
        sb.append(getOperand(0)).append(", ");

    }
}
