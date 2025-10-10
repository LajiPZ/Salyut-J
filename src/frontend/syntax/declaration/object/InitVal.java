package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class InitVal extends ASTNode {
    public enum Type {
        Single, Multiple
    }
    private Type type;
    private Exp singleExp;
    private ArrayList<Exp> multipleExps;

    public InitVal() {
        this.type = Type.Multiple;
        this.singleExp = null;
        this.multipleExps = new ArrayList<>();
    }

    public InitVal(Exp singleExp) {
        this.type = Type.Single;
        this.singleExp = singleExp;
        this.multipleExps = null;
    }

    public void addMultipleExp(Exp multipleExp) {
        assert(this.type == Type.Multiple);
        this.multipleExps.add(multipleExp);
    }

    public static InitVal parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        InitVal initVal;
        if (tokenStream.checkPoll(TokenType.LeftBrace)) {
            initVal = new InitVal();
            if (!tokenStream.check(TokenType.RightBrace)) {
                do {
                    initVal.addMultipleExp(
                        Exp.parse(tokenStream, errors)
                    );
                } while (tokenStream.checkPoll(TokenType.Comma));
            }
            tokenStream.next(TokenType.RightBrace);
        } else {
            initVal = new InitVal(
                Exp.parse(tokenStream, errors)
            );
        }
        return initVal;
    }
}
