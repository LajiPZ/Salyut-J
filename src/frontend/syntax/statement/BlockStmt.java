package frontend.syntax.statement;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.syntax.block.Block;
import frontend.token.TokenStream;

import java.util.List;

public class BlockStmt extends Stmt {
    private Block block;

    public BlockStmt(Block block) {
        super(Type.Block);
        this.block = block;
    }

    public static BlockStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        return new BlockStmt(Block.parse(tokenStream, errors));
    }

    @Override
    public void visit() {
        Tabulator.intoNewScope();
        block.visit();
        Tabulator.exitScope();
    }
}
