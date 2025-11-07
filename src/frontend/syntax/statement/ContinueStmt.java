package frontend.syntax.statement;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.value.instruction.IBranch;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class ContinueStmt extends Stmt {
    private Token label;

    public ContinueStmt(Token label) {
        super(Type.Continue);
        this.label = label;
    }

    public static ContinueStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return new ContinueStmt(token);
    }

    @Override
    public void visit() {
        if (!Tabulator.inLoop()) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.BreakContinueOutsideLoop, label.getFileLoc())
            );
        }
    }

    public void build(IrBuilder builder) {
        builder.getCurrentLoop().addContinue(
                (IBranch) builder.insertInst(
                        new IBranch(null)
                )
        );
        builder.newBBlock(false); // 跳转后开新块...
    }
}
