package backend.mips.operand;

import java.util.Arrays;
import java.util.HashSet;

public class CP1Reg extends PReg {
    // Float registers, that is, those in CP1
    // There are 32 of them in CP1, which seems more than appealing to me...

    private int id;
    private String name;

    public CP1Reg(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public static final CP1Reg[] f = new CP1Reg[] {
        new CP1Reg(0, "$f0"),
        new CP1Reg(1, "$f1"),
        new CP1Reg(2, "$f2"),
        new CP1Reg(3, "$f3"),
        new CP1Reg(4, "$f4"),
        new CP1Reg(5, "$f5"),
        new CP1Reg(6, "$f6"),
        new CP1Reg(7, "$f7"),
        new CP1Reg(8, "$f8"),
        new CP1Reg(9, "$f9"),
        new CP1Reg(10, "$f10"),
        new CP1Reg(11, "$f11"),
        new CP1Reg(12, "$f12"),
        new CP1Reg(13, "$f13"),
        new CP1Reg(14, "$f14"),
        new CP1Reg(15, "$f15"),
        new CP1Reg(16, "$f16"),
        new CP1Reg(17, "$f17"),
        new CP1Reg(18, "$f18"),
        new CP1Reg(19, "$f19"),
        new CP1Reg(20, "$f20"),
        new CP1Reg(21, "$f21"),
        new CP1Reg(22, "$f22"),
        new CP1Reg(23, "$f23"),
        new CP1Reg(24, "$f24"),
        new CP1Reg(25, "$f25"),
        new CP1Reg(26, "$f26"),
        new CP1Reg(27, "$f27"),
        new CP1Reg(28, "$f28"),
        new CP1Reg(29, "$f29"),
        new CP1Reg(30, "$f30"),
        new CP1Reg(31, "$f31"),
    };

    public static HashSet<CP1Reg> availableCP1Regs = new HashSet<>(Arrays.asList(f));

    @Override
    public String toMIPS() {
        return name;
    }
}
