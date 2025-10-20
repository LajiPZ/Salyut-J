package frontend.symbol;

import java.util.ArrayList;
import java.util.List;

public class FuncSymbol extends Symbol {
    public enum Type {
        Void, Int
    }

    private Type type;
    private ArrayList<ValSymbol> parameters;

    public FuncSymbol(String ident, Type type) {
        super(ident);
        this.type = type;
        this.parameters = new ArrayList<>();
    }

    public void addParameter(ValSymbol val) {
        this.parameters.add(val);
    }

    public int getParameterCount() {
        return parameters.size();
    }

    public List<ValSymbol> getParameters() {
        return parameters;
    }
}
