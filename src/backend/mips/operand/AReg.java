package backend.mips.operand;

public class AReg extends PReg {
    // Arch registers, that is, $1~$31, $0 excluded
    private int id;
    private String name;
    
    public AReg(int id, String name) {
        this.id = id;
        this.name = name;
    }
    
    public static final AReg zero = new AReg(0, "$zero");
    public static final AReg at = new AReg(1, "$at");
    public static final AReg v0 = new AReg(2, "$v0");
    public static final AReg v1 = new AReg(3, "$v1");
    public static final AReg k0 = new AReg(26, "$k0");
    public static final AReg k1 = new AReg(27, "$k1");
    public static final AReg gp = new AReg(28, "$gp");
    public static final AReg sp = new AReg(29, "$sp");
    public static final AReg fp = new AReg(30, "$fp");
    public static final AReg ra = new AReg(31, "$ra");

    public static final AReg[] a = new AReg[] {
        new AReg(4, "$a0"),
        new AReg(5, "$a1"),
        new AReg(6, "$a2"),
        new AReg(7, "$a3")
    };

    public static final AReg[] t = new AReg[] {
        new AReg(8, "$t0"), new AReg(9, "$t1"),
        new AReg(10, "$t2"), new AReg(11, "$t3"),
        new AReg(12, "$t4"), new AReg(13, "$t5"),
        new AReg(14, "$t6"), new AReg(15, "$t7"),
        new AReg(24, "$t8"), new AReg(25, "$t9")
    };

    public static final AReg[] s = new AReg[] {
        new AReg(16, "$s0"), new AReg(17, "$s1"),
        new AReg(18, "$s2"), new AReg(19, "$s3"),
        new AReg(20, "$s4"), new AReg(21, "$s5"),
        new AReg(22, "$s6"), new AReg(23, "$s7")
    };

    @Override
    public String toMIPS() {
        return name;
    }
}
