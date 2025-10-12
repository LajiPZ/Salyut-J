package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.declaration.BType;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ConstDecl extends ASTNode {
    private BType type;
    private ArrayList<ConstDef> constDefs = new ArrayList<>();

    public ConstDecl(BType type) {
        this.type = type;
    }

    public void addConstDef(ConstDef constDef) {
        constDefs.add(constDef);
    }

    public static ConstDecl parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        tokenStream.next(TokenType.Const);
        BType type = BType.parse(tokenStream, errors);
        ConstDecl decl = new ConstDecl(type);
        do {
            decl.addConstDef(ConstDef.parse(tokenStream, errors));
        } while (tokenStream.checkPoll(TokenType.Comma));
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon,";",tokenStream.peek(-1).getFileLoc())
            );
        }
        tokenStream.logParse("<ConstDecl>");
        return decl;
    }
}
