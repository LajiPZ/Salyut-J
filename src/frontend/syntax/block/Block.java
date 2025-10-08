package frontend.syntax.block;

import java.util.ArrayList;

public class Block {
    private ArrayList<BlockItem> items;

    public Block() {
        this.items = new ArrayList<>();
    }

    public void addItem(BlockItem item) {
        this.items.add(item);
    }
}
