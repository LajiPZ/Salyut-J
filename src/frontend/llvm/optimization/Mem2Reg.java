package frontend.llvm.optimization;

import frontend.datatype.BaseType;
import frontend.datatype.PointerType;
import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.*;
import utils.DoublyLinkedList;

import java.util.*;

public class Mem2Reg implements Pass {
    // TODO：不做这个的话，我们寄存器分配部分没有任何意义。。。

    private List<Value> allocates = new LinkedList<>();
    private Map<IPhi, Value> newPhis = new HashMap<>();
    private Map<Value, Set<BBlock>> allocateDefines = new HashMap<>();
    private Map<BBlock, Set<BBlock>> dominatorFrontiers = new HashMap<>();

    @Override
    public void run(IrModule module) {
        for (Function func : module.getFunctions()) {
            Value.counter.set(func.resumeValCounter());
            findAllocates(func);
            findDominationFrontiers(func);
            putPhi();
            renameVariables(func);
            func.saveCurrentValCounter(Value.counter.reset());
        }
    }

    private void findAllocates(Function func) {
        // 找到所有的Alloc，以及所有的定义点
        allocates.clear();
        allocateDefines.clear();
        for (BBlock block: func.getBBlocks()) {
            if (!func.getCtrlFlowGraph().containsBBlock(block)) {
                continue;
            }
            for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                Inst inst = node.getValue();
                if (inst instanceof IAllocate allocate) {
                    if (((PointerType) allocate.getType()).getBaseType() instanceof BaseType) {
                        allocates.add(allocate);
                    }
                }
                if (inst instanceof IStore store) {
                    if (allocates.contains(store.getPointer())) {
                        allocateDefines.computeIfAbsent(store.getPointer(), k -> new LinkedHashSet<>()).add(block);
                    }
                }
            }
        }
    }

    private void findDominationFrontiers(Function func) {
        dominatorFrontiers.clear();
        for (BBlock block: func.getBBlocks()) {
            dominatorFrontiers.put(block, new HashSet<>());
        }
        for (BBlock block: func.getBBlocks()) {
            if (func.getCtrlFlowGraph().getPredecessors(block).size() <= 1) {
                // Sequential / Dead Block
                continue;
            }
            for (BBlock predecessor: func.getCtrlFlowGraph().getPredecessors(block)) {
                BBlock prev = predecessor;
                while (prev != func.getDomTree().getImmediateDominator(block)) {
                    dominatorFrontiers.get(prev).add(block);
                    prev = func.getDomTree().getImmediateDominator(prev);
                }
            }
        }
    }

    private void putPhi() {
        // 在各个定义点的支配边界放Phi
        newPhis.clear();
        for (Value allocate : allocates) {
            Set<BBlock> visited = new HashSet<>();
            LinkedList<BBlock> worklist = new LinkedList<>(allocateDefines.getOrDefault(allocate, Collections.emptySet()));
            while (!worklist.isEmpty()) {
                BBlock block = worklist.pop();
                for (BBlock df : dominatorFrontiers.get(block)) {
                    if (!visited.contains(df)) {
                        visited.add(df);
                        IPhi phi = new IPhi(((PointerType) allocate.getType()).getBaseType());
                        new DoublyLinkedList.Node<Inst>(phi).insertBefore(df.getInstructions().getHead());
                        newPhis.put(phi, allocate);
                        // Phi自身也是对alloc对应变量的赋值，故要考虑其支配边界
                        if (!worklist.contains(df)) {
                            worklist.push(df);
                        }
                    }
                }
            }
        }
    }

    private void renameVariables(Function func) {
        Set<BBlock> visited = new HashSet<>();
        Map<Value, Value> replaceMap = new HashMap<>(); // 被替换掉的指令（如Load），被替换为的值

        LinkedList<Map<BBlock, Map<Value, Value>>> worklist = new LinkedList<>(); // BBlock到replaceMap映射的集合，用于给Phi加入来源
        worklist.push(Map.of(func.getBBlocks().get(0), new HashMap<>()));
        while (!worklist.isEmpty()) {
            var entry = worklist.pop();
            BBlock block = entry.keySet().iterator().next();
            Map<Value, Value> incoming = entry.get(block); // 当前块中，alloc对应的最新值
            if (visited.contains(block)) {
                continue;
            }
            visited.add(block);
            Iterator<DoublyLinkedList.Node<Inst>> it = block.getInstructions().iterator();
            while (it.hasNext()) {
                Inst inst = it.next().getValue();
                for (var replaceEntry: replaceMap.entrySet()) {
                    inst.replaceOperand(replaceEntry.getKey(), replaceEntry.getValue());
                }
                if (inst instanceof IAllocate alloc) {
                    if (!allocates.contains(alloc)) {
                        continue;
                    }
                    it.remove();
                }
                if (inst instanceof ILoad load) {
                    if (!allocates.contains(load.getPointer())) {
                        continue;
                    }
                    replaceMap.put(load, incoming.getOrDefault(load.getPointer(), new IntConstant(0, load.getType())));
                    it.remove();
                }
                if (inst instanceof IStore store) {
                    if (!allocates.contains(store.getPointer())) {
                        continue;
                    }
                    incoming.put(store.getPointer(), store.getValue());
                    it.remove();
                }
                if (inst instanceof IPhi phi) {
                    if (!newPhis.containsKey(phi)) {
                        continue;
                    }
                    Value alloc = newPhis.get(phi);
                    incoming.put(alloc, phi);
                }
            }
            for (BBlock successor : func.getCtrlFlowGraph().getSuccessors(block)) {
                worklist.push(Map.of(successor, new HashMap<>(incoming)));
                for (DoublyLinkedList.Node<Inst> node : successor.getInstructions()) {
                    Inst inst = node.getValue();
                    if (inst instanceof IPhi phi) {
                        if (newPhis.containsKey(phi)) {
                            Value alloc = newPhis.get(phi);
                            if (incoming.containsKey(alloc)) {
                                phi.addSourcePair(block, incoming.get(alloc));
                            } else {
                                phi.addSourcePair(block, new IntConstant(0, ((PointerType) alloc.getType()).getBaseType()));
                            }
                        }
                    }
                }
            }
        }
        for (BBlock block : func.getBBlocks()) {
            // visited的已经换好了，没visited的没换完，故有这一步
            if (!visited.contains(block)) {
                for (DoublyLinkedList.Node<Inst> node : block.getInstructions()) {
                    Inst inst = node.getValue();
                    for (var replaceEntry: replaceMap.entrySet()) {
                        inst.replaceOperand(replaceEntry.getKey(), replaceEntry.getValue());
                    }
                }
            }
        }
    }
}

