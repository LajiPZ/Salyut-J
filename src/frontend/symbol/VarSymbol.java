package frontend.symbol;

import frontend.datatype.DataType;
import frontend.llvm.value.Value;

public class VarSymbol extends ValSymbol {
    private boolean isStatic = false;
    private boolean isFromParam = false;

    private Value staticCtrl;

    public VarSymbol(String ident, boolean isStatic, DataType dataType, int scopeCnt) {
        super(ident, Type.Var, dataType, scopeCnt);
        this.isStatic = isStatic;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public void setFromParam() {
        isFromParam = true;
    }

    public boolean isFromParam() { return isFromParam; }

    public void setStaticCtrl(Value staticCtrl) {
        this.staticCtrl = staticCtrl;
    }

    public Value getStaticCtrl() { return staticCtrl; }
}
