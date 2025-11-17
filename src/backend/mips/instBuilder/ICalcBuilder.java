package backend.mips.instBuilder;

import backend.mips.MipsBlock;
import backend.mips.MipsBuilder;
import backend.mips.instruction.*;
import backend.mips.operand.AReg;
import backend.mips.operand.Immediate;
import backend.mips.operand.Operand;
import backend.mips.operand.VReg;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICalc;
import frontend.llvm.value.instruction.ICompare;
import frontend.llvm.value.instruction.Inst;
import frontend.llvm.value.instruction.Operator;

import java.util.ArrayList;
import java.util.List;

public class ICalcBuilder extends InstBuilder {

    public static List<Instruction> build(Inst inst, MipsBlock block, MipsBuilder builder) {
        Operator op;
        if (inst instanceof ICalc) {
            op = ((ICalc) inst).getOp();
        } else {
            op = ((ICompare) inst).getOp();
        }

        // 两个都是立即数的情况
        if ((inst.getOperand(0) instanceof IntConstant) && (inst.getOperand(1) instanceof IntConstant)) {
            VReg reg = builder.getVRegFromValue(inst);
            return List.of(
                new Calc(
                    Calc.Op.addiu,
                    reg,
                    AReg.zero,
                    new Immediate(
                        op.calc(
                            ((IntConstant) inst.getOperand(0)).getValue(),
                            ((IntConstant) inst.getOperand(1)).getValue()
                        )
                    )
                )
            );
        }

        // 下面的话，只可能有其中一个操作数是立即数
        switch (op) {
            case ADD -> {
                return chooseImmInst(inst, Calc.Op.addu, Calc.Op.addiu, builder);
            }
            case SUB -> {
                return buildSub(inst, builder);
            }
            case MUL -> {
                return buildMulDiv(inst, MulDiv.Op.mult, RegMove.Op.mflo, builder);
            }
            // 除法代价太大！因此先在此做一些优化
            case DIV -> {
                return buildFastDiv(inst, RegMove.Op.mflo, builder);
            }
            case MOD -> {
                return buildFastDiv(inst, RegMove.Op.mfhi, builder);
            }
            case EQ -> {
                return buildCompare(inst, Calc.Op.seq, builder);
            }
            case NE -> {
                return buildCompare(inst, Calc.Op.sne, builder);
            }
            case LE -> {
                return buildCompare(inst, Calc.Op.sle, builder);
            }
            case GE -> {
                return buildCompare(inst, Calc.Op.sge, builder);
            }
            case LT -> {
                return buildCompare(inst, Calc.Op.slt, builder);
            }
            case GT -> {
                return buildCompare(inst, Calc.Op.sgt, builder);
            }
            default -> throw new RuntimeException("Unknown operator " + op);
        }
    }

    /**
     * 对于**顺序无关**的指令，处理操作数其中一个是立即数的情况
     * @param inst
     * @param op
     * @param immOp
     * @return
     */
    private static List<Instruction> chooseImmInst(Inst inst, Calc.Op op, Calc.Op immOp, MipsBuilder builder) {
        Value l = inst.getOperand(0);
        Value r = inst.getOperand(1);
        VReg reg = builder.getVRegFromValue(inst);
        if (l instanceof IntConstant intConstant) {
            return List.of(
                new Calc(
                    immOp,
                    reg,
                    builder.getVRegFromValue(r),
                    new Immediate(intConstant.getValue())
                )
            );
        }
        if (r instanceof IntConstant intConstant) {
            return List.of(
                new Calc(
                    immOp,
                    reg,
                    builder.getVRegFromValue(l),
                    new Immediate(intConstant.getValue())
                )
            );
        }
        return List.of(
            new Calc(
                op,
                reg,
                builder.getVRegFromValue(l),
                builder.getVRegFromValue(r)
            )
        );
    }

    private static List<Instruction> buildSub(Inst inst, MipsBuilder builder) {
        Value l = inst.getOperand(0);
        Value r = inst.getOperand(1);
        VReg reg = builder.getVRegFromValue(inst);
        if (l instanceof IntConstant intConstant) {
            return List.of(
                new Calc(
                    Calc.Op.addiu,
                    AReg.at,
                    AReg.zero,
                    new Immediate(intConstant.getValue())
                ),
                new Calc(
                    Calc.Op.subu,
                    reg,
                    AReg.at,
                    builder.getVRegFromValue(r)
                )
            );
        } else if (r instanceof IntConstant intConstant) {
            return List.of(
                new Calc(
                    Calc.Op.addiu,
                    reg,
                    builder.getVRegFromValue(l),
                    new Immediate(-intConstant.getValue())
                )
            );
        } else {
            return List.of(
                new Calc(
                    Calc.Op.subu,
                    reg,
                    builder.getVRegFromValue(l),
                    builder.getVRegFromValue(r)
                )
            );
        }
    }

    private static List<Instruction> buildCompare(Inst inst, Calc.Op op, MipsBuilder builder) {
        Value l = inst.getOperand(0);
        Value r = inst.getOperand(1);
        VReg reg = builder.getVRegFromValue(inst);
        List<Instruction> res = new ArrayList<>();
        Operand lOperand, rOperand;

        if (l instanceof IntConstant intConstant) {
            lOperand = new VReg();
            res.add(
                new Calc(
                    Calc.Op.addiu,
                    lOperand,
                    AReg.zero,
                    new Immediate(intConstant.getValue())
                )
            );
        } else {
            lOperand = builder.getVRegFromValue(l);
        }

        if (r instanceof IntConstant intConstant) {
            rOperand = new VReg();
            res.add(
                new Calc(
                    Calc.Op.addiu,
                    rOperand,
                    AReg.zero,
                    new Immediate(intConstant.getValue())
                )
            );
        } else {
            rOperand = builder.getVRegFromValue(r);
        }

        res.add(
            new Calc(
                op,
                reg,
                lOperand,
                rOperand
            )
        );
        return res;
    }

    private static List<Instruction> buildMulDiv(Inst inst, MulDiv.Op op, RegMove.Op moveOp, MipsBuilder builder) {
        Value l = inst.getOperand(0);
        Value r = inst.getOperand(1);
        List<Instruction> res = new ArrayList<>();
        VReg reg = builder.getVRegFromValue(inst);
        if (l instanceof IntConstant intConstant) {
            res.add(
                new Calc(
                    Calc.Op.addiu,
                    AReg.at,
                    AReg.zero,
                    new Immediate(intConstant.getValue())
                )
            );
            res.add(
                new MulDiv(
                    op,
                    AReg.at,
                    builder.getVRegFromValue(r)
                )
            );
        } else if (r instanceof IntConstant intConstant) {
            res.add(
                new Calc(
                    Calc.Op.addiu,
                    AReg.at,
                    AReg.zero,
                    new Immediate(intConstant.getValue())
                )
            );
            res.add(
                new MulDiv(
                    op,
                    builder.getVRegFromValue(l),
                    AReg.at
                )
            );
        } else {
            res.add(
                new MulDiv(
                    op,
                    builder.getVRegFromValue(l),
                    builder.getVRegFromValue(r)
                )
            );
        }
        res.add(
            new RegMove(moveOp, reg)
        );
        return res;
    }

    private static List<Instruction> buildFastDiv(Inst inst, RegMove.Op op, MipsBuilder builder) {
        // 这里只处理了取余的情况
        if (
            !(inst.getOperand(0) instanceof IntConstant) &&
            inst.getOperand(1) instanceof IntConstant intConstant &&
            intConstant.getValue() != 0 &&
            op == RegMove.Op.mfhi
        ) {
            if (intConstant.getValue() == 1) {
                // = 0
                VReg reg = builder.getVRegFromValue(inst);
                return List.of(
                    new Calc(Calc.Op.addu, reg, AReg.zero, AReg.zero)
                );
            }
            // 2的次幂
            if (intConstant.getValue() > 0 &&
                Integer.numberOfLeadingZeros(intConstant.getValue()) + Integer.numberOfTrailingZeros(intConstant.getValue()) == 31
            ) {
                int k = Integer.numberOfTrailingZeros(intConstant.getValue());
                Operand operand = builder.getVRegFromValue(inst.getOperand(0));
                VReg reg = builder.getVRegFromValue(inst);

                // 要做的事情其实很简单：保留低位，再在高位把符号或上
                VReg step1 = new VReg();
                VReg step2 = new VReg();
                VReg step3 = new VReg();
                VReg temp1 = new VReg();
                VReg temp2 = new VReg();
                return List.of(
                    new Calc(Calc.Op.andi, step1, operand, new Immediate(intConstant.getValue() - 1)),
                    new Calc(Calc.Op.addiu, step2, step1, new Immediate(-1)),
                    new Shift(Shift.Op.sra, temp1, operand, new Immediate(31)),
                    new Shift(Shift.Op.sll, temp2, temp1, new Immediate(k)),
                    new Calc(Calc.Op.or, step3, step2, temp2),
                    new Calc(Calc.Op.addiu, reg, step3, new Immediate(1))
                );
            }
        }
        return buildMulDiv(inst, MulDiv.Op.div, op, builder);
    }
}
