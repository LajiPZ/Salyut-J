package frontend.llvm.optimization;

import frontend.datatype.VoidType;
import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;
import utils.Pair;

import java.util.*;

public class InlineFunction implements Pass {
    private HashSet<BBlock> newlyAdded = new HashSet<>();

    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            Value.counter.set(f.resumeValCounter());
            HashSet<Function> activeFunctions = new HashSet<>();
            activeFunctions.add(f);
            for (var n : f.getBBlocks()) {
                BBlock block = n.getValue();
                if (newlyAdded.contains(block)) break;
                for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                    Inst inst = node.getValue();
                    if (inst instanceof ICall call) {
                        // getint()也是ICall调用的，所以要处理一下
                        if (module.getFunctions().contains(call.getFunction())) {
                            execute(module, node, block, f, activeFunctions);
                        }
                    }
                }
            }
            f.saveCurrentValCounter(Value.counter.reset());
        }
    }

    private void execute(IrModule module, DoublyLinkedList.Node<Inst> callNode, BBlock callAtBlock, Function callAtFunction, HashSet<Function> activeFunctions) {

        Function calledFunction = ((ICall) callNode.getValue()).getFunction();
        if (activeFunctions.contains(calledFunction)) return;
        activeFunctions.add(calledFunction);

        Stack<Pair<DoublyLinkedList.Node<Inst>, BBlock>> pendingCalls = new Stack<>();
        Stack<Pair<DoublyLinkedList.Node<Inst>, BBlock>> recursiveCalls = new Stack<>();

        List<BBlock> bBlocksToBeInserted = new LinkedList<>();

        // 1. ICall为界，分块
        BBlock lowerBBlock = new BBlock(callAtFunction);
        // iteratorAfter的行为，保证下半部分第一条就是ICall
        Iterator<DoublyLinkedList.Node<Inst>> it = callAtBlock.getInstructions().iteratorAfter(callNode);
        it.next();
        it.remove();
        while (it.hasNext()) {
            DoublyLinkedList.Node<Inst> node = it.next();
            Inst inst = node.getValue();
            it.remove();
            lowerBBlock.addInstruction(inst);
        }
        bBlocksToBeInserted.add(lowerBBlock);

        // 2. 展开函数
        // 展开部分终结块的Phi对应的Value，代替原来的ICall
        IPhi retPhi = new IPhi(callNode.getValue().getType()); // 这个Phi用来替代ICall
        if (!(calledFunction.getType() instanceof VoidType)) new DoublyLinkedList.Node<Inst>(retPhi).insertIntoHead(lowerBBlock.getInstructions());
        // 把Call的引用换成retPhi
        for (var n : callAtFunction.getBBlocks()) {
            BBlock block = n.getValue();
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                inst.replaceOperand(callNode.getValue(), retPhi);
            }
        }
        for (DoublyLinkedList.Node<Inst> node : lowerBBlock.getInstructions()) {
            Inst inst = node.getValue();
            inst.replaceOperand(callNode.getValue(), retPhi);
        }

        HashMap<Value, Value> replacementMap = new HashMap<>(); // 记录函数克隆之后的替换值
        for (int i = 0; i < calledFunction.getParams().size(); i++) {
            replacementMap.put(
                calledFunction.getParams().get(i),
                callNode.getValue().getOperand(i)
            );
        }

        // 克隆块
        for (var node : calledFunction.getBBlocks()) {
            BBlock block = node.getValue();
            BBlock newBlk = new BBlock(callAtFunction);
            replacementMap.put(block, newBlk);
            bBlocksToBeInserted.add(newBlk);
        }

        // 克隆块内指令
        for (var n : calledFunction.getBBlocks()) {
            BBlock block = n.getValue();
            BBlock newBlk = (BBlock) replacementMap.get(block);
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                Inst newInst = inst.clone();
                if (inst instanceof IReturn ret) {
                    if (!ret.getOperands().isEmpty()) retPhi.addSourcePair(
                        newBlk,
                        ret.getOperand(0)
                    );
                    replacementMap.put(ret, retPhi);
                    Inst substitute = new IBranch(lowerBBlock);
                    newBlk.addInstruction(substitute);
                } else {
                    replacementMap.put(inst, newInst);
                    newBlk.addInstruction(newInst);
                }
            }
        }

        // 换克隆结果的操作数，检查ICall
        for (var n : calledFunction.getBBlocks()) {
            BBlock block = n.getValue();
            BBlock newBlk = (BBlock) replacementMap.get(block);
            for (DoublyLinkedList.Node<Inst> node : newBlk.getInstructions()) {
                Inst inst = node.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value replaced = replacementMap.getOrDefault(inst.getOperand(i), inst.getOperand(i));
                    inst.replaceOperand(i, replaced);
                }
                if (inst instanceof ICall call && module.getFunctions().contains(call.getFunction())) {
                    recursiveCalls.push(new Pair<>(node, newBlk));
                }
            }
        }

        // 处理lowerBBlock内的ICall
        for (DoublyLinkedList.Node<Inst> node : lowerBBlock.getInstructions()) {
            Inst inst = node.getValue();
            for (int i = 0; i < inst.getOperands().size(); i++) {
                Value replaced = replacementMap.getOrDefault(inst.getOperand(i), inst.getOperand(i));
                inst.replaceOperand(i, replaced);
            }
            if (inst instanceof ICall call && module.getFunctions().contains(call.getFunction())) {
                pendingCalls.push(new Pair<>(node, lowerBBlock));
            }
        }

        for (BBlock b : lowerBBlock.getSuccessors()) {
            for (var n : b.getInstructions()) {
                Inst inst = n.getValue();
                if (inst instanceof IPhi) inst.replaceOperand(callAtBlock, lowerBBlock);
            }
        }

        // 现在这个函数里的也要换，ICall不一定只在lowerBBlock内
        for (var node : callAtFunction.getBBlocks()) {
            BBlock block = node.getValue();
            for (var inode : block.getInstructions()) {
                Inst inst = inode.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value replaced = replacementMap.getOrDefault(inst.getOperand(i), inst.getOperand(i));
                    inst.replaceOperand(i, replaced);
                }
            }
        }

        // 3. 连接基本块
        // 可以得知，内联函数的入口块，一定是切分下部分块的下一个
        BBlock entry = bBlocksToBeInserted.get(1);
        callAtBlock.addInstruction(
            new IBranch(entry)
        );
        for (BBlock block : bBlocksToBeInserted) {
            callAtFunction.addBBlock(block);
            newlyAdded.add(block);
        }

        while (!pendingCalls.isEmpty()) {
            var pair = pendingCalls.pop();
            DoublyLinkedList.Node<Inst> node = pair.getValue1();
            BBlock blk = pair.getValue2();
            execute(module, node, blk, callAtFunction, new HashSet<>());
        }

        while (!recursiveCalls.isEmpty()) {
            var pair = recursiveCalls.pop();
            DoublyLinkedList.Node<Inst> node = pair.getValue1();
            BBlock blk = pair.getValue2();
            execute(module, node, blk, callAtFunction, new HashSet<>(activeFunctions));
        }

        /*
        for (DoublyLinkedList.Node<BBlock> bnode : callAtFunction.getBBlocks()) {
            BBlock block = bnode.getValue();
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                if (dummyMap.containsKey(inst)) {
                    retPhi.replaceOperand(dummyMap.get(inst), block);
                }
            }
        }
        */
        activeFunctions.remove(calledFunction);
    }
}
