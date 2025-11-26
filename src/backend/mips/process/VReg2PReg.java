package backend.mips.process;

import backend.mips.MipsBlock;
import backend.mips.MipsFunction;
import backend.mips.instruction.*;
import backend.mips.operand.*;
import backend.mips.utils.BlockLivingData;
import backend.mips.utils.UndirectedGraph;
import backend.mips.utils.spillLoc.CP1SpillLoc;
import backend.mips.utils.spillLoc.MemSpillLoc;
import backend.mips.utils.spillLoc.SpillLoc;

import java.util.*;

// TODO: concurrent modification; 不得不更改Block内Instruction的数据结构
// TODO: 分配逻辑有问题，全局变量的分配有冲突

/**
 * Ref: Ep.13, Building an Optimizing Compiler
 * 考虑到没有浮点指令，选择将CP1.reg，作为溢出的一个选择
 */
public class VReg2PReg {

    static class Config {
        // 本设计中，全局和局部的可用寄存器均一致
        // 溢出所用寄存器也通过局部变量分配完成，不需要预留寄存器处理溢出变量
        static final List<PReg> availPRegs = List.of(
            AReg.t[0], AReg.t[1], AReg.t[2], AReg.t[3],
            AReg.t[4], AReg.t[5], AReg.t[6], AReg.t[7],
            AReg.s[0], AReg.s[1], AReg.s[2], AReg.s[3],
            AReg.s[4], AReg.s[5], AReg.s[6], AReg.s[7],
            AReg.v1, AReg.t[8], AReg.t[9],
            AReg.gp,  // group pointer我们没用到，也可以来
            AReg.k0, AReg.k1
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
        if (MipsFunction.isCaller(function)) {
            this.assignedPRegs.add(AReg.ra);
        }
        refill();
        function.fillStackSize();
    }

    private final HashSet<CP1Reg> availCP1RegForSpill = new HashSet<>(Arrays.asList(CP1Reg.f));
    private HashMap<VReg, SpillLoc> spillLocMap = new HashMap<>();
    private int currentOffset = 0; // 从$sp往上，依次是spilledReg、保存寄存器

    public void allocateSpillLoc(VReg vReg) {
        if (availCP1RegForSpill.isEmpty()) {
            function.enlargeStackSize(4);
            spillLocMap.put(
                vReg,
                new MemSpillLoc(currentOffset)
            );
            currentOffset += 4;
        } else {
            CP1Reg cp1Reg = availCP1RegForSpill.iterator().next();
            availCP1RegForSpill.remove(cp1Reg);
            spillLocMap.put(
                vReg,
                new CP1SpillLoc(cp1Reg)
            );
        }
    }

    public void releaseSpillLoc(SpillLoc spillLoc) {
        if (spillLoc instanceof CP1SpillLoc cp1SpillLoc) {
            availCP1RegForSpill.add(cp1SpillLoc.getReg());
        }
    }

    public SpillLoc getSpillLoc(VReg vReg) {
        return spillLocMap.get(vReg);
    }

    public Instruction getSpillLoad(VReg dest, SpillLoc src) {
        if (src instanceof CP1SpillLoc cp1Loc) {
            return new CP1RegMove(
                CP1RegMove.Op.mfc1, dest, cp1Loc.getReg()
            );
        } else {
            return new Load(
                Mem.Align.w,
                dest,
                AReg.sp,
                new Immediate(((MemSpillLoc) src).getOffset())
            );
        }
    }

    public Instruction getSpillStore(VReg src, SpillLoc dest) {
        if (dest instanceof CP1SpillLoc cp1Loc) {
            return new CP1RegMove(
                CP1RegMove.Op.mtc1, src, cp1Loc.getReg()
            );
        } else {
            return new Store(
                Mem.Align.w,
                src,
                AReg.sp,
                new Immediate(((MemSpillLoc) dest).getOffset())
            );
        }
    }

    private void refill() {
        // 1. refill
        for (MipsBlock block : function.getBlocks()) {
            for (Instruction instruction : block.getInstructions()) {
                instruction.fillPReg(colorMap);
            }
        }

        // 2. store/recover the allocated registers
        // 在第一个调整$fp的指令后，加保存指令
        Instruction target = null;
        for (Instruction inst : function.getEntry().getInstructions()) {
            if (inst instanceof Calc && inst.getDefOperands().contains(AReg.fp)) {
                target = inst;
                break;
            }
        }

        function.enlargeStackSize(4 * assignedPRegs.size());
        for (PReg pReg : assignedPRegs) {
            function.getEntry().insertAfter(
                new Store(Mem.Align.w, pReg, AReg.sp, new Immediate(currentOffset)),
                target
            );
            function.getExit().insertAfter(
                new Load(Mem.Align.w, pReg, AReg.sp, new Immediate(currentOffset)),
                null
            );
            currentOffset += 4;
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
            livingData.getLocal().addAll(local);
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
                    int divisor1 = neighborsLeft.get(o1) == 0 ? 1 : neighborsLeft.get(o1);
                    int divisor2 = neighborsLeft.get(o2) == 0 ? 1 : neighborsLeft.get(o2);
                    return priority1 / divisor1 - priority2 / divisor2;
                }).get();
                bucketMap.get(neighborsLeft.get(t)).remove(t);
            } else {
                // 因为这里是HashSet，没法确定选哪个好，故还是按照priority最小来做
                HashSet<VReg> bucket = bucketMap.get(current);
                t = bucket.stream().min((o1, o2) -> {
                    int priority1 = priorityMap.get(o1);
                    int priority2 = priorityMap.get(o2);
                    int divisor1 = neighborsLeft.get(o1) == 0 ? 1 : neighborsLeft.get(o1);
                    int divisor2 = neighborsLeft.get(o2) == 0 ? 1 : neighborsLeft.get(o2);
                    return priority1 / divisor1 - priority2 / divisor2;
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
                    bucketMap.computeIfAbsent(neighborsLeft.get(reg), k -> new HashSet<>()).add(reg);
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
                allocateSpillLoc(t);
                spilledVRegs.add(t);
            } else {
                PReg color = choosePRegGlobal(t, unavailablePRegs);
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
    private PReg choosePRegGlobal(VReg t, HashSet<PReg> unavailablePRegs) {
        for (VReg vReg : globalConflictGraph.getNeighbors(t)) {
            if (!inGlobalGraph.get(vReg)) {
                for (VReg z : globalConflictGraph.getNeighbors(vReg)) {
                    if (inGlobalGraph.get(z) && !globalConflictGraph.getNeighbors(z).contains(t)) {
                        PReg color = colorMap.get(z);
                        if (!unavailablePRegs.contains(color)) return colorMap.get(z);
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
                HashSet<VReg> vRegs = new HashSet<>();
                // 此处认为，point为指令；毕竟操作数可以相同
                vRegs.addAll(instruction.getDefVRegs());
                vRegs.addAll(instruction.getUseVRegs());
                for (VReg vReg : vRegs) {
                    priorityMap.computeIfAbsent(vReg, k -> 0);
                    priorityMap.put(vReg, priorityMap.get(vReg) + 1);
                }
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


        private PReg choosePRegLocal(VReg t, HashSet<PReg> unavailablePRegs) {
            for (VReg vReg : localConflictGraph.getNeighbors(t)) {
                if (!inLocalGraph.getOrDefault(vReg, false)) {
                    for (VReg z : localConflictGraph.getNeighbors(vReg)) {
                        if (inLocalGraph.getOrDefault(z, false) && !localConflictGraph.getNeighbors(z).contains(t)) {
                            PReg color = colorMap.get(z);
                            if (!unavailablePRegs.contains(color)) return colorMap.get(z);
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
            HashSet<VReg> insertStore = new HashSet<>(spilledVRegs);
            HashSet<VReg> insertLoad = new HashSet<>();
            for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                Instruction instruction = block.getInstructions().get(i);
                for (VReg t : instruction.getDefVRegs()) {
                    if (spilledVRegs.contains(t)) {
                        if (insertStore.contains(t)) {
                            insertStore.remove(t);
                            // insert store T, memory(T) after I
                            block.insertAfter(
                                getSpillStore(t, getSpillLoc(t)),
                                instruction
                            );
                        }
                        insertLoad.remove(t);
                    }
                }
                for (VReg t : instruction.getUseVRegs()) {
                    if (spilledVRegs.contains(t)) {
                        insertLoad.add(t);
                    }
                }
            }
            HashMap<VReg, VReg> newNameMap = new HashMap<>();
            for (Instruction instruction : block.getInstructions()) {
                for (VReg t : instruction.getUseVRegs()) {
                    if (spilledVRegs.contains(t)) {
                        if (!newNameMap.containsKey(t)) {
                            newNameMap.put(t, new VReg());
                        }
                        if (insertLoad.contains(t)) {
                            insertLoad.remove(t);
                            // insert load memory(T) => newName(t) before I
                            block.insertBefore(
                                getSpillLoad(newNameMap.get(t), getSpillLoc(t)),
                                instruction
                            );
                        }
                        instruction.replaceOperand(t, newNameMap.get(t));
                    }
                }
                for (VReg t : instruction.getDefVRegs()) {
                    if (spilledVRegs.contains(t)) {
                        if (!newNameMap.containsKey(t)) {
                            newNameMap.put(t, new VReg());
                        }
                        instruction.replaceOperand(t, newNameMap.get(t));
                    }
                }
            }
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
                    localConflictGraph.addVertex(t);
                }
            }
            timeCount++;
            for (VReg t : live) {
                startTimeR.put(t, timeCount);
                for (VReg u : live) {
                    // TODO: IMHO this is redundant
                    if (u != t) {
                        localConflictGraph.addVertex(u);
                        localConflictGraph.addVertex(t);
                        localConflictGraph.addEdge(u, t);
                    }
                }
            }
        }

        private void buildLocalBuckets() {
            int maxNeighbors = localConflictGraph.getVertices().stream().map(localConflictGraph::getEdgeCount).max(Integer::compare).orElse(0);
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
                if (!localBucketMap.getOrDefault(i, new HashSet<>()).isEmpty()) {
                    VReg t = localBucketMap.get(i).iterator().next();
                    localBucketMap.get(i).remove(t);
                    stack.push(t);
                    inLocalGraph.put(t, false); // 原文是下面的u = false，似乎有问题，此处已修正
                    for (VReg u : localConflictGraph.getNeighbors(t)) {
                        if (inLocalGraph.getOrDefault(u,false)) {
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
                if (startTimeI.get(instruction) > finishTime) { // 考虑finishTimeI就是finishTime
                    if (live.size() - liveThrough.size() >= numberRegisters) { // 类似着色，只对局部变量数超出可用寄存器的情况进行处理
                        Iterator<VReg> it = new HashSet<>(live).iterator();
                        while (it.hasNext()) {
                            VReg vReg = it.next();
                            if (colorMap.containsKey(vReg) || !inLocalGraph.getOrDefault(vReg, false)) {
                                // 较原文的修正
                                // 分配过了（e.g. 全局变量，未被分配的一定是局部变量），或者已经从图里取出到栈内，就不应分配
                                continue;
                            }
                            if (endTimeR.get(vReg) < finishTime) { // later than; startTime/endTime不重合，不用考虑等号
                                continue;
                            }
                            if (startTimeR.get(vReg) > beginTime) { // precedes
                                continue;
                            }
                            colorMap.put(vReg, colorMap.get(t));
                            // 此时，t已经被分配了PReg,vReg本来也没有被移出图，所以不用做任何事情
                            finishTime = startTimeR.get(vReg);
                            break;
                        }
                    }
                }
            }
            numberRegisters--;
        }

        private HashMap<Instruction, SpillLoc> releaseSpillLocMap = new HashMap<>();

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
            HashSet<VReg> live = new HashSet<>(liveEnd); // 以liveEnd初始化，旨在屏蔽掉那些已完成分配的全局变量
            for (int i = block.getInstructions().size() - 1; i >= 0; i--) {
                Instruction instruction = block.getInstructions().get(i);
                if (releaseSpillLocMap.containsKey(instruction)) {
                    SpillLoc spillLoc = releaseSpillLocMap.get(instruction);
                    releaseSpillLoc(spillLoc);
                }
                for (VReg t : instruction.getDefVRegs()) {
                    live.remove(t);
                    if (!instruction.getUseVRegs().contains(t) && !globalRegisters.contains(colorMap.get(t))) {
                        // 第二个条件，是为了忽略通过liveStart共用分配好的结果
                        freeRegisters.add(colorMap.get(t));
                    }
                }
                for (VReg t : instruction.getUseVRegs()) {
                    if (!live.contains(t)) {
                        live.add(t);
                        // 修正了原文逻辑，这里只对没入栈的分配
                        // 再次，没被分配寄存器的一定是局部变量，不用担心全局变量混进来
                        if (!colorMap.containsKey(t) && inLocalGraph.getOrDefault(t, false)) {
                            if (freeRegisters.isEmpty()) {
                                localSpillRegister(live, block, instruction, freeRegisters);
                            }
                            PReg s = freeRegisters.iterator().next();
                            freeRegisters.remove(s);
                            colorMap.put(t, s);
                            assignedPRegs.add(s);
                            // inLocalGraph.put(t, true);
                        }
                    }
                }
            }
        }

        private void giveStackedVRegsColor() {
            // 在Stacked内，一定能找到可用的PReg
            while (!stack.isEmpty()) {
                VReg t = stack.pop();
                HashSet<PReg> unavailable = new HashSet<>();
                for (VReg u : localConflictGraph.getNeighbors(t)) {
                    unavailable.add(colorMap.get(u));
                }
                PReg color = choosePRegLocal(t, unavailable);
                colorMap.put(t, color);
                assignedPRegs.add(color);
                inLocalGraph.put(t, true);
            }
        }

        private void localSpillRegister(Set<VReg> live, MipsBlock block, Instruction instruction, Set<PReg> freeRegisters) {
            // live中的此时都已经被分配PReg
            int earliest = 0;
            Instruction lastUseDef = null;
            VReg target = null;
            for (VReg vReg : live) {
                int prev = endTimeI.get(block.getInstructions().get(0));
                Instruction prevInst = null;
                loop: for (Instruction instr : block.getInstructions()) {
                    if (instr.equals(instruction)) {
                        break;
                    }
                    for (VReg u : instr.getDefVRegs()) {
                        if (u.equals(vReg)) {
                            prev = endTimeI.get(instr);
                            prevInst = instr;
                            continue loop;
                        }
                    }
                    for (VReg u : instr.getUseVRegs()) {
                        if (u.equals(vReg)) {
                            prev = endTimeI.get(instr);
                            prevInst = instr;
                            continue loop;
                        }
                    }
                }
                if (prev > earliest) {
                    earliest = prev;
                    lastUseDef = prevInst;
                    target = vReg;
                }
            }

            allocateSpillLoc(target);
            SpillLoc spillLoc = getSpillLoc(target);
            // insert load MEMORY(target), target after I
            block.insertAfter(
                getSpillLoad(target, spillLoc),
                instruction
            );
            VReg newName = new VReg();
            inLocalGraph.put(newName, true);
            // insert store newName, memory(target) after prevUse
            block.insertAfter(
                getSpillStore(newName, spillLoc),
                lastUseDef
            );

            // 考虑到我们是倒着做onePassAllocate的，遍历到lastUseDef时，溢出位置刚好释放
            releaseSpillLocMap.put(lastUseDef, spillLoc);

            int loc = lastUseDef == null ? -1 : block.getInstructions().indexOf(lastUseDef);
            for (; loc >= 0; loc--) {
                Instruction instr = block.getInstructions().get(loc);
                instr.replaceOperand(target, newName);
            }
            // 原文似乎把insert笔误成了delete...
            freeRegisters.add(colorMap.get(target));
        }
    }
}
