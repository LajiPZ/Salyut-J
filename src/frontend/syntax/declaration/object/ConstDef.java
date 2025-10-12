package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ConstDef extends ASTNode {
    private Token ident;
    private ConstExp indexExp = null;
    private ConstInitVal initVal = null;

    public ConstDef(Token ident) {
        this.ident = ident;
    }

    public void setInitVal(ConstInitVal initVal) {
        this.initVal = initVal;
    }

    public void setIndexExp(ConstExp indexExp) {
        this.indexExp = indexExp;
    }

    public static ConstDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token ident = tokenStream.next(TokenType.Ident);
        ConstDef def = new ConstDef(ident);
        if (tokenStream.checkPoll(TokenType.LeftBracket)) {
            def.setIndexExp(ConstExp.parse(tokenStream, errors));
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.getPrevToken().getFileLoc())
                );
            }
        }
        tokenStream.next(TokenType.Assign);
        def.setInitVal(ConstInitVal.parse(tokenStream, errors));
        tokenStream.logParse("<ConstDef>");
        return def;
    }
}
