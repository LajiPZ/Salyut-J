package frontend.syntax;

import frontend.syntax.declaration.function.FuncDef;
import frontend.syntax.declaration.function.MainFuncDef;
import frontend.syntax.declaration.object.Decl;

import java.util.ArrayList;

public class CompileUnit extends ASTNode {
    private ArrayList<Decl> decls = new ArrayList<>();
    private ArrayList<FuncDef> funcDefs = new ArrayList<>();
    private MainFuncDef mainFuncDef;

    public CompileUnit(MainFuncDef mainFuncDef) {
        this.mainFuncDef = mainFuncDef;
    }

    public void addDecl(Decl decl) {
        decls.add(decl);
    }

    public void addFuncDef(FuncDef funcDef) {
        funcDefs.add(funcDef);
    }

}
