package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.*;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.VReg;
import frontend.datatype.VoidType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICall;
import frontend.llvm.value.instruction.Inst;

import java.util.LinkedList;
import java.util.List;

public class ICallBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        String functionName = ((ICall) inst).getFunction().getName();
        if (
            functionName.equals("putch")
            || functionName.equals("getint")
            || functionName.equals("putint")
        ) {
            return buildReservedCall(inst, builder);
        } else {
            return buildFuncCall(inst, block, builder);
        }
    }

    private static List<Instruction> buildReservedCall(Inst inst, MipsBuilder builder) {
        ICall call = (ICall) inst;
        // putstr我们没用，所以只用考虑这三个
        if (call.getFunction().getName().equals("putch") && call.getOperand(0) instanceof IntConstant intConstant) {
            // 加入buffer，等待输出
            builder.appendOutputBuffer((char) intConstant.getValue());
            return List.of();
        }
        if (call.getFunction().getName().equals("putch")) {
            return buildSyscallWrite(11, call.getOperand(0), builder);
        }
        if (call.getFunction().getName().equals("putint")) {
            return buildSyscallWrite(1, call.getOperand(0), builder);
        }
        if (call.getFunction().getName().equals("getint")) {
            VReg reg = builder.getVRegFromValue(call);
            return buildSyscallRead(5, reg);
        }
        throw new RuntimeException("Unknown function: " + call.getFunction().getName());
    }

    private static List<Instruction> buildFuncCall(Inst inst, MipsBlock block, MipsBuilder builder) {
        List<Instruction> ret = new LinkedList<>();
        MipsBlock target = builder.getMipsFunction(((ICall) inst).getFunction()).getEntry();
        for (int i = 0; i < inst.getOperands().size() && i < AReg.a.length; i++) {
            ret.add(
                new Calc(
                    Calc.Op.addiu, AReg.a[i],
                    convertToVReg(ret, inst.getOperand(i), builder), new Immediate(0)
                )
            );
        }

        for (int i = AReg.a.length; i < inst.getOperands().size(); i++) {
            ret.add(
                // 此处是caller，所以用sp
                new Store(
                    Mem.Align.w,
                    convertToVReg(ret, inst.getOperand(i), builder),
                    AReg.sp, new Immediate(MipsBuilder.argsOffset(i))
                )
            );
        }

        ret.add(
            new Jump(Jump.Op.jal, target)
        );
        if (!(inst.getType() instanceof VoidType)) {
            VReg retValue = builder.getVRegFromValue(inst);
            ret.add(
                new Calc(
                    Calc.Op.addiu, retValue, AReg.v0, new Immediate(0)
                )
            );
        }
        return ret;
    }

    private static VReg convertToVReg(List<Instruction> instructions, Value value, MipsBuilder builder) {
        if (value instanceof IntConstant intConstant) {
            VReg reg = new VReg();
            builder.addValueMapping(value, reg);
            instructions.add(
                new Calc(
                    Calc.Op.addiu, reg, AReg.zero, new Immediate(intConstant.getValue())
                )
            );
            return reg;
        }
        return builder.getVRegFromValue(value);
    }

    private static List<Instruction> buildSyscallRead(int syscall, VReg reg) {
        return List.of(
            new Calc(Calc.Op.addiu, AReg.v0, AReg.zero, new Immediate(syscall)),
            new Syscall(),
            new Calc(Calc.Op.addiu, reg, AReg.v0, new Immediate(0))
        );
    }

    private static List<Instruction> buildSyscallWrite(int syscall, Value value, MipsBuilder builder) {
        Instruction load;
        if (value instanceof IntConstant intConstant) {
            load = new Calc(
                Calc.Op.addiu, AReg.a[0], AReg.zero, new Immediate(intConstant.getValue())
            );
        } else {
            load = new Calc(
                Calc.Op.addiu, AReg.a[0], builder.getVRegFromValue(value), new Immediate(0)
            );
        }
        return List.of(
            load,
            new Calc(Calc.Op.addiu, AReg.v0, AReg.zero, new Immediate(syscall)),
            new Syscall()
        );
    }


}
