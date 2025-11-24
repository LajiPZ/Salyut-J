package backend.mips.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * 活跃变量分析。
 * in/out记录进入/离开块时活跃的全局变量。
 * local记录块内局部变量。
 * @param <T>
 */
public class BlockLivingData<T> {
    private HashSet<T> in, out, local;

    public BlockLivingData() {
        this.in = new HashSet<>();
        this.out = new HashSet<>();
        this.local = new HashSet<>();
    }

    public Set<T> getIn() {
        return in;
    }

    public Set<T> getOut() {
        return out;
    }

    public Set<T> getLocal() {
        return local;
    }

    public static <T> void minus(Set<T> src, Set<T> operand) {
        src.removeAll(operand);
    }

    public static <T> void union(Set<T> dest, Set<T> src) {
        dest.addAll(src);
    }

}
