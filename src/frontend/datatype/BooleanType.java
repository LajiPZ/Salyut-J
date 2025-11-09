package frontend.datatype;

public class BooleanType extends BaseType {

    public BooleanType() {
        super(1);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BooleanType;
    }
}
