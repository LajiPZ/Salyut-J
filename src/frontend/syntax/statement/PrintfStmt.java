package frontend.syntax.statement;

import frontend.syntax.expression.Exp;
import frontend.token.Token;

import java.util.ArrayList;

public class PrintfStmt extends Stmt {
    private Token label;
    private String formatString;
    private ArrayList<Exp> args = new ArrayList<>();

    public PrintfStmt(Token label, String formatString) {
        super(Type.Printf);
        this.label = label;
        this.formatString = formatString;
    }

    public void addArgument(Exp exp) {
        args.add(exp);
    }
}
