package frontend.datatype;

abstract public class BaseType extends DataType {

    private int width;

    public BaseType(int width) {
        this.width = width;
    }

    public int getWidth() {
        return width;
    }

    public boolean compatibleWith(DataType dt) {
        return dt instanceof BaseType && ((BaseType) dt).getWidth() == width;
    }

    @Override
    public String toString() {
        return "i" + width;
    }
}
