package backend.mips;

import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;

import java.util.List;
import java.util.Map;

public class MipsBuilder {
    // This is designed specifically for a function...
    private int stackSize;
    private Map<BBlock, MipsBlock> blockMap;
    private Map<Function, MipsFunction> functionMap;

    public MipsBuilder(Map<Function, MipsFunction> functionMap, List<BBlock> bblocks, MipsBlock entry, MipsBlock exit) {

    }

    public void addParameters(List<Value> parameters) {
        for (int i = 0; i < parameters.size(); i++) {

        }
    }

    public MipsBlock getMipsBlock(BBlock block) {
        return blockMap.get(block);
    }

    public int getStackSize() {
        return stackSize;
    }
}
