package frontend.datatype;

public class PointerType extends DataType {
    private DataType base;

    public PointerType(DataType base) {
        this.base = base;
    }

    public DataType getBaseType() {
        return base;
    }

    @Override
    public boolean compatibleWith(DataType dt) {
        return dt instanceof PointerType && ((PointerType) dt).getBaseType().compatibleWith(base);
    }

    @Override
    public String toString() {
        return base.toString() + "*";
    }
}
