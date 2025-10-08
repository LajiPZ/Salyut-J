package frontend.syntax.declaration.object;

import frontend.syntax.declaration.BType;

import java.util.ArrayList;

public class ConstDecl extends Decl {
    private BType type;
    private ConstDef lConstDef;
    private ArrayList<ConstDef> rConstDefs = new ArrayList<>();

    public ConstDecl(BType type, ConstDef lConstDef) {
        super(Type.ConstDecl);
        this.type = type;
        this.lConstDef = lConstDef;
    }

    public void addRConstDef(ConstDef rConstDef) {
        rConstDefs.add(rConstDef);
    }
}
