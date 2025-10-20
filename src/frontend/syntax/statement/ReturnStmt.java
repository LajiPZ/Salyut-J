package frontend.syntax.statement;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.expression.Exp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class ReturnStmt extends Stmt {
    private Token label;
    private Exp expr;

    public ReturnStmt(Token label, Exp expr) {
        super(Type.Return);
        this.label = label;
        this.expr = expr;
    }

    public ReturnStmt(Token label) {
        super(Type.Return);
        this.label = label;
        this.expr = null;
    }

    public static ReturnStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        if (!tokenStream.check(TokenType.Semicolon)) {
            Exp expr = Exp.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.Semicolon)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
                );
            }
            return new ReturnStmt(token,expr);
        }
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return new ReturnStmt(token);
    }

    @Override
    public void visit() {
        if (this.expr != null) {
            Tabulator.setActualReturnType(Tabulator.FuncReturnType.Int);
        } else {
            Tabulator.setActualReturnType(Tabulator.FuncReturnType.Void);
        }
        expr.visit();
    }
}
