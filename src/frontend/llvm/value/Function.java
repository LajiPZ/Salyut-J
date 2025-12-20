package frontend.llvm.value;

import frontend.datatype.DataType;
import frontend.llvm.tools.ControlFlowGraph;
import frontend.llvm.tools.DominatorTree;
import frontend.llvm.tools.LoopInformation;
import utils.DoublyLinkedList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public class Function extends Value {
    private final DoublyLinkedList<BBlock> bBlocks;
    private final List<Value> params;
    private boolean isExtern = false;

    private int valCounter;

    // Properties for optimization-related analysis
    private ControlFlowGraph ctrlFlowGraph = null;
    private DominatorTree domTree = null;

    private HashMap<BBlock, LoopInformation> loopMap = null;
    private List<LoopInformation> loops = null;
    private List<LoopInformation> allLoops = null;

    public void setCtrlFlowGraph(ControlFlowGraph ctrlFlowGraph) {
        this.ctrlFlowGraph = ctrlFlowGraph;
    }

    public ControlFlowGraph getCtrlFlowGraph() {
        return ctrlFlowGraph;
    }

    public void setDomTree(DominatorTree domTree) {
        this.domTree = domTree;
    }

    public DominatorTree getDomTree() {
        return domTree;
    }

    public List<LoopInformation> getLoops() {
        return loops;
    }

    public void setLoops(List<LoopInformation> loops) {
        this.loops = loops;
    }

    public void setAllLoops(List<LoopInformation> allLoops) {
        this.allLoops = allLoops;
    }

    public void setLoopMap(HashMap<BBlock, LoopInformation> loopMap) {
        this.loopMap = loopMap;
    }

    public int resumeValCounter() {
        int prev = this.valCounter;
        this.valCounter = 0;
        return prev;
    }

    public void saveCurrentValCounter(int counter) {
        this.valCounter = counter;
    }

    public Function(String name, DataType type) {
        super(name, type);
        this.bBlocks = new DoublyLinkedList<>();
        this.params = new ArrayList<>();
    }

    public Value addParam(DataType type) {
        Value param = new Value(type);
        params.add(param);
        return param;
    }

    public static Function extern(String name, DataType type, DataType... args) {
        Function func = new Function(name, type);
        func.isExtern = true;
        for (DataType arg : args) {
            // declare只需输出类型，Value.counter的事情再议
            func.addParam(arg);
        }
        // 为了保证第一个函数定义，从参数开始，以%0开始计数，重置一下计数器
        Value.counter.reset();
        return func;
    }

    public void addBBlock(BBlock bBlock) {
        new DoublyLinkedList.Node<BBlock>(bBlock).insertIntoTail(bBlocks);
    }

    public List<Value> getParams() {
        return params;
    }

    public DoublyLinkedList<BBlock> getBBlocks() {
        return bBlocks;
    }

    @Override
    public String toString() {
        List<BBlock> output = new LinkedList<>();
        for (DoublyLinkedList.Node<BBlock> node : bBlocks) {
            BBlock bBlock = node.getValue();
            output.add(bBlock);
        }
        // 输出函数定义，external函数此处不关心
        return "define dso_local " +
                getType() +
                " @" + getName() +
                "(" +
                params.stream().map(Value::toString).collect(Collectors.joining(", ")) +
                ") {\n" +
                output.stream().map(Value::toString).collect(Collectors.joining("\n")) +
                "}\n";
    }
}
