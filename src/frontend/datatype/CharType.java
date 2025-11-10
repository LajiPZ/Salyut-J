package frontend.datatype;

public class CharType extends BaseType {
    public CharType() {
        super(8);
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CharType;
    }
}
