package frontend.symbol;

import java.util.Comparator;

public class SymbolComparator implements Comparator<Symbol> {

    @Override
    public int compare(Symbol o1, Symbol o2) {
        return o1.getScopeCnt() - o2.getScopeCnt();
    }
}
