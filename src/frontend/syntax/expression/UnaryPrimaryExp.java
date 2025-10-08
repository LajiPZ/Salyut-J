package frontend.syntax.expression;

public class UnaryPrimaryExp extends UnaryExp {
    private final PrimaryExp exp;

    public UnaryPrimaryExp(PrimaryExp exp) {
        super(Type.Primary);
        this.exp = exp;
    }
}
