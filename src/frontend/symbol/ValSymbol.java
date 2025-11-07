package frontend.symbol;

import frontend.llvm.value.Value;
import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.datatype.PointerType;
import frontend.datatype.init.InitType;

public class ValSymbol extends Symbol {
    public enum Type {
        Const, Var
    }

    private boolean isFuncParam = false;
    private InitType initType = null;
    private Type type;
    private DataType dataType;

    private Value value; // LLVM

    public ValSymbol(String ident, Type type, DataType dataType, int scopeCnt) {
        super(ident, scopeCnt);
        this.type = type;
        this.dataType = dataType;
    }

    public void setInitType(InitType initType) {
        this.initType = initType;
    }

    public boolean hasInitType() {
        return initType != null;
    }

    public InitType getInitType() {
        return initType;
    }

    public DataType getDataType() {
        return dataType;
    }

    public void setValue(Value value) { this.value = value; }

    public Value getValue() { return value; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(super.getScopeCnt());
        sb.append(" ");
        sb.append(super.getIdent());
        sb.append(" ");
        if (type == Type.Const) sb.append("Const");
        else if (((VarSymbol)this).isStatic()) sb.append("Static");
        if (dataType instanceof IntType) sb.append("Int");
        else if (dataType instanceof ArrayType || dataType instanceof PointerType) sb.append("IntArray"); // 这里偷了个懒
        return sb.toString();
    }

    public boolean isFromParam() {
        return isFuncParam;
    }
}
