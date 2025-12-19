package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import utils.Counter;

public class RenameValues implements Pass {

    @Override
    public void run(IrModule module) {
        for (Function function : module.getFunctions()) {
            execute(function);
        }
    }

    private Counter counter = new Counter();

    private void updateName(Value value) {
        if (value instanceof IntConstant) {
            return;
        }
        if (value.getName() == null || value.getName().isEmpty() || value.getName().charAt(0) == '@') {
            return;
        }
        String prefix = value.getName().charAt(0) == '%' ? "%" : "";
        value.setName(prefix + counter.get());
    }

    private void execute(Function function) {
        counter.reset();
        function.getParams().forEach(this::updateName);
        function.getBBlocks().forEach(block -> {
            this.updateName(block);
            block.getInstructions().forEach(instruction -> {
                this.updateName(instruction.getValue());
            });
        });
    }
}
