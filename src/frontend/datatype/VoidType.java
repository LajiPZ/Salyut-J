package frontend.datatype;

public class VoidType extends DataType{
    @Override
    public boolean compatibleWith(DataType dt) {
        return dt instanceof VoidType;
    }

    @Override
    public String toString() {
        return "void";
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof VoidType;
    }

    @Override
    public int getSize() {
        return 0;
    }
}
