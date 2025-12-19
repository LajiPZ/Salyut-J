package frontend.llvm.value.instruction;

import frontend.datatype.DataType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;
import utils.Pair;

import java.util.ArrayList;
import java.util.List;

public class IPhi extends Inst {
    public IPhi(DataType type) {
        super("%" + Value.counter.get(), type);
    }

    public void addSourcePair(BBlock source, Value value) {
        addOperand(source);
        addOperand(value);
    }

    public List<Pair<BBlock, Value>> getSourcePairs() {
        List<Pair<BBlock, Value>> pairs = new ArrayList<>();
        for (int i = 0; i < getOperands().size(); i+=2) {
            pairs.add(
                new Pair<>((BBlock) getOperand(i), getOperand(i+1))
            );
        }
        return pairs;
    }

    public void dropSourcePair(int index) {
        dropOperand(index * 2);
        dropOperand(index * 2);
    }

    @Override
    public Integer numbering() {
        return getOperands().stream().map(Value::hashCode).reduce(0, Integer::sum);
    }

    @Override
    public boolean numberingEquals(Inst inst) {
        // 由于是LVN，由于我们Phi的构建方法，可以保证等价的Phi，sourcePair的顺序是一样的
        if (!(inst instanceof IPhi other)) {
            return false;
        }
        if (this.getOperands().size() != other.getOperands().size()) {
            return false;
        }
        for (int i = 0; i < this.getOperands().size(); i++) {
            if (!this.getOperands().get(i).equals(other.getOperands().get(i))) {
                return false;
            }
        }
        return true;
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
