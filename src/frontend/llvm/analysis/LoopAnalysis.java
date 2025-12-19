package frontend.llvm.analysis;

import frontend.llvm.IrModule;
import frontend.llvm.Pass;
import frontend.llvm.tools.ControlFlowGraph;
import frontend.llvm.tools.DominatorTree;
import frontend.llvm.tools.LoopInformation;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;

import java.util.*;

public class LoopAnalysis implements Pass {

    private HashMap<BBlock, LoopInformation> loopInfoMap = new HashMap<>();
    private List<LoopInformation> loops = new LinkedList<>();
    private List<LoopInformation> allLoops = new LinkedList<>();

    @Override
    public void run(IrModule module) {
        for (Function f : module.getFunctions()) {
            loopInfoMap.clear();
            loops.clear();
            allLoops.clear();
            execute(f);
        }
    }

    private void execute(Function function) {
        DominatorTree dom = function.getDomTree();
        ControlFlowGraph cfg = function.getCtrlFlowGraph();

        if (function.getBBlocks().isEmpty()) return;

        List<BBlock> postOrder = dom.getPostOrder(function.getBBlocks().get(0));

        for (BBlock head : postOrder) {
            Stack<BBlock> backEdges = new Stack<>();
            for (BBlock block : cfg.getPredecessors(head)) {
                if (dom.isAncestor(head, block)) {
                    backEdges.push(block);
                }
            }
            if (!backEdges.isEmpty()) {
                detectLoop(head, backEdges, cfg);
            }
        }

        for (BBlock block : postOrder) {
            fillLoopInfo(block);
        }

        tidyUpAllLoops();

        function.setLoopMap(new HashMap<>(loopInfoMap));
        function.setLoops(new LinkedList<>(loops));
        function.setAllLoops(new LinkedList<>(allLoops));

        for (LoopInformation loop : allLoops) {
            for (BBlock bBlock : loop.getBlocks()) {
                for (BBlock successor : cfg.getSuccessors(bBlock)) {
                    if (!loop.getBlocks().contains(successor)) {
                        loop.addExitTargetBlock(successor);
                        loop.addExitBlock(bBlock);
                    }
                }
            }
            for (BBlock predecessor : cfg.getPredecessors(loop.getBlocks().get(0))) {
                if (!loop.getBlocks().contains(predecessor)) {
                    loop.addLatchBlock(predecessor);
                }
            }
        }
    }

    private void detectLoop(BBlock head, Stack<BBlock> backEdges, ControlFlowGraph cfg) {
        LoopInformation currentLoop = new LoopInformation(head);
        while (!backEdges.isEmpty()) {
            BBlock backEdge = backEdges.pop();
            LoopInformation subLoop = loopInfoMap.get(backEdge);
            if (subLoop == null) {
                loopInfoMap.put(backEdge, currentLoop);
                if (backEdge == currentLoop.getHead()) {
                    continue;
                }
                for (BBlock predecessor : cfg.getPredecessors(backEdge)) {
                    backEdges.push(predecessor);
                }
            } else {
                while (subLoop.hasParentLoop()) {
                    subLoop = subLoop.getParentLoop();
                }
                if (subLoop == currentLoop) {
                    continue;
                }
                subLoop.setParentLoop(currentLoop);
                for (BBlock predecessor : cfg.getPredecessors(subLoop.getHead())) {
                    if (loopInfoMap.get(predecessor) != subLoop) {
                        backEdges.push(predecessor);
                    }
                }
            }
        }
    }

    private void fillLoopInfo(BBlock block) {
        LoopInformation subLoop = loopInfoMap.get(block);
        if (subLoop != null && subLoop.getHead() == block) {
            if (subLoop.hasParentLoop()) {
                subLoop.getParentLoop().addSubLoop(subLoop);
            } else {
                loops.add(subLoop);
            }
            subLoop.reverseBlocks();
            subLoop.reverseSubLoops();
            subLoop = subLoop.getParentLoop();
        }
        while (subLoop != null) {
            subLoop.addBlock(block);
            subLoop = subLoop.getParentLoop();
        }
    }

    private void tidyUpAllLoops() {
        Stack<LoopInformation> queue = new Stack<>();
        queue.addAll(loops);
        allLoops.addAll(loops);
        while (!queue.isEmpty()) {
            LoopInformation loop = queue.pop();
            if (!loop.getSubLoops().isEmpty()) {
                queue.addAll(loop.getSubLoops());
                allLoops.addAll(loop.getSubLoops());
            }
        }
    }


}
