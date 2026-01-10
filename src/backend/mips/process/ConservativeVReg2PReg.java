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
import settings.Settings;
import utils.DoublyLinkedList;

import java.util.*;


// 保守实现，处理局部变量仍然没法分配的情况，此时我们选择牺牲$k0, $k1，给局部变量做一下溢出

public class ConservativeVReg2PReg {

    static class Config {
        // 本设计中，全局和局部的可用寄存器均一致
        // 溢出所用寄存器也通过局部变量分配完成，不需要预留寄存器处理溢出变量
        // 代价是，调用的保存/恢复开销很大，因为无法预知局部变量是哪些，就算局部变量也要保存
        static final List<PReg> availPRegs = List.of(
            AReg.t[0], AReg.t[1], AReg.t[2], AReg.t[3],
            AReg.t[4], AReg.t[5], AReg.t[6], AReg.t[7],
            AReg.s[0], AReg.s[1], AReg.s[2], AReg.s[3],
            AReg.s[4], AReg.s[5], AReg.s[6], AReg.s[7],
            AReg.v1, AReg.t[8], AReg.t[9],
            AReg.gp  // group pointer我们没用到，也可以来*/
        );

        static final List<PReg> localSpillRegs = List.of(
            AReg.k0, AReg.k1
        );
    }

    private final MipsFunction function;
    private final HashMap<MipsBlock, BlockLivingData<VReg>> livingDataMap = new HashMap<>();

    private final HashSet<PReg> assignedPRegs = new HashSet<>();
    private final HashSet<VReg> spilledVRegs = new HashSet<>();
    private final HashMap<VReg, PReg> colorMap = new HashMap<>();

    private final HashSet<PReg> protectedPRegs = new HashSet<>();

    public ConservativeVReg2PReg(MipsFunction function) {
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

    // 如果只是基于栈来处理溢出，简单加就可以
    // 这里基于CP1溢出的话，其管理就必须对于编译单元全体而言，因而是static
    private final static HashSet<CP1Reg> availCP1RegForSpill = (Settings.OptimizeConfig.allowSpillToCP1) ? CP1Reg.availableCP1Regs : new HashSet<>();
    private final HashSet<CP1Reg> availCP1RegForSpillLocal = new HashSet<>();
    private HashMap<VReg, SpillLoc> spillLocMap = new HashMap<>();
    private int currentOffset = 0; // 从$sp往上，依次是spilledReg、保存寄存器

    public void allocateSpillLoc(VReg vReg) {
        if (!availCP1RegForSpillLocal.isEmpty()) {
            CP1Reg cp1Reg = availCP1RegForSpillLocal.iterator().next();
            availCP1RegForSpillLocal.remove(cp1Reg);
            spillLocMap.put(
                vReg,
                new CP1SpillLoc(cp1Reg)
            );
        } else if (!availCP1RegForSpill.isEmpty()) {
            CP1Reg cp1Reg = availCP1RegForSpill.iterator().next();
            availCP1RegForSpill.remove(cp1Reg);
            spillLocMap.put(
                vReg,
                new CP1SpillLoc(cp1Reg)
            );
        } else {
            function.enlargeStackSize(4);
            spillLocMap.put(
                vReg,
                new MemSpillLoc(currentOffset)
            );
            currentOffset += 4;
        }
    }

    public void releaseSpillLoc(SpillLoc spillLoc) {
        // 需要注意的是，此处的回收只对于局部溢出，且不会回收到全局CP1列表；因为分给函数了就不可能回收
        if (spillLoc instanceof CP1SpillLoc cp1SpillLoc) {
            availCP1RegForSpillLocal.add(cp1SpillLoc.getReg());
        }
    }

    public SpillLoc getSpillLoc(VReg vReg) {
        return spillLocMap.get(vReg);
    }

    public Instruction getSpillLoad(Operand dest, SpillLoc src) {
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

    public Instruction getSpillStore(Operand src, SpillLoc dest) {
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
        int spillPRegIndex = 0;
        for (MipsBlock block : function.getBlocks()) {
            for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                // 读取useVRegs，如果有SpillLoc，则在前面load进k1/k2
                // 读取defVRegs，如果有SpillLoc，则在后面加Store进SpillLoc
                Instruction instruction = node.getValue();
                instruction.fillPReg(colorMap);
                for (VReg use : instruction.getUseVRegs()) {
                    PReg replacement = Config.localSpillRegs.get(spillPRegIndex ^= 1);
                    new DoublyLinkedList.Node<>(
                        getSpillLoad(
                            replacement,
                            getSpillLoc(use)
                        )
                    ).insertBefore(node);
                    instruction.replaceOperand(use, replacement);
                }
                for (VReg def : instruction.getDefVRegs()) {
                    PReg replacement = Config.localSpillRegs.get(spillPRegIndex ^= 1);
                    new DoublyLinkedList.Node<>(
                        getSpillStore(
                            replacement,
                            getSpillLoc(def)
                        )
                    ).insertAfter(node);
                    instruction.replaceOperand(def, replacement);
                }
            }
        }

        // 2. store/recover the allocated global registers
        // but why would u do it for main()?
        // 在第一个调整$fp的指令后，加保存指令
        if (!function.toString().equals("main")) {
            DoublyLinkedList.Node<Instruction> target = null;
            for (DoublyLinkedList.Node<Instruction> node : function.getEntry().getInstructions()) {
                Instruction inst = node.getValue();
                if (inst instanceof Calc && inst.getDefOperands().contains(AReg.fp)) {
                    target = node;
                    break;
                }
            }

            HashSet<PReg> stackSavedPRegs = new HashSet<>();
            // 事实上这样是会出问题的，如果栈上多次调用了同一个函数呢？
            HashMap<PReg, CP1Reg> CP1SaveMap = new HashMap<>();
            for (PReg pReg : assignedPRegs) {
                if (Settings.OptimizeConfig.allowCallSaveToCP1) {
                    if (!CP1Reg.availableCP1Regs.isEmpty() && pReg != AReg.ra) {
                        CP1Reg cp1Reg = CP1Reg.availableCP1Regs.iterator().next();
                        CP1Reg.availableCP1Regs.remove(cp1Reg);
                        CP1SaveMap.put(pReg, cp1Reg);
                    } else {
                        stackSavedPRegs.add(pReg);
                    }
                } else {
                    stackSavedPRegs.add(pReg);
                }
            }

            for (var entry : CP1SaveMap.entrySet()) {
                function.getEntry().insertAfter(
                    new CP1RegMove(CP1RegMove.Op.mtc1, entry.getKey(), entry.getValue()),
                    target
                );
                function.getExit().insertAfter(
                    new CP1RegMove(CP1RegMove.Op.mfc1, entry.getKey(), entry.getValue()),
                    null
                );
            }

            function.enlargeStackSize(4 * stackSavedPRegs.size());
            for (PReg pReg : stackSavedPRegs) {
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
                Set<VReg> in = new HashSet<>(out);
                Iterator<DoublyLinkedList.Node<Instruction>> backwardIt = block.getInstructions().backwardIterator();
                while (backwardIt.hasNext()) {
                    Instruction instruction = backwardIt.next().getValue();
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
     * 如果真是这样就好了。如果存在在多个块只def而从未Use的呢？这就会出问题...为此专门处理一下
     */
    HashMap<VReg, MipsBlock> defBlockMap = new HashMap<>();
    HashSet<VReg> riskyDefs = new HashSet<>();
    private void classifyLocalVRegs() {
        for (MipsBlock block : function.getBlocks()) {
            BlockLivingData<VReg> livingData = livingDataMap.get(block);
            Set<VReg> in = livingData.getIn();
            Set<VReg> out = livingData.getOut();
            Set<VReg> local = new HashSet<>();
            for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                Instruction instruction = node.getValue();
                BlockLivingData.union(local, instruction.getDefVRegs());
                BlockLivingData.union(local, instruction.getUseVRegs());
                for (VReg def : instruction.getDefVRegs()) {
                    if (!defBlockMap.containsKey(def)) {
                        defBlockMap.put(def, block);
                    } else {
                        if (defBlockMap.get(def) != block) {
                            // System.out.println("Risky def: " + def.toString());
                            riskyDefs.add(def);
                        }
                    }
                }
            }
            BlockLivingData.minus(local, in);
            BlockLivingData.minus(local, out);
            // local集初始化为空，直接加即可
            livingData.getLocal().addAll(local);
        }
        for (MipsBlock block : function.getBlocks()) {
            BlockLivingData<VReg> livingData = livingDataMap.get(block);
            livingData.getLocal().removeAll(riskyDefs);
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

            Set<VReg> local = livingData.getLocal();
            HashSet<VReg> liveAfter = new HashSet<>(livingData.getOut());

            Iterator<DoublyLinkedList.Node<Instruction>> backwardIt = block.getInstructions().backwardIterator();
            while (backwardIt.hasNext()) {
                Instruction instruction = backwardIt.next().getValue();
                for (VReg def : instruction.getDefVRegs()) {
                    if (!local.contains(def)) {
                        graph.addVertex(def);
                        liveAfter.remove(def);
                        for (VReg v : liveAfter) {
                            graph.addVertex(v);
                            graph.addEdge(def, v);
                        }
                    }
                }
                for (VReg use : instruction.getUseVRegs()) {
                    if (!local.contains(use)) {
                        liveAfter.add(use);
                        graph.addVertex(use);
                    }
                }
            }

            for (VReg reg1 : liveAfter) {
                for (VReg reg2 : liveAfter) {
                    if (!reg1.equals(reg2)) {
                        graph.addVertex(reg1);
                        graph.addVertex(reg2);
                        graph.addEdge(reg1, reg2);
                    }
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
            if (unavailablePRegs.size() >= Config.availPRegs.size()) {
                allocateSpillLoc(t);
                spilledVRegs.add(t);
            } else {
                PReg color = choosePRegGlobal(t, unavailablePRegs);
                colorMap.put(t, color);
                assignedPRegs.add(color);
                protectedPRegs.add(color);
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
                        if (color != null && !unavailablePRegs.contains(color)) return colorMap.get(z);
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
            for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                Instruction instruction = node.getValue();
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
                            if (color != null && !unavailablePRegs.contains(color)) { return colorMap.get(z); }
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

        HashMap<VReg, VReg> newNameMap = new HashMap<>();

        private void insertGlobalSpill() {
            HashSet<VReg> insertStore = new HashSet<>(spilledVRegs);
            HashSet<VReg> insertLoad = new HashSet<>();

            Iterator<DoublyLinkedList.Node<Instruction>> backwardIterator = block.getInstructions().backwardIterator();
            while (backwardIterator.hasNext()) {
                DoublyLinkedList.Node<Instruction> node = backwardIterator.next();
                Instruction instruction = node.getValue();
                for (VReg t : instruction.getDefVRegs()) {
                    if (spilledVRegs.contains(t)) {
                        if (insertStore.contains(t)) {
                            insertStore.remove(t);
                            // insert store T, memory(T) after I
                            block.insertAfter(
                                getSpillStore(t, getSpillLoc(t)),
                                node
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

            for (DoublyLinkedList.Node<Instruction> node : block.getInstructions()) {
                Instruction instruction = node.getValue();
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
                                node
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

            Iterator<DoublyLinkedList.Node<Instruction>> backwardIterator = block.getInstructions().backwardIterator();
            while (backwardIterator.hasNext()) {
                Instruction instruction = backwardIterator.next().getValue();
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

            // liveThrough表示整个块内活跃，且有引用的变量
            // 由上面过程可知，我们应该把它从liveStart里去除
            liveStart.removeAll(liveThrough);

            liveEnd = new HashSet<>(livingDataMap.get(block).getOut());
            liveEnd.removeAll(liveThrough);
            liveEnd.removeAll(liveTransparent);
            liveEnd.removeAll(liveStart);
        }

        private void buildLocalConflictGraph() {
            localConflictGraph = new UndirectedGraph<>();
            HashSet<VReg> live = new HashSet<>(livingDataMap.get(block).getOut());
            live.removeAll(spilledVRegs);
            int timeCount = 0;
            for (VReg t : live) {
                endTimeR.put(t, timeCount);
            }

            Iterator<DoublyLinkedList.Node<Instruction>> backwardIterator = block.getInstructions().backwardIterator();
            while (backwardIterator.hasNext()) {
                Instruction instruction = backwardIterator.next().getValue();
                timeCount++;
                endTimeI.put(instruction, timeCount);
                for (VReg t : instruction.getDefVRegs()) {
                    startTimeR.put(t, timeCount);
                    live.remove(t);
                    localConflictGraph.addVertex(t);
                    for (VReg u : live) {
                        localConflictGraph.addVertex(u);
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
            for (VReg t : live) {
                startTimeR.put(t, Integer.MAX_VALUE); // Better not...
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
            int maxNeighbors = localConflictGraph.getVertices().stream().map(localConflictGraph::getEdgeCount).max(Integer::compare).orElse(0); // 这个orElse其实不用
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


        private HashMap<PReg, Integer> liveStartAvailMap = new HashMap<>();
        HashSet<PReg> liveStartPRegs = new HashSet<>();



        private void onePassAllocate() {
            // 对于那些仍留在图中的局部VReg分配
            // 不溢出，不能使用的；liveStart（前面已经处理过），liveThrough/Transparent（始终占用）
            // 可以复用的：liveEnd
            // 这是建立在这样一个假设上：倒序遍历，如果一个寄存器可用，则在当前指令前方，这个寄存器一定没有被使用过
            // 我们当前的spill破坏了这个假设，所以引起了问题，需要专门解决
            HashSet<PReg> freePRegs = new HashSet<>(Config.availPRegs);

            for (VReg vReg : liveStart) {
                // System.out.println("liveStart " + vReg + " with PReg " + colorMap.get(vReg).toMIPS());
                liveStartPRegs.add(colorMap.get(vReg));
            }

            freePRegs.removeAll(liveStartPRegs); // 这个，不需要了...？实践下来发现还是得留着？？

            for (VReg vReg : liveThrough) {
                freePRegs.remove(colorMap.get(vReg));
            }
            for (VReg vReg : liveTransparent) {
                freePRegs.remove(colorMap.get(vReg));
            }
            for (VReg vReg : liveEnd) {
                // System.out.println("liveEnd " + vReg + " with PReg " + colorMap.get(vReg).toMIPS());
                freePRegs.remove(colorMap.get(vReg)); // 这个在遍历的时候会回收进freePRegs
            }

            HashSet<VReg> live = new HashSet<>(livingDataMap.get(block).getOut());
            live.removeAll(spilledVRegs); // Out集可能包含已经溢出的全局VReg；他们已经被处理为局部变量，故不应出现

            Iterator<DoublyLinkedList.Node<Instruction>> backwardIterator = block.getInstructions().backwardIterator();
            while (backwardIterator.hasNext()) {
                DoublyLinkedList.Node<Instruction> node = backwardIterator.next();
                Instruction instruction = node.getValue();

                for (VReg t : instruction.getDefVRegs()) {
                    live.remove(t);
                    // 此时定义部分也该分配pReg！
                    getColorForOnePass(freePRegs, t);
                    if (!instruction.getUseVRegs().contains(t)) {
                        // liveStart的pReg也可以回收，下面getColor会判断能否分配；不能的话，溢出逻辑应该能解决问题
                        // !instruction.getUseVRegs().contains(t)要有，否则两个不同use会被分到同一个pReg
                        if (colorMap.get(t) != null) {
                            // System.out.println(block + " Released: " + colorMap.get(t).toMIPS());
                            if (!liveStartPRegs.contains(colorMap.get(t))) freePRegs.add(colorMap.get(t));
                        }
                    }
                    if (getSpillLoc(t) != null) {
                        releaseSpillLoc(getSpillLoc(t));
                    }
                }
                for (VReg t : instruction.getUseVRegs()) {
                    // live内的一定被分配过
                    if (!live.contains(t)) {
                        live.add(t);
                        if (colorMap.containsKey(t)) {
                            PReg pReg = colorMap.get(t);
                            // 如果这个寄存器还在 free 池子里，必须把它拿走，因为它现在被占用了！
                            if (pReg != null) {
                                freePRegs.remove(pReg);
                            }
                        }
                        // 只对没入栈的分配
                        // 没被分配寄存器的，一定是局部变量
                        getColorForOnePass(freePRegs, t);
                    }
                }
            }
        }

        private void getColorForOnePass(HashSet<PReg> freePRegs, VReg t) {
            // 在这么改之后，不能被分配的VReg会保持null，需要在上面插load/store
            if (!colorMap.containsKey(t) && inLocalGraph.getOrDefault(t, false)) {
                // 仅在只def不use的情况时出现
                // 此时live集内一定没有t，上面有live.remove(t)也不会影响
                // 从优化角度而言，这种指令应该被删掉，从而减小寄存器分配的压力
                // 只考虑不在栈内的变量
                PReg color = null;
                if (!freePRegs.isEmpty()) {
                    boolean avail = false;
                    for (PReg pReg : freePRegs) {
                        if (liveStartAvailMap.containsKey(pReg)) {
                            if (startTimeR.get(t) < liveStartAvailMap.get(pReg)) {
                                avail = true;
                                color = pReg;
                                break;
                            }
                        } else {
                            HashSet<PReg> unavail = new HashSet<>();
                            for (VReg u : localConflictGraph.getNeighbors(t)) {
                                unavail.add(colorMap.get(u));
                            }
                            if (!unavail.contains(pReg)) {
                                avail = true;
                                color = pReg;
                                break;
                            }
                        }
                    }
                    if (avail) {
                        PReg s = color;
                        freePRegs.remove(s);
                        colorMap.put(t, s);
                        assignedPRegs.add(s);
                        // System.out.println(t.toString() + " got from onePass: " + colorMap.get(t).toMIPS());
                    } else {
                        allocateSpillLoc(t);
                    }
                } else {
                    allocateSpillLoc(t);
                }
            }
        }

        private void giveStackedVRegsColor() {
            // 在Stacked内，一定能找到可用的PReg
            while (!stack.isEmpty()) {
                VReg t = stack.pop();
                HashSet<PReg> unavailable = new HashSet<>();
                for (VReg u : localConflictGraph.getNeighbors(t)) {
                    // System.out.println(t.toString() + " neighbors: " + u.toString());
                    unavailable.add(colorMap.get(u));
                }
                PReg color = choosePRegLocal(t, unavailable);
                colorMap.put(t, color);
                assignedPRegs.add(color);
                inLocalGraph.put(t, true);
                // System.out.println(t.toString() + " got " + color.toMIPS());
            }
        }
    }
}
