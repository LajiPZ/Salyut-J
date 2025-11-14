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
    public static final AReg v0 = new AReg(2, "v0");
    public static final AReg v1 = new AReg(3, "v1");
    public static final AReg k0 = new AReg(26, "$k0");
    public static final AReg k1 = new AReg(27, "$k1");
    public static final AReg gp = new AReg(28, "$gp");
    public static final AReg sp = new AReg(29, "$sp");
    public static final AReg fp = new AReg(30, "$fp");
    public static final AReg ra = new AReg(31, "$ra");
}
