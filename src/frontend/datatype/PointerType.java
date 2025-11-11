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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof PointerType && ((PointerType) obj).getBaseType().equals(base);
    }

    @Override
    public int getSize() {
        return 4; // 32-bit addr
    }
}
