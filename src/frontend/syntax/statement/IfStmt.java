package frontend.syntax.statement;

import frontend.syntax.logical.CondExp;

public class IfStmt extends Stmt {
    private CondExp condExp;
    private Stmt stmt;
    private Stmt elseStmt;

    private IfStmt(CondExp condExp, Stmt stmt) {
        super(Type.If);
        this.condExp = condExp;
        this.stmt = stmt;
        this.elseStmt = null;
    }

    private IfStmt(CondExp condExp, Stmt stmt, Stmt elseStmt) {
        super(Type.If);
        this.condExp = condExp;
        this.stmt = stmt;
        this.elseStmt = elseStmt;
    }

}
