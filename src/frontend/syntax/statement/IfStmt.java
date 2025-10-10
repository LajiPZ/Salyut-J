package frontend.syntax.statement;

import frontend.syntax.logical.CondExp;
import frontend.token.Token;

public class IfStmt extends Stmt {
    private Token label;
    private CondExp condExp;
    private Stmt stmt;
    private Stmt elseStmt;

    public IfStmt(Token label, CondExp condExp, Stmt stmt) {
        super(Type.If);
        this.label = label;
        this.condExp = condExp;
        this.stmt = stmt;
        this.elseStmt = null;
    }

    public IfStmt(Token label, CondExp condExp, Stmt stmt, Stmt elseStmt) {
        super(Type.If);
        this.label = label;
        this.condExp = condExp;
        this.stmt = stmt;
        this.elseStmt = elseStmt;
    }

}
