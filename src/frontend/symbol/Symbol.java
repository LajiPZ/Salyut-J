package frontend.symbol;

public class Symbol {
    private String ident;
    private int scopeCnt;

    public Symbol(String ident, int scopeCnt) {
        this.ident = ident;
        this.scopeCnt = scopeCnt;
    }

    public String getIdent() {
        return ident;
    }

    public int getScopeCnt() {
        return scopeCnt;
    }

}
