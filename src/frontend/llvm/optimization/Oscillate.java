package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.analysis.ControlFlowAnalysis;
import frontend.llvm.analysis.DominatorAnalysis;
import frontend.llvm.analysis.LoopAnalysis;

import java.util.List;

public class Oscillate implements Pass {
    @Override
    public void run(IrModule module) {
        for (int i = 0; i < 7; i++) {
            List<Pass> passes = List.of(
                new InlineFunction(),
                new ConstantFolding(),
                new ControlFlowAnalysis(),
                new DominatorAnalysis(),
                new LoopAnalysis(),

                new SimplifyControlFlow(),
                new ControlFlowAnalysis(),
                new DominatorAnalysis(),
                new LoopAnalysis(),
                new EliminateDeadCode()
            );
            for (Pass p : passes) p.run(module);
        }
    }
}
