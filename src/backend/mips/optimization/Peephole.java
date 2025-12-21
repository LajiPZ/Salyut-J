package backend.mips.optimization;

import backend.mips.MipsBlock;
import backend.mips.MipsFunction;
import backend.mips.MipsModule;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.Operand;
import utils.DoublyLinkedList;

public class Peephole {

    public void run(MipsModule module) {
        for (MipsFunction function : module.getFunctions()) {
            for (MipsBlock block : function.getBlocks()) {
                for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                    Instruction inst = node.getValue();
                    if (canRemove(inst)) {
                        node.drop();
                    }
                }
            }
        }
    }
    
    private boolean canRemove(Instruction inst) {
        if (inst instanceof Calc calc) {
            if (calc.getOp() == Calc.Op.addiu) {
                Operand[] operands = calc.getOperands();
                if (operands[1] instanceof Immediate imm && imm.asInt() == 0) {
                    // $t0 <- 0 + $t0
                    return operands[0] == operands[2];
                }
                if (operands[2] instanceof Immediate imm && imm.asInt() == 0) {
                    // $t0 <- $t0 + 0
                    return operands[0] == operands[1];
                }
            }
            if (calc.getOp() == Calc.Op.addu) {
                Operand[] operands = calc.getOperands();
                if (operands[1] == AReg.zero) {
                    // $t0 <- $t0 + $0
                    return operands[0] == operands[2];
                }
                if (operands[2] == AReg.zero) {
                    // $t0 <- $0 + $t0
                    return operands[0] == operands[1];
                }
            }
            if (calc.getOp() == Calc.Op.subu) {
                Operand[] operands = calc.getOperands();
                if (operands[2] == AReg.zero) {
                    // $t0 <- $t0 - $0
                    return operands[0] == operands[1];
                }
            }
            
        }
        return false;
    }
}
