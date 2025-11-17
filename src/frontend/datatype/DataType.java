package frontend.datatype;

abstract public class DataType {

    abstract public boolean compatibleWith(DataType dt);

    /**
     * 获取类型大小，单位为字节（byte, 1 byte = 8 bits）
     * @return
     */
    abstract public int getSize();

    public DataType getFinalDataType() {
        if (this instanceof ArrayType) {
            return ((ArrayType)this).getBaseType().getFinalDataType();
        } else if (this instanceof PointerType) {
            return ((PointerType)this).getBaseType().getFinalDataType();
        } else {
            return this;
        }
    }
}
