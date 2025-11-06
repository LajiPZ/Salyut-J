package frontend.llvm.value.instruction;

import frontend.llvm.value.Value;
import frontend.datatype.DataType;

import java.util.ArrayList;
import java.util.List;


/**
 * 按照教程中的LLVM IR结构，应该还有User，Inst继承User类
 * 考虑到Inst是User的唯一子类，此处选择跳过User，直接继承自Value
 */
abstract public class Inst extends Value {

    protected List<Value> operands;

    public Inst(String name, DataType type) {
        super(name, type);
        this.operands = new ArrayList<>();
    }

    abstract public String toLLVM();
}
