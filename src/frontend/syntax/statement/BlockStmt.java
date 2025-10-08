package frontend.syntax.statement;

import frontend.syntax.block.Block;

public class BlockStmt extends Stmt {
    private Block block;

    public BlockStmt(Block block) {
        super(Type.Block);
        this.block = block;
    }
}
