package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class VarDecl extends ASTNode {
    private boolean isStatic;
    private BType type;
    private ArrayList<VarDef> varDefs = new ArrayList<>();

    public VarDecl(boolean isStatic, BType type) {
        this.isStatic = isStatic;
        this.type = type;
    }

    public void addVarDef(VarDef varDef) {
        varDefs.add(varDef);
    }

    public static VarDecl parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        boolean isStatic = tokenStream.checkPoll(TokenType.Static);
        BType type = BType.parse(tokenStream, errors);
        VarDecl varDecl = new VarDecl(isStatic, type);
        do {
            varDecl.addVarDef(VarDef.parse(tokenStream, errors));
        } while (tokenStream.checkPoll(TokenType.Comma));
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
               new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        tokenStream.logParse("<VarDecl>");
        return varDecl;
    }
}
