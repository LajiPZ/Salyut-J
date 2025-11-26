package backend.mips.process;

import backend.mips.MipsBlock;
import backend.mips.MipsFunction;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.Jump;
import backend.mips.instruction.Phi;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.Operand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

// TODO: 注意，这个实现忽略了Phi的“并行复制”属性，省去分配多的PReg之虞
// 为什么可行？
// 因为LLVM IR是SSA，此时我们只是将一个结果写到了IPhi对应的新VReg里
// 且我们只在条件短路时用了IPhi，这个VReg不会用于给任何变量赋值，故没有任何影响！

public class RemovePhi {
    private MipsFunction function;

    public RemovePhi(MipsFunction function) {
        this.function = function;
    }

    public void run(){
        HashMap<MipsBlock, List<Instruction>> toBeInserted = new HashMap<>();

        // Well, to prevent concurrentModification, we have to do something stupid...
        ArrayList<MipsBlock> toBeAdded = new ArrayList<>();

        for (MipsBlock block : function.getBlocks()) {
            HashMap<MipsBlock, MipsBlock> intermediateBlkMap = new HashMap<>();

            for (Instruction inst : block.getInstructions()) {
                if (!(inst instanceof Phi phi)) {
                    continue;
                }
                // Now we see a Phi instruction
                for (int i = 0; i < phi.getOperandCount(); i++) {
                    MipsBlock srcBlk = phi.getSource(i);
                    Operand op = phi.getOperand(i);
                    if (srcBlk.getSuccessors().size() == 1) {
                        // 前序块只会来到当前块，直接插入move
                        toBeInserted.computeIfAbsent(srcBlk, k -> new LinkedList<>()).addAll(
                            List.of(
                                // TODO: 如果是并行复制，那么这里中间要加一个VReg temp = new VReg();
                                buildMove(phi.getRes(), op)
                            )
                        );
                    } else {
                        // 前序块下一步的分支有多个可能；每个可能加一个中间块，处理所有后续块可能中，phi赋值的问题
                        if (!intermediateBlkMap.containsKey(srcBlk)) {
                            // 创建唯一的一个中间块，处理phi
                            MipsBlock newBlk = new MipsBlock(srcBlk + ".phi" + block);
                            // 对应的，建立的转移图要改
                            MipsBlock.removeEdge(srcBlk, block);
                            MipsBlock.addEdge(srcBlk, newBlk);
                            MipsBlock.addEdge(newBlk, block);
                            // 处理srcBlk的转移目标；引到newBlk上，newBlk之后再加jump，转移到原来的跳转目标
                            srcBlk.replaceAllBranchTarget(block, newBlk);
                            intermediateBlkMap.put(srcBlk, newBlk);
                        }
                        MipsBlock newBlk = intermediateBlkMap.get(srcBlk);
                        // TODO: 如果是并行复制，那么这里要先全部赋值到各个temp上，再从temp赋值到phi上
                        newBlk.addInstruction(buildMove(phi.getRes(), op));
                    }
                }
            }

            // Remove Phi in the ASM
            block.removeAllPhi();

            // 最后添加中间块的跳转指令
            for (MipsBlock newBlk : intermediateBlkMap.values()) {
                // function.addBlock(newBlk);
                toBeAdded.add(newBlk);
                newBlk.addInstruction(new Jump(Jump.Op.j, block));
            }
        }
        for (var entry: toBeInserted.entrySet()) {
            System.out.println(function.toString() + entry.getKey());
            MipsBlock srcBlk = entry.getKey();
            for (Instruction inst : entry.getValue()) {
                // TODO: 如果是并行复制，也要做到先全部赋值到各个temp上，再从temp赋值到phi上
                srcBlk.insertBeforeLastInstruction(inst);
            }
        }
        for (MipsBlock newBlk : toBeAdded) {
            function.addBlock(newBlk);
        }
    }

    private static Instruction buildMove(Operand dest, Operand src) {
        if (src instanceof Immediate) {
            return new Calc(Calc.Op.addiu, dest, AReg.zero, src);
        } else {
            return new Calc(Calc.Op.addiu, dest, src, new Immediate(0));
        }
    }
}

