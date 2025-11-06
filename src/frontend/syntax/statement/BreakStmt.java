package frontend.syntax.statement;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class BreakStmt extends Stmt {
    private Token label;

    public BreakStmt(Token label) {
        super(Type.Break);
        this.label = label;
    }

    public static BreakStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return new BreakStmt(token);
    }

    @Override
    public void visit() {
        if (!Tabulator.inLoop()) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.BreakContinueOutsideLoop, label.getFileLoc())
            );
        }
    }
    
    @Override
    public void build(IrBuilder builder) {

    }
}
