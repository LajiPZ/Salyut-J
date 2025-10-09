package frontend.syntax;

import frontend.error.ErrorEntry;
import frontend.syntax.declaration.function.FuncDef;
import frontend.syntax.declaration.function.MainFuncDef;
import frontend.syntax.declaration.object.Decl;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class CompileUnit extends ASTNode {
    private ArrayList<Decl> decls = new ArrayList<>();
    private ArrayList<FuncDef> funcDefs = new ArrayList<>();
    private MainFuncDef mainFuncDef = null;

    public CompileUnit() {}

    public void setMainFuncDef(MainFuncDef mainFuncDef) {
        this.mainFuncDef = mainFuncDef;
    }

    public void addDecl(Decl decl) {
        decls.add(decl);
    }

    public void addFuncDef(FuncDef funcDef) {
        funcDefs.add(funcDef);
    }


    public static CompileUnit parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        CompileUnit compileUnit = new CompileUnit();
        while (!tokenStream.isEnd()) {
            if (tokenStream.peek(1).ofType(TokenType.Main)) {
                compileUnit.setMainFuncDef(MainFuncDef.parse(tokenStream, errors));
            } else if (tokenStream.peek(2).ofType(TokenType.LeftParen)) {
                compileUnit.addFuncDef(FuncDef.parse(tokenStream, errors));
            } else {
                compileUnit.addDecl(Decl.parse(tokenStream, errors));
            }
        }
        return compileUnit;
    }
}
