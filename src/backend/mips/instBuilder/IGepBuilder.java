package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.Calc;
import backend.mips.instruction.Instruction;
import backend.mips.instruction.LoadAddr;
import backend.mips.instruction.Shift;
import backend.mips.operand.Immediate;
import backend.mips.operand.Operand;
import backend.mips.operand.VReg;
import frontend.datatype.ArrayType;
import frontend.datatype.DataType;
import frontend.datatype.PointerType;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IGep;
import frontend.llvm.value.instruction.Inst;

import java.util.LinkedList;
import java.util.List;

public class IGepBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        List<Instruction> ret = new LinkedList<>();
        VReg reg = builder.getVRegFromValue(inst);
        Value pointer = inst.getOperand(0);

        Operand ptrOperand;
        if (pointer.getName().startsWith("@")) {
            ptrOperand = new VReg();
            ret.add(
                new LoadAddr(
                    new Immediate(builder.getGlobalVarTag(pointer)),
                    ptrOperand
                )
            );
        } else {
            ptrOperand = builder.getVRegFromValue(pointer);
        }

        Value offset = inst.getOperand(1);
        DataType baseType;
        if (((IGep)inst).isFromArgs()) {
            baseType = ((PointerType) pointer.getType()).getBaseType();
        } else {
            baseType = ((ArrayType) ((PointerType) pointer.getType()).getBaseType()).getBaseType();
        }

        if (offset instanceof IntConstant intConstant) {
            ret.add(
                new Calc(
                    Calc.Op.addiu,
                    reg,
                    ptrOperand,
                    new Immediate(
                        intConstant.getValue() * baseType.getSize()
                    )
                )
            );
        } else {
            VReg temp = new VReg();
            ret.addAll(List.of(
                // TODO: 此处假设没有多维数组；数组内只可能出现Int；为了优化性能，改为移位
                new Shift(
                    Shift.Op.sll,
                    temp,
                    builder.getVRegFromValue(offset),
                    new Immediate(32 - Integer.numberOfLeadingZeros(baseType.getSize()) - 1)
                ),
                new Calc(Calc.Op.addu, reg, ptrOperand, temp)
            ));
        }
        return ret;
    }

}
