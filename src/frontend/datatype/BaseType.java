package frontend.datatype;

abstract public class BaseType extends DataType {

    private int width;

    public BaseType(int width) {
        this.width = width;
    }

    /**
     * 获取类型的位宽（bits）
     * @return
     */
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

    @Override
    public boolean equals(Object obj) {
        return obj instanceof BaseType && ((BaseType) obj).getWidth() == width;
    }

    @Override
    public int getSize() {
        return (width + 7) / 8;
    }
}
