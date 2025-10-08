package frontend.syntax.declaration.object;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;

import java.util.ArrayList;

public class ConstInitVal extends ASTNode {
    public enum Type {
        Single, Multiple
    }
    private Type type;
    private ConstExp singleConstExp;
    private ArrayList<ConstExp> multipleConstExps;

    public ConstInitVal() {
        this.type = Type.Multiple;
        this.singleConstExp = null;
        this.multipleConstExps = new ArrayList<>();
    }

    public ConstInitVal(ConstExp singleConstExp) {
        this.type = Type.Single;
        this.singleConstExp = singleConstExp;
        this.multipleConstExps = null;
    }

    public void addConstExp(ConstExp constExp) {
        assert(this.type == Type.Multiple);
        this.multipleConstExps.add(constExp);
    }
}
