package frontend.syntax.expression;

import frontend.syntax.ASTNode;
import frontend.syntax.misc.LVal;
import frontend.syntax.misc.Number;

public class PrimaryExp extends ASTNode {
    public enum Type {
        Exp, LVal, Number
    }

    private final Type type;
    private final Object value;

    public PrimaryExp(Exp exp) {
        this.type = Type.Exp;
        this.value = exp;
    }

    public PrimaryExp(LVal lval) {
        this.type = Type.LVal;
        this.value = lval;
    }

    public PrimaryExp(Number number) {
        this.type = Type.Number;
        this.value = number;
    }



}
