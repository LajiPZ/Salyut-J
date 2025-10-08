package frontend.syntax.statement;

import frontend.syntax.logical.CondExp;

public class ForBlockStmt extends Stmt {
    private ForStmt initStmt;
    private CondExp condExp;
    private ForStmt thenStmt;
    private Stmt stmt;

    public ForBlockStmt(ForStmt initStmt, CondExp condExp, ForStmt thenStmt, Stmt stmt) {
        super(Type.For);
        this.stmt = stmt;
        this.initStmt = initStmt;
        this.condExp = condExp;
        this.thenStmt = thenStmt;
    }

}
