package frontend.syntax.block;

import frontend.error.ErrorEntry;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class Block {
    private ArrayList<BlockItem> items;

    public Block() {
        this.items = new ArrayList<>();
    }

    public void addItem(BlockItem item) {
        this.items.add(item);
    }

    public static Block parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        tokenStream.next(TokenType.LeftBrace);
        Block block = new Block();
        while (!tokenStream.check(TokenType.RightBrace)) {
            block.addItem(
                BlockItem.parse(tokenStream, errors)
            );
        }
        tokenStream.next(TokenType.RightBrace);
        tokenStream.logParse("<Block>");
        return block;
    }
}
