package backend.mips;

import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.llvm.value.GlobalVariable;
import frontend.llvm.value.Value;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class MipsGlobalVariable {

    private DataType dataType;
    private String tag;
    private List<Integer> initList;
    private String initString;

    public MipsGlobalVariable(DataType dataType, String tag, List<Integer> initList) {
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

    public String toMIPS() {
        StringBuilder sb = new StringBuilder();
        sb.append(tag).append(": ");
        if (initList != null) {
            if (dataType.getFinalDataType().compatibleWith(new IntType())) {
                sb.append(".word ");
            } else {
                sb.append(".byte ");
            }
            sb.append(initList.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        } else if (initString != null) {
            sb.append(".asciiz \"").append(initString.replace("\n", "\\n")).append("\"");
        } else {
            throw new RuntimeException("Expected initStr/List but found null!!");
        }
        return sb.toString();
    }
}
