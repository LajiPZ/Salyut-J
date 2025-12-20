package frontend.llvm.value.instruction;

import frontend.datatype.ArrayType;
import frontend.datatype.PointerType;
import frontend.llvm.value.Value;

import java.util.Map;

public class IGep extends Inst {
    // Get Element Pointer

    private boolean fromArgs;

    public IGep(Value pointer, Value offset, boolean fromArgs) {
        super(
            "%" + Value.counter.get(),
            // 下面指示的是，gep之后，gep指令对应指针的类型是什么！
            fromArgs ? new PointerType( // int[][6] -> (int[6])*
                    ((PointerType)pointer.getType()).getBaseType()
                ) : new PointerType( // (int[5][6])* -> (int[6])*
                    ((ArrayType)((PointerType)pointer.getType()).getBaseType()).getBaseType()
                )
        );
        this.fromArgs = fromArgs;
        addOperand(pointer);
        addOperand(offset);
    }

    public boolean isFromArgs() {
        return fromArgs;
    }

    @Override
    public String toLLVM() {
        StringBuilder sb = new StringBuilder();
        sb.append(getName()).append(" = getelementptr ");
        sb.append(((PointerType)(getOperand(0).getType())).getBaseType()).append(", ");
        sb.append(getOperand(0)).append(", ");
        if (!fromArgs) {
            // 此时传入的是(int[2][5])*，所以有这一步
            // 来自参数时，callGep()已经load了，传入的直接就是(int[5])*
            sb.append("i32 0, ");
        }
        sb.append(getOperand(1));
        return sb.toString();
    }

    @Override
    public Integer numbering() {
        return getOperand(0).hashCode() ^ getOperand(1).hashCode();
    }

    @Override
    public boolean numberingEquals(Inst inst) {
        if (!(inst instanceof IGep other)) {
            return false;
        }
        return getOperand(0).equals(other.getOperand(0))
            && getOperand(1).equals(other.getOperand(1))
            && fromArgs == other.fromArgs;
    }

    @Override
    public Inst clone() {
        return new IGep(
            getOperand(0),
            getOperand(1),
            fromArgs
        );
    }
}
