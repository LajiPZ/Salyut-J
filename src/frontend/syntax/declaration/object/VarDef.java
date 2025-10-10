package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class VarDef extends ASTNode {
    private Token ident;
    private ConstExp indexExp = null;
    private InitVal initVal = null;

    public VarDef(Token ident) {
        this.ident = ident;
        this.indexExp = null;
        this.initVal = null;
    }

    public void setIndexExp(ConstExp indexExp) {
        this.indexExp = indexExp;
    }

    public void setInitVal(InitVal initVal) {
        this.initVal = initVal;
    }

    public static VarDef parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token ident = tokenStream.next(TokenType.Ident);
        VarDef varDef = new VarDef(ident);
        if (tokenStream.checkPoll(TokenType.LeftBracket)) {
            ConstExp exp = ConstExp.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.RightBracket)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRBracket, "]", tokenStream.peek(-1).getFileLoc())
                );
            }
            varDef.setIndexExp(exp);
        }
        if (tokenStream.checkPoll(TokenType.Assign)) {
            varDef.setInitVal(
                InitVal.parse(tokenStream, errors)
            );
        }
        return varDef;
    }
}
