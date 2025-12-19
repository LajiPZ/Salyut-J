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
            dropUnrachableBBlocks(function, predecessorMap);
            var replacementMap = mergeBBlock(function, predecessorMap);
            updateOperand(function, replacementMap);
       }
       // TODO: controlFlow, dominatorTree都要重建
   }

   private void updateOperand(Function function, HashMap<Value, Value> replacementMap) {
       for (BBlock bblock : function.getBBlocks()) {
           for (DoublyLinkedList.Node<Inst> node : bblock.getInstructions()) {
               Inst inst = node.getValue();
               for (int i = 0; i < inst.getOperands().size(); i++) {
                   Value operand = inst.getOperand(i);
                   if (replacementMap.containsKey(operand)) {
                       inst.replaceOperand(i, replacementMap.get(operand));
                   }
               }
           }
       }
   }


   // 把常数条件的条件分支，换成Jump
   private void cutConstantCondition(Function function) {
       for (BBlock bBlock : function.getBBlocks()) {
           if (bBlock.getLastInstruction() instanceof IBranch branch) {
               if (branch.isConditinal() && branch.getOperand(0) instanceof IntConstant intConstant) {
                   bBlock.getInstructions().getTail().drop();

                   IBranch newBranch;
                   BBlock branchTarget;
                   if (intConstant.getValue() == 0) {
                       newBranch = new IBranch((BBlock) branch.getOperand(2));
                       branchTarget = (BBlock) branch.getOperand(1);
                   } else {
                       newBranch = new IBranch((BBlock) branch.getOperand(1));
                       branchTarget = (BBlock) branch.getOperand(2);
                   }

                   bBlock.addInstruction(newBranch);
                   for (DoublyLinkedList.Node<Inst> node : branchTarget.getInstructions()) {
                       Inst inst = node.getValue();
                       if (inst instanceof IPhi phi) {
                           var sourcePairs = phi.getSourcePairs();
                           for (int i = 0; i < sourcePairs.size(); i++) {
                               if (sourcePairs.get(i).getValue1() == bBlock) {
                                   phi.dropSourcePair(i);
                                   break;
                               }
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
       queue.add(function.getBBlocks().get(0));
       predecessorMap.put(function.getBBlocks().get(0), new HashSet<>());
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

   private void dropUnrachableBBlocks(Function function, HashMap<BBlock, HashSet<BBlock>> predecessorMap) {
       HashSet<BBlock> droppedBBlocks = new HashSet<>();
       Iterator<BBlock> iterator = function.getBBlocks().iterator();
       while (iterator.hasNext()) {
           BBlock bBlock = iterator.next();
           if (!predecessorMap.containsKey(bBlock)) {
               droppedBBlocks.add(bBlock);
               iterator.remove();
           }
       }
       for (BBlock bBlock : function.getBBlocks()) {
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
               }
           }
       }
   }

   private HashMap<Value, Value> mergeBBlock(Function function, HashMap<BBlock, HashSet<BBlock>> predecessorMap) {
       HashMap<Value, Value> replacementMap = new HashMap<>(); // 主要为了处理Phi指令定义的Value
       Iterator<BBlock> iterator = function.getBBlocks().iterator();
       while (iterator.hasNext()) {
           BBlock bBlock = iterator.next();
           // 把当前的bBlock并到predecessor里
           if (predecessorMap.get(bBlock).size() == 1) {
               BBlock predecessor = predecessorMap.get(bBlock).iterator().next();
               if (predecessor.getSuccessors().size() == 1) {
                   predecessor.getInstructions().getTail().drop();
                   for (DoublyLinkedList.Node<Inst> node : bBlock.getInstructions()) {
                       Inst inst = node.getValue();
                       // 此时就不需要Phi赋值了
                       if (inst instanceof IPhi phi) {
                           Value value = phi.getOperand(1);
                           replacementMap.put(phi, replacementMap.getOrDefault(value, value));
                       } else {
                           predecessor.addInstruction(inst);
                       }
                   }
                   iterator.remove();
                   predecessorMap.get(predecessor).remove(bBlock);
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
       }
       return replacementMap;
   }


}

