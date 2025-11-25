package backend.mips.utils.spillLoc;

public class MemSpillLoc extends SpillLoc {
    private int offset; // offset to frame pointer

    public MemSpillLoc(int offset) {
        this.offset = offset;
    }

    public int getOffset() {
        return offset;
    }
}
