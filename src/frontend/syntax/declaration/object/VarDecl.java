package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.TokenStream;

import java.util.ArrayList;
import java.util.List;

public class VarDecl extends ASTNode {
    private boolean isStatic;
    private BType type;
    private VarDef lVarDef;
    private ArrayList<VarDef> rVarDefs = new ArrayList<>();

    public VarDecl(boolean isStatic, BType type, VarDef lVarDef) {
        this.isStatic = isStatic;
        this.type = type;
        this.lVarDef = lVarDef;
    }

    public void addRVarDef(VarDef rVarDef) {
        rVarDefs.add(rVarDef);
    }

    public static VarDecl parse(TokenStream tokenStream, List<ErrorEntry> errors) {


    }
}
