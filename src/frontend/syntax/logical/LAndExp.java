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

final public class LAndExp extends ASTNode {
    private final ArrayList<EqExp> eqExps = new ArrayList<>();

    public LAndExp() {}

    public void addEqExp(EqExp EqExp) {
        this.eqExps.add(EqExp);
    }

    public static LAndExp parse(TokenStream ts, List<ErrorEntry> errors) {
        LAndExp exp = new LAndExp();
        exp.addEqExp(EqExp.parse(ts, errors));
        while (ts.check(TokenType.And)) {
            ts.logParse("<LAndExp>");
            ts.poll();
            exp.addEqExp(EqExp.parse(ts, errors));
        }
        ts.logParse("<LAndExp>");
        return exp;
    }

    public void visit() {
        for (EqExp eqExp : eqExps) {
            eqExp.visit();
        }
    }

    public Value build(IrBuilder builder) {
        // basically identical to that in LOrExp
        // 通过给每个条件开bblk, 每个blk带跳转，实现短路取值
        // 要跳入的bblk在完全遍历之后才知道，所以要先放Null再填
        if (eqExps.size() == 1) {
            return eqExps.get(0).build(builder);
        }
        List<BBlock> bBlocks = new ArrayList<>();
        BBlock currentBlock = builder.getInsertPoint();
        for (EqExp eqExp : eqExps) {
            Value cond = ValueConverter.toBoolean(eqExp.build(builder));
            currentBlock = builder.getInsertPoint();
            BBlock nextBBlk = builder.newBBlock(false);
            builder.insertInst(
                    currentBlock,
                    new IBranch(
                            cond, nextBBlk, null
                    )
            );
            bBlocks.add(currentBlock);
            currentBlock = nextBBlk;
        }
        // 此时，currentBlk为求值为零对应的路径，其余blk对应都是求值为1的路径
        // 随后，各路径汇入一个bblk,继续后续编译
        // 利用Phi即可求解
        builder.fillBranchTarget(currentBlock, bBlocks, builder);
        IPhi phi = new IPhi(new BooleanType());
        for (BBlock bBlock : bBlocks) {
            phi.addSourcePair(bBlock, IntConstant.logicZero);
        }
        phi.addSourcePair(currentBlock, IntConstant.logicOne);
        builder.insertInst(phi);
        return phi;
    }
}
