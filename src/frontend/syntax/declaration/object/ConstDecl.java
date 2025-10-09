package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.TokenStream;

import java.util.ArrayList;
import java.util.List;

public class ConstDecl extends ASTNode {
    private BType type;
    private ConstDef lConstDef;
    private ArrayList<ConstDef> rConstDefs = new ArrayList<>();

    public ConstDecl(BType type, ConstDef lConstDef) {
        this.type = type;
        this.lConstDef = lConstDef;
    }

    public void addRConstDef(ConstDef rConstDef) {
        rConstDefs.add(rConstDef);
    }

    public static ConstDecl parse(TokenStream tokenStream, List<ErrorEntry> errors) {

        return null;
    }
}
