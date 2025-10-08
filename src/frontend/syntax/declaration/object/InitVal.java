package frontend.syntax.declaration.object;

import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;

import java.util.ArrayList;

public class InitVal extends ASTNode {
    public enum Type {
        Single, Multiple
    }
    private Type type;
    private Exp singleExp;
    private ArrayList<Exp> multipleExps;

    public InitVal() {
        this.type = Type.Multiple;
        this.singleExp = null;
        this.multipleExps = new ArrayList<>();
    }

    public InitVal(Exp singleExp) {
        this.type = Type.Single;
        this.singleExp = singleExp;
        this.multipleExps = null;
    }

    public void addMultipleExp(Exp multipleExp) {
        assert(this.type == Type.Multiple);
        this.multipleExps.add(multipleExp);
    }
}
