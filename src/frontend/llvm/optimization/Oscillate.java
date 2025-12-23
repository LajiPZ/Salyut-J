package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.analysis.ControlFlowAnalysis;
import frontend.llvm.analysis.DominatorAnalysis;
import frontend.llvm.analysis.LoopAnalysis;
import settings.Settings;

import java.util.LinkedList;
import java.util.List;

public class Oscillate implements Pass {
    @Override
    public void run(IrModule module) {
        for (int i = 0; i < Settings.OptimizeConfig.oscillateIterations; i++) {
            new InlineFunction().run(module);
            new ConstantFolding().run(module);
            new SimplifyControlFlow().run(module);
        }
        List<Pass> passes = List.of(
            new ControlFlowAnalysis(),
            new DominatorAnalysis(),
            new EliminateDeadCode()
        );
        for (Pass p : passes) p.run(module);
    }
}
