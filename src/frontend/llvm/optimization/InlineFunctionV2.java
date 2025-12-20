package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.*;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.*;

public class InlineFunctionV2 implements Pass {
    private int inlineCounter = 0;

    @Override
    public void run(IrModule module) {
        boolean changed = true;
        while (changed) {
            changed = false;
            // 每次迭代只内联一层，防止在遍历时修改结构导致迭代器失效
            for (Function f : module.getFunctions()) {
                if (!module.getFunctions().contains(f)) continue;
                if (tryInline(f, module)) {
                    changed = true;
                    break;
                }
            }
        }
    }

    private boolean tryInline(Function caller, IrModule module) {
        for (var n : caller.getBBlocks()) {
            BBlock bb = n.getValue();
            for (DoublyLinkedList.Node<Inst> node : bb.getInstructions()) {
                if (node.getValue() instanceof ICall call) {
                    Function callee = call.getFunction();
                    // 只内联模块内定义的非递归函数，且不内联正在处理的函数
                    if (module.getFunctions().contains(callee) && callee != caller) {
                        inlineAtCallSite(caller, bb, node, call);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void inlineAtCallSite(Function caller, BBlock callBlock, DoublyLinkedList.Node<Inst> callNode, ICall call) {
        Function callee = call.getFunction();

        // 1. 拆分 callBlock 为 preBlock 和 postBlock
        // postBlock 包含 call 之后的指令
        BBlock postBlock = new BBlock(caller);
        Iterator<DoublyLinkedList.Node<Inst>> it = callBlock.getInstructions().iteratorAfter(callNode);
        while (it.hasNext()) {
            DoublyLinkedList.Node<Inst> nextNode = it.next();
            Inst inst = nextNode.getValue();
            it.remove();
            postBlock.addInstruction(inst);
        }

        // 将 call 指令从 callBlock 中移除
        postBlock.getInstructions().getHead().drop();

        // 2. 建立映射：callee 的 Value -> caller 里的新 Value
        Map<Value, Value> vMap = new HashMap<>();

        // 映射参数：实参代替形参
        for (int i = 0; i < callee.getParams().size(); i++) {
            vMap.put(callee.getParams().get(i), call.getOperand(i));
        }

        // 3. 克隆基本块（不含指令）
        List<BBlock> clonedBlocks = new ArrayList<>();
        for (var node : callee.getBBlocks()) {
            BBlock calleeBB = node.getValue();
            BBlock newBB = new BBlock(caller);
            vMap.put(calleeBB, newBB);
            clonedBlocks.add(newBB);
        }

        // 4. 克隆指令并处理控制流
        IPhi retPhi = null;
        if (!(call.getType() instanceof frontend.datatype.VoidType)) {
            retPhi = new IPhi(call.getType());
            new DoublyLinkedList.Node<>((Inst) retPhi).insertIntoHead(postBlock.getInstructions());
        }

        for (var node : callee.getBBlocks()) {
            BBlock calleeBB = node.getValue();
            BBlock newBB = (BBlock) vMap.get(calleeBB);
            for (DoublyLinkedList.Node<Inst> instNode : calleeBB.getInstructions()) {
                Inst origInst = instNode.getValue();

                if (origInst instanceof IReturn ret) {
                    // Return 变成跳转到 postBlock
                    if (retPhi != null && !ret.getOperands().isEmpty()) {
                        retPhi.addSourcePair(newBB, ret.getOperand(0));
                    }
                    newBB.addInstruction(new IBranch(postBlock));
                } else {
                    Inst clonedInst = origInst.clone();
                    vMap.put(origInst, clonedInst);
                    newBB.addInstruction(clonedInst);
                }
            }
        }

        // 5. 修复克隆指令的操作数
        for (BBlock newBB : clonedBlocks) {
            for (DoublyLinkedList.Node<Inst> instNode : newBB.getInstructions()) {
                Inst inst = instNode.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value op = inst.getOperand(i);
                    if (vMap.containsKey(op)) {
                        inst.replaceOperand(i, vMap.get(op));
                    }
                }
            }
        }

        // 6. 修复 retPhi 的操作数（来自于 callee 内部的值需要重映射）
        if (retPhi != null) {
            for (int i = 0; i < retPhi.getOperands().size(); i++) {
                Value op = retPhi.getOperand(i);
                if (vMap.containsKey(op)) {
                    retPhi.replaceOperand(i, vMap.get(op));
                }
            }
            // 替换原 call 的所有引用为 retPhi
            // call.replaceAllUsesWith(retPhi);
            for (DoublyLinkedList.Node<Inst> node : postBlock.getInstructions()) {
                Inst inst = node.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value operand = inst.getOperand(i);
                    if (operand == call) {
                        inst.replaceOperand(i, retPhi);
                    }
                }
            }
            for (BBlock newBB : clonedBlocks) {
                for (DoublyLinkedList.Node<Inst> instNode : newBB.getInstructions()) {
                    Inst inst = instNode.getValue();
                    for (int i = 0; i < inst.getOperands().size(); i++) {
                        Value operand = inst.getOperand(i);
                        if (operand == call) {
                            inst.replaceOperand(i, retPhi);
                        }
                    }
                }
            }
        }

        // 7. 链接入口：callBlock -> callee 的首个克隆块
        BBlock entryOfCallee = clonedBlocks.get(0);
        callBlock.addInstruction(new IBranch(entryOfCallee));

        // 8. 将新块加入函数
        for (BBlock newBB : clonedBlocks) caller.addBBlock(newBB);
        caller.addBBlock(postBlock);
    }
}