package backend.mips.process;

import backend.mips.MipsBlock;
import backend.mips.MipsFunction;
import backend.mips.instruction.Instruction;
import backend.mips.operand.PReg;
import backend.mips.operand.VReg;
import backend.mips.utils.BlockLivingData;
import backend.mips.utils.UndirectedGraph;

import java.util.*;

/**
 * Ref: Ep.13, Building an Optimizing Compiler
 * 考虑到没有浮点指令，选择将CP1.reg，作为溢出的一个选择
 */
public class VReg2PReg {

    static class Config {
        static final List<PReg> availPRegs = List.of(

        );
    }

    private final MipsFunction function;
    private final HashMap<MipsBlock, BlockLivingData<VReg>> livingDataMap = new HashMap<>();

    private final HashSet<PReg> assignedPRegs = new HashSet<>();
    private final HashSet<VReg> spilledVRegs = new HashSet<>();
    private final HashMap<VReg, PReg> colorMap = new HashMap<>();

    public VReg2PReg(MipsFunction function) {
        this.function = function;
        for (MipsBlock block : function.getBlocks()) {
            livingDataMap.put(block, new BlockLivingData<>());
        }
    }

    public void run() {
        buildLivingData();
        classifyLocalVRegs();
        // Driver for register allocation
        globalAllocation();
        for (MipsBlock block : function.getBlocks()) {
            new LocalVReg2PReg(block).localAllocation();
        }
    }

    /**
     * 直接构建in/out集。def/use信息通过倒序遍历块内指令确定。
     */
    private void buildLivingData() {
        boolean changed = true;
        while (changed) {
            changed = false;
            for (MipsBlock block : function.getBlocks()) {
                BlockLivingData<VReg> livingData = livingDataMap.get(block);
                Set<VReg> out = new HashSet<>();
                for (MipsBlock successor : block.getSuccessors()) {
                    BlockLivingData.union(out, livingDataMap.get(successor).getIn());
                }
                Set<VReg> in = new HashSet<>();
                for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                    Instruction instruction = block.getInstructions().get(i);
                    BlockLivingData.minus(in, instruction.getDefVRegs());
                    BlockLivingData.union(in, instruction.getUseVRegs());
                }
                if (!in.equals(livingData.getIn()) || !out.equals(livingData.getOut())) {
                    changed = true;
                    livingData.getIn().clear();
                    livingData.getIn().addAll(in);
                    livingData.getOut().clear();
                    livingData.getOut().addAll(out);
                }
            }
        }
    }

    /**
     * 通过in/out集以及块内引用VReg信息，确定出块内的局部寄存器。
     * 可以得知，出现在块内，且不在in/out集内的，即为局部寄存器。
     */
    private void classifyLocalVRegs() {
        for (MipsBlock block : function.getBlocks()) {
            BlockLivingData<VReg> livingData = livingDataMap.get(block);
            Set<VReg> in = livingData.getIn();
            Set<VReg> out = livingData.getOut();
            Set<VReg> local = new HashSet<>();
            for (Instruction instruction : block.getInstructions()) {
                BlockLivingData.union(local, instruction.getDefVRegs());
                BlockLivingData.union(local, instruction.getUseVRegs());
            }
            BlockLivingData.minus(local, in);
            BlockLivingData.minus(local, out);
            // local集初始化为空，直接加即可
            livingData.getLocal().addAll(in);
        }
    }

    // Below is the implementation of the algorithm described in Ref.

    private UndirectedGraph<VReg> globalConflictGraph;
    private HashMap<VReg, Boolean> inGlobalGraph;

    /**
     * 为全局寄存器构建冲突图。
     */
    private void buildGlobalConflictGraph() {
        UndirectedGraph<VReg> graph = new UndirectedGraph<>();
        for (MipsBlock block : function.getBlocks()) {
            BlockLivingData<VReg> livingData = livingDataMap.get(block);
            // 先建立点
            for (VReg reg : livingData.getOut()) {
                graph.addVertex(reg);
            }
            for (VReg reg : livingData.getIn()) {
                graph.addVertex(reg);
            }
            // 再建立边
            Set<VReg> local = livingData.getLocal();
            HashSet<VReg> liveAfter = new HashSet<>(livingData.getOut());
            for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                // def（如果是全局reg）跟当前除自己以外，所有liveAfter的VReg建立边
                Instruction instruction = block.getInstructions().get(i);
                for (VReg def : instruction.getDefVRegs()) {
                    if (!local.contains(def)) {
                        for (VReg v : liveAfter) {
                            if (!v.equals(def)) graph.addEdge(def, v);
                        }
                    }
                }
                // 更新liveAfter；(liveAfter - def) + use；忽略局部变量
                for (VReg def : instruction.getDefVRegs()) {
                    if (!local.contains(def)) liveAfter.remove(def);
                }
                for (VReg use : instruction.getUseVRegs()) {
                    if (!local.contains(use)) liveAfter.add(use);
                }
            }
        }
        globalConflictGraph = graph;
    }

    /**
     * 图着色分配全局寄存器。
     */
    private void globalAllocation() {
        buildGlobalConflictGraph();
        HashMap<Integer, HashSet<VReg>> bucketMap = new HashMap<>();
        inGlobalGraph = new HashMap<>();
        for (VReg vReg : globalConflictGraph.getVertices()) {
            int conflict = globalConflictGraph.getEdgeCount(vReg);
            bucketMap.computeIfAbsent(conflict, k -> new HashSet<>()).add(vReg);
            inGlobalGraph.put(vReg, true);
        }
        Stack<VReg> allocStack = buildGlobalAllocStack(bucketMap);
        assignGlobalPRegs(allocStack);
    }

    /**
     * 构建全局变量着色顺序的栈。
     */
    private Stack<VReg> buildGlobalAllocStack(HashMap<Integer, HashSet<VReg>> bucketMap) {
        // TODO: bucket可能有问题
        HashMap<VReg, Integer> priorityMap = computePriority();
        HashSet<VReg> nodes = new HashSet<>(globalConflictGraph.getVertices());
        HashMap<VReg, Integer> neighborsLeft = new HashMap<>();
        for (VReg vReg : nodes) {
            neighborsLeft.put(vReg, globalConflictGraph.getEdgeCount(vReg));
        }

        Stack<VReg> stack = new Stack<>();
        VReg t;
        while (!nodes.isEmpty()) {
            List<Integer> indexSet = bucketMap.keySet().stream().sorted().toList(); // 如果要倒序，改sorted即可
            int current = -1; // 分配过程中，一定能找到一个非空的bucket
            for (Integer index : indexSet) {
                if (!bucketMap.get(index).isEmpty()) {
                    current = index;
                    break;
                }
            }
            if (current > Config.availPRegs.size()) {
                // 对所有Nodes找...
                t = nodes.stream().min((o1, o2) -> {
                    int priority1 = priorityMap.get(o1);
                    int priority2 = priorityMap.get(o2);
                    return priority1 / neighborsLeft.get(o1) - priority2 / neighborsLeft.get(o2);
                }).get();
                bucketMap.get(neighborsLeft.get(t)).remove(t);
            } else {
                // 因为这里是HashSet，没法确定选哪个好，故还是按照priority最小来做
                HashSet<VReg> bucket = bucketMap.get(current);
                t = bucket.stream().min((o1, o2) -> {
                    int priority1 = priorityMap.get(o1);
                    int priority2 = priorityMap.get(o2);
                    return priority1 / neighborsLeft.get(o1) - priority2 / neighborsLeft.get(o2);
                }).get();
                bucket.remove(t);
            }
            nodes.remove(t);
            stack.push(t);
            inGlobalGraph.put(t, false);
            for (VReg reg : globalConflictGraph.getNeighbors(t)) {
                if (inGlobalGraph.get(reg)) {
                    bucketMap.get(neighborsLeft.get(reg)).remove(reg);
                    neighborsLeft.compute(reg, (k,v) -> v == null ? null : v - 1); // just to eliminate warnings...
                    bucketMap.get(neighborsLeft.get(reg)).add(reg);
                }
            }
        }
        return stack;
    }

    private void assignGlobalPRegs(Stack<VReg> stack) {
        while (!stack.isEmpty()) {
            VReg t = stack.pop();
            HashSet<PReg> unavailablePRegs = new HashSet<>();
            for (VReg vReg : globalConflictGraph.getNeighbors(t)) {
                if (inGlobalGraph.get(vReg)) {
                    unavailablePRegs.add(colorMap.get(vReg));
                }
            }
            if (unavailablePRegs.size() == Config.availPRegs.size()) {
                // 地址在实际替换为pReg时做
                spilledVRegs.add(t);
            } else {
                PReg color = choosePReg(t, unavailablePRegs);
                colorMap.put(t, color);
                assignedPRegs.add(color);
                inGlobalGraph.put(t, true);
            }
        }
    }

    /**
     * 选取合适的PReg分配。
     * 相较Ref，去除了caller/ee save（认为这种情况没有可用的pReg）
     * 此外，原实现没有处理heuristic全部失效的情况，此处补上。
     */
    private PReg choosePReg(VReg t, HashSet<PReg> unavailablePRegs) {
        for (VReg vReg : globalConflictGraph.getNeighbors(t)) {
            if (!inGlobalGraph.get(vReg)) {
                for (VReg z : globalConflictGraph.getNeighbors(vReg)) {
                    if (inGlobalGraph.get(z) && !globalConflictGraph.getNeighbors(z).contains(t)) {
                        return colorMap.get(z);
                    }
                }
            }
        }
        for (PReg pReg : assignedPRegs) {
            if (!unavailablePRegs.contains(pReg)) {
                return pReg;
            }
        }
        for (PReg pReg : Config.availPRegs) {
            if (!unavailablePRegs.contains(pReg)) {
                return pReg;
            }
        }
        throw new RuntimeException("No available pReg found!!");
    }

    private HashMap<VReg, Integer> computePriority() {
        HashMap<VReg, Integer> priorityMap = new HashMap<>();
        for (MipsBlock block : function.getBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                // TODO
            }
        }
        return priorityMap;
    }

    private class LocalVReg2PReg {
        private MipsBlock block;

        private UndirectedGraph<VReg> localConflictGraph; // 说是localGraph，其实是把全局变量、局部变量一起考虑的冲突图
        private HashMap<VReg, Boolean> inLocalGraph = new HashMap<>();
        private HashMap<VReg, Integer> localNeighborsLeft = new HashMap<>();

        private HashSet<VReg> liveTransparent;
        private HashSet<VReg> liveThrough;
        private HashSet<VReg> liveStart;
        private HashSet<VReg> liveEnd;
        private HashSet<VReg> localRegisters;

        private HashMap<Integer, HashSet<VReg>> localBucketMap;

        private int numberRegisters;
        private int maxPressure = -1;

        private HashMap<Instruction, Integer> startTimeI = new HashMap<>();
        private HashMap<Instruction, Integer> endTimeI = new HashMap<>();
        private HashMap<VReg, Integer> startTimeR = new HashMap<>();
        private HashMap<VReg, Integer> endTimeR = new HashMap<>();

        private Stack<VReg> stack;

        public LocalVReg2PReg(MipsBlock block) {
            this.block = block;
        }

        /**
         * 处理块内的局部寄存器。
         */
        private void localAllocation() {
            if (!spilledVRegs.isEmpty()) {
                insertGlobalSpill();
            }
            localClassify();
            buildLocalConflictGraph();
            buildLocalBuckets();
            stack = new Stack<>();
            numberRegisters = Config.availPRegs.size(); // 原文有笔误，写成了maxPressure
            //int i = 0;
            while (!liveStart.isEmpty()) {
                addToLocalStack();
                VReg t = liveStart.iterator().next();
                liveStart.remove(t);
                allocateWithGlobal(t);
            }
            addToLocalStack();
            boolean hasUnstacked = false;
            for (Boolean bool : inLocalGraph.values()) {
                if (bool) {
                    hasUnstacked = true;
                    break;
                }
            }
            if (hasUnstacked) onePassAllocate();
            giveStackedVRegsColor();

        }

        private void insertGlobalSpill() {

        }

        private void localClassify() {
            liveTransparent = new HashSet<>(livingDataMap.get(block).getOut());
            liveThrough = new HashSet<>(livingDataMap.get(block).getOut());
            liveStart = new HashSet<>();
            localRegisters = new HashSet<>();
            HashSet<VReg> live = new HashSet<>(livingDataMap.get(block).getOut());
            maxPressure = live.size();
            for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                Instruction instruction = block.getInstructions().get(i);
                for (VReg vReg : instruction.getDefVRegs()) {
                    live.remove(vReg);
                    liveTransparent.remove(vReg);
                    liveStart.remove(vReg);
                    if (!instruction.getUseVRegs().contains(vReg)) {
                        liveThrough.remove(vReg);
                    }
                    if (!colorMap.containsKey(vReg)) {
                        localRegisters.add(vReg);
                    }
                }
                for (VReg vReg : instruction.getUseVRegs()) {
                    live.add(vReg);
                    liveTransparent.remove(vReg);
                    liveStart.add(vReg);
                    if (!colorMap.containsKey(vReg)) {
                        localRegisters.add(vReg);
                    }
                }
                int pressure = live.size();
                if (pressure > maxPressure) {
                    maxPressure = pressure;
                }
            }
            liveEnd = new HashSet<>(livingDataMap.get(block).getOut());
            liveEnd.removeAll(liveThrough);
        }

        private void buildLocalConflictGraph() {
            localConflictGraph = new UndirectedGraph<>();
            HashSet<VReg> live = new HashSet<>(livingDataMap.get(block).getOut());
            live.removeAll(spilledVRegs);
            int timeCount = 0;
            for (VReg t : live) {
                endTimeR.put(t, timeCount);
            }
            for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                Instruction instruction = block.getInstructions().get(i);
                timeCount++;
                endTimeI.put(instruction, timeCount);
                for (VReg t : instruction.getDefVRegs()) {
                    startTimeR.put(t, timeCount);
                    live.remove(t);
                    for (VReg u : live) {
                        localConflictGraph.addVertex(u);
                        localConflictGraph.addVertex(t);
                        localConflictGraph.addEdge(u, t);
                    }
                }
                timeCount++;
                startTimeI.put(instruction, timeCount);
                for (VReg t : instruction.getUseVRegs()) {
                    endTimeR.putIfAbsent(t, timeCount); // 注：原文逻辑有问题，此处只能赋值一次，后续的应该全部忽略
                    live.add(t);
                }
            }
            timeCount++;
            for (VReg t : live) {
                startTimeR.put(t, timeCount);
                for (VReg u : live) {
                    if (u != t) {
                        localConflictGraph.addVertex(u);
                        localConflictGraph.addVertex(t);
                        localConflictGraph.addEdge(u, t);
                    }
                }
            }
        }

        private void buildLocalBuckets() {
            int maxNeighbors = localConflictGraph.getVertices().stream().map(localConflictGraph::getEdgeCount).max(Integer::compare).get();
            localBucketMap = new HashMap<>();
            for (int i = 0 ; i <= maxNeighbors ; i++) {
                localBucketMap.put(i, new HashSet<>());
            }
            for (VReg vReg : localRegisters) {
                localBucketMap.get(localConflictGraph.getEdgeCount(vReg)).add(vReg);
                localNeighborsLeft.put(vReg, localConflictGraph.getEdgeCount(vReg));
                inLocalGraph.put(vReg, true);
            }
        }

        private void addToLocalStack() {
            // 类似全局的压栈
            // 但是，对于超过numberRegisters的，不压入栈；
            // 这样的效果就是，一定能被分配的被压入栈，不能被分配的留在图中
            int i = 0;
            while (i < numberRegisters) {
                if (!localBucketMap.get(i).isEmpty()) {
                    VReg t = localBucketMap.get(i).iterator().next();
                    stack.push(t);
                    inLocalGraph.put(t, false); // 原文是下面的u = false，似乎有问题，此处已修正
                    for (VReg u : localConflictGraph.getNeighbors(t)) {
                        if (inLocalGraph.get(u)) {
                            localBucketMap.get(localNeighborsLeft.get(u)).remove(u);
                            localNeighborsLeft.compute(u, (k,v) -> v == null ? null : v - 1); // just to eliminate warnings...
                            localBucketMap.get(localNeighborsLeft.get(u)).add(u);
                            if (i > localNeighborsLeft.get(u)) i = localNeighborsLeft.get(u);
                        }
                    }
                } else {
                    i = i + 1;
                }
            }
        }

        private void allocateWithGlobal(VReg t) {
            // 注意时间是倒着的
            int beginTime = endTimeR.get(t);
            int finishTime = 0; // that is, end of the blk
            for (VReg vReg : liveEnd) {
                if (colorMap.get(vReg).equals(colorMap.get(t))) {
                    // 原文仍然有问题，此处应该选取最早的那一个定义
                    if (startTimeR.get(vReg) > finishTime) {
                        finishTime = startTimeR.get(vReg);
                    }
                }
            }
            HashSet<VReg> live = new HashSet<>(livingDataMap.get(block).getOut());
            for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                Instruction instruction = block.getInstructions().get(i);
                live.removeAll(instruction.getDefVRegs());
                live.addAll(instruction.getUseVRegs());
                if (endTimeI.get(instruction) > finishTime) { // TODO: precedes

                }
            }
        }

        private void onePassAllocate() {
            // 对于那些仍然留在图中的VReg
            HashSet<PReg> globalRegisters = new HashSet<>();
            for (VReg t : liveStart) {
                globalRegisters.add(colorMap.get(t));
            }
            HashSet<PReg> freeRegisters = new HashSet<>(Config.availPRegs);
            for (VReg t : liveEnd) {
                freeRegisters.remove(colorMap.get(t));
                inLocalGraph.put(t, true);
            }
            freeRegisters.removeAll(globalRegisters);
            HashSet<VReg> live = new HashSet<>(liveEnd);
            // TODO...
        }

        private void giveStackedVRegsColor() {
            // 在Stacked内，一定能找到可用的PReg
            while (!stack.isEmpty()) {
                VReg t = stack.pop();
                HashSet<PReg> unavailable = new HashSet<>();
                for (VReg u : localConflictGraph.getNeighbors(t)) {
                    unavailable.add(colorMap.get(u));
                }
                PReg color = choosePReg(t, unavailable);
                colorMap.put(t, color);
                assignedPRegs.add(color);
                inLocalGraph.put(t, true);
            }
        }

        private void localSpillRegister() {

        }
    }



}
