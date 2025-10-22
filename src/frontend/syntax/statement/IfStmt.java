package frontend.syntax.statement;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.logical.CondExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

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

    public static IfStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        tokenStream.next(TokenType.LeftParen);
        CondExp cond = CondExp.parse(tokenStream, errors);
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        Stmt stmt = Stmt.parse(tokenStream, errors);
        if (tokenStream.checkPoll(TokenType.Else)) {
            Stmt elseStmt = Stmt.parse(tokenStream, errors);
            return new IfStmt(token, cond, stmt, elseStmt);
        }
        return new IfStmt(token, cond, stmt);
    }

    @Override
    public void visit() {
        condExp.visit();
        stmt.visit();
        Tabulator.setActualReturnType(Tabulator.FuncReturnType.Void); // 由此解决控制流引起的问题
        if (elseStmt != null) {
            elseStmt.visit();
            Tabulator.setActualReturnType(Tabulator.FuncReturnType.Void); // 同上
        }
    }

}
