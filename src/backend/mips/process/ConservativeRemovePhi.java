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
import backend.mips.operand.VReg;
import utils.DoublyLinkedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

// 保守的RemovePhi实现；260109测试发现，原实现仍然会在一些case上翻车

public class ConservativeRemovePhi {
    private MipsFunction function;

    public ConservativeRemovePhi(MipsFunction function) {
        this.function = function;
    }

    public void run(){
        HashMap<MipsBlock, List<Instruction>> toBeInserted = new HashMap<>();

        // Well, to prevent concurrentModification, we have to do something stupid...
        // But with DoublyLinkedList implemented, this seems redundant

        ArrayList<MipsBlock> toBeAdded = new ArrayList<>();

        for (MipsBlock block : function.getBlocks()) {
            HashMap<MipsBlock, MipsBlock> intermediateBlkMap = new HashMap<>();

            for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                Instruction inst = node.getValue();
                if (!(inst instanceof Phi phi)) {
                    continue;
                }
                // Now we see a Phi instruction
                for (int i = 0; i < phi.getOperandCount(); i++) {
                    MipsBlock srcBlk = phi.getSource(i);
                    Operand op = phi.getOperand(i);
                    if (srcBlk.getSuccessors().size() == 1) {
                        // 前序块只会来到当前块，直接插入move
                        VReg temp = new VReg();
                        toBeInserted.computeIfAbsent(srcBlk, k -> new LinkedList<>()).addAll(
                            List.of(
                                // TODO: 如果是并行复制，那么这里中间要加一个VReg temp = new VReg();
                                buildMove(temp, op),
                                buildMove(phi.getRes(), temp)
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
                        VReg temp = new VReg();
                        newBlk.insertAfter(buildMove(temp, op),null);
                        newBlk.addInstruction(buildMove(phi.getRes(), temp));
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
            MipsBlock srcBlk = entry.getKey();
            List<Instruction> instructions = entry.getValue();
            for (int i = 0; i < instructions.size(); i += 2) {
                srcBlk.insertBeforeLastInstruction(instructions.get(i));
            }
            for (int i = 1; i < instructions.size(); i += 2) {
                srcBlk.insertBeforeLastInstruction(instructions.get(i));
            }
            /*
            for (Instruction inst : entry.getValue()) {
                // TODO: 如果是并行复制，也要做到先全部赋值到各个temp上，再从temp赋值到phi上
                srcBlk.insertBeforeLastInstruction(inst);
            }*/
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