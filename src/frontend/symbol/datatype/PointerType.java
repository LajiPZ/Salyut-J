package frontend.symbol.datatype;

public class PointerType extends DataType {
    private DataType base;

    public PointerType(DataType base) {
        this.base = base;
    }
}
