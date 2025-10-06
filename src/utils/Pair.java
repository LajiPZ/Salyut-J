package utils;

public class Pair<K,V> {

    K value1;
    V value2;

    public Pair(K value1, V value2) {
        this.value1 = value1;
        this.value2 = value2;
    }

    public K getValue1() {
        return value1;
    }

    @Override
    public String toString() {
        return "<" + value1.toString() + "," + value2.toString() + ">";
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Pair) {
            return value1.equals(((Pair) o).value1) && value2.equals(((Pair) o).value2);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return value1.hashCode() ^ value2.hashCode();
    }
}
