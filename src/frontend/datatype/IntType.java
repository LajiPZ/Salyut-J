package frontend.datatype;

public class IntType extends BaseType {

    public IntType() {
        super(32);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof IntType;
    }
}
