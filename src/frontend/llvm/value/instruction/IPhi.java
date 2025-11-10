package frontend.llvm.value.instruction;

import frontend.datatype.DataType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;

public class IPhi extends Inst {
    public IPhi(DataType type) {
        super("%" + Value.counter.get(), type);
    }

    public void addSourcePair(BBlock source, Value value) {
        addOperand(source);
        addOperand(value);
    }


    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" = ").append("phi ").append(getType()).append(" ");
        for (int i = 0; i < getOperands().size(); i += 2) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append("[");
            sb.append(getOperand(i+1).getName()).append(", %").append(getOperand(i).getName());
            sb.append("]");
        }
        return sb.toString();
    }
}
