package frontend.llvm.optimization;

import frontend.datatype.VoidType;
import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.*;

public class InlineFunction implements Pass {

    private HashMap<Value, Value> callReplacementMap = new HashMap<>();

    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            Value.counter.set(f.resumeValCounter());
            List<BBlock> bBlocksToBeInserted = new LinkedList<>();
            for (BBlock block : f.getBBlocks()) {
                for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                    Inst inst = node.getValue();
                    if (inst instanceof ICall call) {
                        // getint()也是ICall调用的，所以要处理一下
                        if (module.getFunctions().contains(call.getFunction())) {
                            HashSet<Function> activeFunctions = new HashSet<>();
                            bBlocksToBeInserted.addAll(execute(module, node, block, f, activeFunctions));
                        }
                    }
                }
            }
            f.saveCurrentValCounter(Value.counter.reset());
            for (BBlock block : bBlocksToBeInserted) f.addBBlock(block);
        }
        // 虽然很蠢，但是这样换ICall的值最简单
        for (Function f : module.getFunctions()) {
            for (BBlock block : f.getBBlocks()) {
                for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                    Inst inst = node.getValue();
                    for (int i = 0; i < inst.getOperands().size(); i++) {
                        Value replaced = callReplacementMap.getOrDefault(inst.getOperand(i), inst.getOperand(i));
                        inst.replaceOperand(i, replaced);
                    }
                }
            }
        }
    }

    private List<BBlock> execute(IrModule module, DoublyLinkedList.Node<Inst> callNode, BBlock callAtBlock, Function callAtFunction, HashSet<Function> activeFunctions) {
        // TODO: activeFunctions有问题

        List<BBlock> bBlocksToBeInserted = new LinkedList<>();

        activeFunctions.add(callAtFunction);
        Function calledFunction = ((ICall) callNode.getValue()).getFunction();
        if (activeFunctions.contains(calledFunction)) return List.of();

        // 1. ICall为界，分块
        BBlock lowerBBlock = new BBlock(callAtFunction);
        // iteratorAfter的行为，保证下半部分第一条就是ICall
        Iterator<DoublyLinkedList.Node<Inst>> it = callAtBlock.getInstructions().iteratorAfter(callNode);
        while (it.hasNext()) {
            DoublyLinkedList.Node<Inst> node = it.next();
            Inst inst = node.getValue();
            it.remove();
            lowerBBlock.addInstruction(inst);
        }
        bBlocksToBeInserted.add(lowerBBlock);

        // 2. 展开函数
        IPhi phi = new IPhi(callNode.getValue().getType());
        BBlock endBlock = new BBlock(callAtFunction);
        if (!(calledFunction.getType() instanceof VoidType)) endBlock.addInstruction(phi);
        endBlock.addInstruction(new IBranch(lowerBBlock));

        // 展开部分终结块的Phi对应的Value，代替原来的ICall
        lowerBBlock.getInstructions().getHead().drop();
        IPhi retPhi = new IPhi(callNode.getValue().getType());
        retPhi.addSourcePair(endBlock, phi);
        new DoublyLinkedList.Node<Inst>(retPhi).insertIntoHead(lowerBBlock.getInstructions());
        callReplacementMap.put(callNode.getValue(), retPhi);

        // 显然，你需要处理lowerBBlock内的ICall
        for (DoublyLinkedList.Node<Inst> node : lowerBBlock.getInstructions()) {
            Inst inst = node.getValue();
            if (inst instanceof ICall call && module.getFunctions().contains(call.getFunction())) {
                List<BBlock> moreBBlocks = execute(module, node, lowerBBlock, callAtFunction, activeFunctions);
                // 如果展开成功，需要更新当前的lowerBBlock，从而继续复制指令；上面保证，moreBBlocks的第一个，一定是被切分块的下半部分
                if (!moreBBlocks.isEmpty()) lowerBBlock = moreBBlocks.get(0);
                // 那么，replacementMap需不需要更新呢？答案是不需要，因为跳转一定是跳转到上半部分，也就是lowerBBlock的旧值，故不必担心...
                bBlocksToBeInserted.addAll(moreBBlocks);
            }
        }

        HashMap<Value, Value> replacementMap = new HashMap<>(); // 记录函数克隆之后的替换值
        for (int i = 0; i < calledFunction.getParams().size(); i++) {
            replacementMap.put(
                calledFunction.getParams().get(i),
                callNode.getValue().getOperand(i)
            );
        }
        for (BBlock block : calledFunction.getBBlocks()) {
            BBlock newBlk = new BBlock(callAtFunction);
            replacementMap.put(block, newBlk);
            bBlocksToBeInserted.add(newBlk);
        }

        for (BBlock block : calledFunction.getBBlocks()) {
            BBlock newBlk = (BBlock) replacementMap.get(block);
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                if (inst instanceof ICall call && module.getFunctions().contains(call.getFunction())) {
                    List<BBlock> moreBBlocks = execute(module, node, newBlk, callAtFunction, activeFunctions);
                    // 如果展开成功，需要更新当前的newBlk，从而继续复制指令；上面保证，moreBBlocks的第一个，一定是被切分块的下半部分
                    if (!moreBBlocks.isEmpty()) newBlk = moreBBlocks.get(0);
                    // 那么，replacementMap需不需要更新呢？答案是不需要，因为跳转一定是跳转到上半部分，也就是newBlk的旧值，故不必担心...
                    bBlocksToBeInserted.addAll(moreBBlocks);
                } else {
                    Inst newInst = inst.clone(replacementMap);
                    replacementMap.put(inst, newInst);
                    if (newInst instanceof IReturn ret) {
                        if (!ret.getOperands().isEmpty()) phi.addSourcePair(newBlk, ret.getOperand(0));
                        newBlk.addInstruction(
                            new IBranch(endBlock)
                        );
                    } else {
                        newBlk.addInstruction(newInst);
                    }
                }
            }
        }

        // 3. 连接基本块
        // 可以得知，内联函数的入口块，一定是切分下部分块的下一个
        BBlock entry = bBlocksToBeInserted.get(1);
        callAtBlock.addInstruction(
            new IBranch(entry)
        );
        bBlocksToBeInserted.add(endBlock);
        return bBlocksToBeInserted;
    }
}
