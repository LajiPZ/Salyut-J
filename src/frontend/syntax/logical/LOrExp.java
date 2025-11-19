package frontend.syntax.logical;

import frontend.IrBuilder;
import frontend.datatype.BooleanType;
import frontend.error.ErrorEntry;
import frontend.llvm.value.BBlock;
import frontend.llvm.value.Value;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.IBranch;
import frontend.llvm.value.instruction.IPhi;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class LOrExp extends ASTNode {
    private final ArrayList<LAndExp> lAndExps = new ArrayList<>();

    public LOrExp() {}

    public void addLAndExp(LAndExp LAndExp) {
        this.lAndExps.add(LAndExp);
    }

    public static LOrExp parse(TokenStream ts, List<ErrorEntry> errors) {
        LOrExp lOrExp = new LOrExp();
        lOrExp.addLAndExp(LAndExp.parse(ts, errors));
        while (ts.check(TokenType.Or)) {
            ts.logParse("<LOrExp>");
            ts.poll();
            lOrExp.addLAndExp(LAndExp.parse(ts, errors));
        }
        ts.logParse("<LOrExp>");
        return lOrExp;
    }

    public void visit() {
        for (LAndExp lAndExp : lAndExps) {
            lAndExp.visit();
        }
    }

    public Value build(IrBuilder builder) {
        // 通过给每个条件开bblk, 每个blk带跳转，实现短路取值
        // 要跳入的bblk在完全遍历之后才知道，所以要先放Null再填
        if (lAndExps.size() == 1) {
            return lAndExps.get(0).build(builder);
        }
        List<BBlock> bBlocks = new ArrayList<>();
        BBlock currentBlock = builder.getInsertPoint();
        for (LAndExp lAndExp : lAndExps) {
            Value cond = ValueConverter.toBoolean(lAndExp.build(builder));
            currentBlock = builder.getInsertPoint();
            BBlock nextBBlk = builder.newBBlock(false);
            builder.insertInst(
                    currentBlock,
                    new IBranch(
                            cond, null, nextBBlk
                    )
            );
            bBlocks.add(currentBlock);
            currentBlock = nextBBlk;
        }
        // 此时，currentBlk为求值为1对应的路径，最后一blk对应是求值为0的路径
        // 随后，各路径汇入一个bblk,继续后续编译
        // 利用Phi即可求解
        builder.fillBranchTarget(currentBlock, bBlocks, builder);
        IPhi phi = new IPhi(new BooleanType());
        for (BBlock bBlock : bBlocks) {
            phi.addSourcePair(bBlock, IntConstant.logicOne);
        }
        phi.addSourcePair(currentBlock, IntConstant.logicZero);
        builder.insertInst(phi);
        return phi;
    }
}
