package frontend.datatype;

public class VoidType extends DataType{
    @Override
    public boolean compatibleWith(DataType dt) {
        return dt instanceof VoidType;
    }
}
