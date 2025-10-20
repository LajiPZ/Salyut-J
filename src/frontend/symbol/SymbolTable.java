package frontend.symbol;

import java.util.HashMap;

public class SymbolTable {
    private int id;
    private HashMap<String, ValSymbol> valSymbols;

    public SymbolTable(int id) {
        this.id = id;
        this.valSymbols = new HashMap<>();
    }

    public boolean containsSymbol(String symbol) {
        return valSymbols.containsKey(symbol);
    }

    public void putSymbol(String ident, ValSymbol valSymbol) {
        valSymbols.put(ident, valSymbol);
    }

    public ValSymbol getSymbol(String ident) {
        return valSymbols.get(ident);
    }
}
