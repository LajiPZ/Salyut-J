package backend.mips;

import frontend.datatype.DataType;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.value.Value;

import java.util.Map;

public class MipsGlobalVariable {

    private DataType dataType;
    private String tag;
    private Map<Integer, Value> initList;
    private String initString;


    public MipsGlobalVariable(DataType dataType, String tag, Map<Integer, Value> initList) {
        this.dataType = dataType;
        this.tag = tag;
        this.initList = initList;
        this.initString = null;
    }

    public MipsGlobalVariable(DataType dataType, String tag, String initString) {
        this.dataType = dataType;
        this.tag = tag;
        this.initList = null;
        this.initString = initString;
    }

    public static MipsGlobalVariable build(GlobalVariable globalVariable) {
        return new MipsGlobalVariable(
            globalVariable.getType(),
            globalVariable.getName(),
            globalVariable.getInitList()
        );
    }
}
