package frontend.syntax.statement;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.logical.CondExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

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

    public static ForBlockStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        tokenStream.next(TokenType.LeftParen);
        ForStmt init = tokenStream.check(TokenType.Semicolon) ? null : ForStmt.parse(tokenStream, errors);
        tokenStream.next(TokenType.Semicolon);
        CondExp cond = tokenStream.check(TokenType.Semicolon) ? null : CondExp.parse(tokenStream, errors);
        tokenStream.next(TokenType.Semicolon);
        ForStmt step = tokenStream.check(TokenType.RightParen) ? null : ForStmt.parse(tokenStream, errors);
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        Stmt stmt = Stmt.parse(tokenStream, errors);
        return new ForBlockStmt(init, cond, step, stmt);
    }

    @Override
    public void visit() {
        Tabulator.intoLoop();
        if (initStmt != null) initStmt.visit();
        if (condExp != null) condExp.visit();
        if (thenStmt != null) thenStmt.visit();
        stmt.visit();
        Tabulator.exitLoop();
    }
}
