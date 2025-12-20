package frontend.llvm.optimization;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IBranch;
import frontend.llvm.value.instruction.IPhi;
import frontend.llvm.value.instruction.Inst;
import utils.DoublyLinkedList;

import java.util.*;

public class SimplifyControlFlow implements Pass {
   @Override
   public void run(IrModule module) {
       for (Function function : module.getFunctions()) {
            cutConstantCondition(function);
            var predecessorMap = buildPredecessorMap(function);
            dropUnreachableBBlocks(function, predecessorMap);
            mergeBBlock(function, predecessorMap);
       }
   }

   // 把常数条件的条件分支，换成Jump
   private void cutConstantCondition(Function function) {
       for (var n : function.getBBlocks()) {
           BBlock bBlock = n.getValue();
           if (bBlock.getLastInstruction() instanceof IBranch branch) {
               if (branch.isConditinal() && branch.getOperand(0) instanceof IntConstant intConstant) {
                   bBlock.getInstructions().getTail().drop();

                   IBranch newBranch;
                   BBlock notTaken;
                   if (intConstant.getValue() == 0) {
                       newBranch = new IBranch((BBlock) branch.getOperand(2));
                       notTaken = (BBlock) branch.getOperand(1);
                   } else {
                       newBranch = new IBranch((BBlock) branch.getOperand(1));
                       notTaken = (BBlock) branch.getOperand(2);
                   }

                   bBlock.addInstruction(newBranch);

                   for (DoublyLinkedList.Node<Inst> node : notTaken.getInstructions()) {
                       Inst inst = node.getValue();
                       if (inst instanceof IPhi phi) {
                           var sourcePairs = phi.getSourcePairs();
                           for (int i = 0; i < sourcePairs.size(); i++) {
                               if (sourcePairs.get(i).getValue1() == bBlock) {
                                   phi.dropSourcePair(i);
                               }
                           }
                           if (phi.getSourcePairs().isEmpty()) {
                               throw new RuntimeException("Phi source pairs is empty");
                               // node.drop(); // What if it truly happens? :(
                           }
                       }
                   }
               }
           }
       }
   }

   private HashMap<BBlock, HashSet<BBlock>> buildPredecessorMap(Function function) {
       HashMap<BBlock, HashSet<BBlock>> predecessorMap = new HashMap<>();
       Deque<BBlock> queue = new LinkedList<>();
       if (function.getBBlocks().isEmpty()) { return predecessorMap; }
       queue.add(function.getBBlocks().getHead().getValue());
       predecessorMap.put(function.getBBlocks().getHead().getValue(), new HashSet<>());
       while (!queue.isEmpty()) {
           BBlock bBlock = queue.poll();
           for (BBlock successor : bBlock.getSuccessors()) {
               if (!predecessorMap.containsKey(successor)) {
                   queue.add(successor);
               }
               predecessorMap.computeIfAbsent(successor, k -> new HashSet<>()).add(bBlock);
           }
       }
       return predecessorMap;
   }

   private void dropUnreachableBBlocks(Function function, HashMap<BBlock, HashSet<BBlock>> predecessorMap) {
       HashSet<BBlock> droppedBBlocks = new HashSet<>();
       Iterator<DoublyLinkedList.Node<BBlock>> iterator = function.getBBlocks().iterator();
       while (iterator.hasNext()) {
           var node = iterator.next();
           BBlock bBlock = node.getValue();
           if (!predecessorMap.containsKey(bBlock)) {
               droppedBBlocks.add(bBlock);
               iterator.remove();
           }
       }
       for (var n : function.getBBlocks()) {
           BBlock bBlock = n.getValue();
           for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
               Inst inst = node.getValue();
               if (inst instanceof IPhi phi) {
                   var sourcePairs = phi.getSourcePairs();
                   LinkedList<Integer> droppedIndex = new LinkedList<>();
                   for (int i = 0; i < sourcePairs.size(); i++) {
                       if (droppedBBlocks.contains(sourcePairs.get(i).getValue1())) {
                           droppedIndex.add(i);
                       }
                   }
                   for (int i = droppedIndex.size() - 1; i >= 0; i--) {
                       phi.dropSourcePair(droppedIndex.get(i));
                   }
                   if (phi.getSourcePairs().isEmpty()) {
                       throw new RuntimeException("Phi source pairs is empty");
                       // node.drop(); // What if it truly happens? :(
                   }
               }
           }
       }
   }

   private void mergeBBlock(Function function, HashMap<BBlock, HashSet<BBlock>> predecessorMap) {
       boolean changed = true;
       while (changed) {
           changed = false;
           HashMap<Value, Value> replacementMap = new HashMap<>(); // 主要为了处理Phi指令定义的Value
           for (var n : function.getBBlocks()) {
               BBlock bBlock = n.getValue();
               if (predecessorMap.get(bBlock).size() == 1) {
                   BBlock predecessor = predecessorMap.get(bBlock).iterator().next();
                   if (predecessor.getSuccessors().size() == 1) {
                       // 绝对一对一；把当前的bBlock并到前继里
                       changed = true;
                       predecessor.getInstructions().getTail().drop();
                       for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
                           Inst inst = node.getValue();
                           // 此时就不需要Phi赋值了
                           if (inst instanceof IPhi phi) {
                               Value value = phi.getOperand(1);
                               replacementMap.put(phi, value);
                           } else {
                               predecessor.addInstruction(inst);
                           }
                       }
                       n.drop();

                       for (BBlock successor : bBlock.getSuccessors()) {
                           predecessorMap.get(successor).remove(bBlock);
                           predecessorMap.get(successor).add(predecessor);
                           for (DoublyLinkedList.Node<Inst> node : successor.getInstructions()) {
                               Inst inst = node.getValue();
                               if (inst instanceof IPhi phi) {
                                   phi.replaceOperand(bBlock, predecessor);
                               }
                           }
                       }
                   }
               }
               break;
           }
           updateOperand(function, replacementMap);
       }
   }

    private void updateOperand(Function function, HashMap<Value, Value> replacementMap) {
        for (var n : function.getBBlocks()) {
            BBlock bblock = n.getValue();
            for (DoublyLinkedList.Node<Inst> node : bblock.getInstructions()) {
                Inst inst = node.getValue();
                for (int i = 0; i < inst.getOperands().size(); i++) {
                    Value replacement = inst.getOperand(i);
                    while (replacementMap.containsKey(replacement)) {
                        replacement = replacementMap.get(replacement);
                        inst.replaceOperand(i, replacement);
                    }
                }
            }
        }
    }
}

