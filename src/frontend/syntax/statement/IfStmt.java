package frontend.syntax.statement;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.instruction.IBranch;
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
        if (elseStmt != null) {
            elseStmt.visit();
        }
    }

    public void build(IrBuilder builder) {
        Value cond = ValueConverter.toBoolean(condExp.build(builder));
        BBlock blockBefore = builder.getInsertPoint();
        BBlock thenBBlk = builder.newBBlock(false);
        stmt.build(builder);
        BBlock thenEndBBlk = builder.getInsertPoint();
        BBlock mergeBBlk;
        if (elseStmt != null) {
            BBlock elseBBlk = builder.newBBlock(false);
            elseStmt.build(builder);
            BBlock elseEndBBlk = builder.getInsertPoint();
            mergeBBlk = builder.newBBlock(false);
            builder.insertInst(
                    blockBefore,
                    new IBranch(
                            cond,thenBBlk, elseBBlk
                    )
            );
            builder.insertInst(
                    thenEndBBlk,
                    new IBranch(
                            mergeBBlk
                    )
            );
            builder.insertInst(
                    elseEndBBlk,
                    new IBranch(
                            mergeBBlk
                    )
            );
        } else {
            mergeBBlk = builder.newBBlock(false);
            builder.insertInst(
                    blockBefore,
                    new IBranch(
                            cond, thenBBlk, mergeBBlk
                    )
            );
            builder.insertInst(
                    thenEndBBlk,
                    new IBranch(
                            mergeBBlk
                    )
            );
        }
    }
}
