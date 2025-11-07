package frontend.llvm.tools;

import frontend.llvm.value.BBlock;
import frontend.llvm.value.instruction.IBranch;

import java.util.ArrayList;
import java.util.List;

public class LoopInfo {
    private BBlock condBBlk;
    private List<IBranch> breaks;
    private List<IBranch> continues;

    public LoopInfo(BBlock condBBlk) {
        this.condBBlk = condBBlk;
        this.breaks = new ArrayList();
        this.continues = new ArrayList<>();
    }

    public void fillBreakTarget(BBlock exitBBlk) {
        breaks.forEach( branch ->
                branch.fillNullTarget(exitBBlk)
        );
    }

    public void fillContinueTarget(BBlock updateBBlk) {
        continues.forEach(branch ->
            branch.fillNullTarget(updateBBlk)
        );
    }

    public void addBreak(IBranch branch) {
        breaks.add(branch);
    }

    public void addContinue(IBranch branch) {
        continues.add(branch);
    }
}
