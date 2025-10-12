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
        // int main()
        while (!tokenStream.isEnd()) {
            if (tokenStream.check(1,TokenType.Main)) {
                compileUnit.setMainFuncDef(MainFuncDef.parse(tokenStream, errors));
            } else if (tokenStream.check(2,TokenType.LeftParen)) {
                compileUnit.addFuncDef(FuncDef.parse(tokenStream, errors));
            } else {
                compileUnit.addDecl(Decl.parse(tokenStream, errors));
            }
        }
        tokenStream.logParse("<CompUnit>");
        return compileUnit;
    }
}
