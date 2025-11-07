package frontend.syntax.statement;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IBranch;
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

    public void build(IrBuilder builder) {
        // 1.init
        if (initStmt != null) initStmt.build(builder);
        // 2.cond; new blk required for cond check
        BBlock condBBlk = builder.newBBlock(true);
        builder.intoLoop(condBBlk);
        Value cond = (condExp != null) ?
                ValueConverter.toBoolean(condExp.build(builder)) :
                IntConstant.logicOne;
        BBlock condEndBBlk = builder.getInsertPoint();
        // 3.body
        BBlock bodyBBlk = builder.newBBlock(false);
        // 利用break填出口
        builder.getCurrentLoop().addBreak(
                (IBranch) builder.insertInst(
                        condEndBBlk,
                        new IBranch(
                                cond, bodyBBlk, null
                        )
                )
        );
        stmt.build(builder);

        // 4. update
        BBlock updateBBlk = builder.newBBlock(true);
        if (thenStmt != null) thenStmt.build(builder);
        BBlock updateEndBBlk = builder.getInsertPoint();
        builder.insertInst(
                new IBranch(condBBlk)
        );

        // 5.end
        BBlock forEndBBlk = builder.newBBlock(false); // 由cond跳过来
        builder.getCurrentLoop().fillBreakTarget(forEndBBlk);
        builder.getCurrentLoop().fillContinueTarget(updateBBlk);
        builder.exitLoop();
    }
}
