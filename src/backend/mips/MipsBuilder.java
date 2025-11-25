package backend.mips;

import backend.mips.instruction.*;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import frontend.datatype.CharType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Function;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICall;
import frontend.llvm.value.instruction.Inst;
import utils.Counter;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class MipsBuilder {
    // A helper for building MIPS asm.
    // This is designed specifically for a function...
    private int stackSize;
    private Map<BBlock, MipsBlock> blockMap;
    private Map<Function, MipsFunction> functionMap;
    private Map<Value, VReg> valueMap;
    private MipsBlock exitBlock;
    private MipsModule top;

    public MipsBuilder(MipsModule top, List<BBlock> bblocks, MipsBlock entry, MipsBlock exit) {
        bblocks.forEach(bblock -> {
            blockMap.put(bblock, new MipsBlock(bblock));
        });

        this.functionMap = top.getFunctionMap();
        this.exitBlock = exit;
        this.top = top;

        MipsBlock.addEdge(entry, getMipsBlock(bblocks.get(0)));

        buildValueMap();

        if (entry.isMainEntry()) {
            stackSize = 0;
        } else {
            stackSize = 4; // 存了调用者的fp
        }
    }

    private void buildValueMap() {
        for (BBlock bblock : blockMap.keySet()) {
            for (Inst instruction : bblock.getInstructions()) {
                if (instruction.getType() != null) {
                    // TODO: 要不要考虑Void?
                    valueMap.put(instruction, new VReg());
                }
            }
        }
    }

    public void addValueMapping(Value value, VReg reg) {
        valueMap.put(value, reg);
    }

    public List<Instruction> addParameters(List<Value> parameters) {
        List<Instruction> ret = new LinkedList<>();
        for (int i = 0; i < parameters.size(); i++) {
            VReg reg = new VReg();
            valueMap.put(parameters.get(i), reg);
            if (i >= AReg.a.length) {
                enlargeStack(4);
                ret.add(
                    new Load(
                        Mem.Align.w,
                        reg,
                        AReg.fp,
                        new Immediate(MipsBuilder.argsOffset(i))
                    )
                );
            } else {
                ret.add(
                    new Calc(
                        Calc.Op.addiu,
                        reg,
                        AReg.a[i],
                        new Immediate(0)
                    )
                );
            }
        }
        return ret;
    }

    public MipsBlock getMipsBlock(BBlock block) {
        return blockMap.get(block);
    }

    public VReg getVRegFromValue(Value value) {
        return valueMap.get(value);
    }

    public MipsFunction getMipsFunction(Function function) {
        return functionMap.get(function);
    }

    public String getGlobalVarTag(Value pointer) {
        assert pointer.getName().startsWith("@");
        return pointer.getName().substring(1) + ".addr";
    }

    public int getStackSize() {
        return stackSize;
    }

    public int enlargeStack(int size) {
        stackSize += size;
        // currentAddr = $fp - stackSize
        return -stackSize;
    }

    public static int argsOffset(int index) {
        // 1.栈从高地址到低地址增长
        // 为何要+2？ fp + 自身空间
        return (2 + index - AReg.a.length) * -4;
    }

    public MipsBlock getExitBlock() {
        return exitBlock;
    }

    private StringBuilder outputBuffer = new StringBuilder();
    private static Counter stringCounter = new Counter();
    private static HashMap<String, String> strTagMap = new HashMap<>();

    /**
     * 输出先前已缓存的输出字符串
     * @param inst
     * @return
     */
    public List<Instruction> preRun(Inst inst) {
        if (inst instanceof ICall call) {
            if (call.getFunction().getName().equals("putch") && call.getOperand(0) instanceof IntConstant) {
                // 在Build ICall的时候处理
                return List.of();
            }
        }
        if (outputBuffer.isEmpty()) {
            // 其他类型指令；没有待输出，则什么都不用做
            return List.of();
        }
        String content = outputBuffer.toString();
        String tag;
        if (strTagMap.containsKey(content)) {
            tag = strTagMap.get(content);
        } else {
            tag = "str." + stringCounter.get();
            strTagMap.put(content, tag);
            top.addGlobalVariable(
                new MipsGlobalVariable(
                    new CharType(),
                    tag,
                    content
                )
            );
        }
        outputBuffer = new StringBuilder();
        return List.of(
            new LoadAddr(new Immediate(tag), AReg.a[0]),
            new Calc(Calc.Op.addiu, AReg.v0, AReg.zero, new Immediate(4)),
            new Syscall()
        );
    }

    public void appendOutputBuffer(char chr) {
        outputBuffer.append(chr);
    }
}
