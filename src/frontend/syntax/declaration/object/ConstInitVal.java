package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class ConstInitVal extends ASTNode {
    public enum Type {
        Single, Multiple
    }
    private Type type;
    private ConstExp singleConstExp;
    private ArrayList<ConstExp> multipleConstExps;

    public ConstInitVal() {
        this.type = Type.Multiple;
        this.singleConstExp = null;
        this.multipleConstExps = new ArrayList<>();
    }

    public ConstInitVal(ConstExp singleConstExp) {
        this.type = Type.Single;
        this.singleConstExp = singleConstExp;
        this.multipleConstExps = null;
    }

    public void addConstExp(ConstExp constExp) {
        assert(this.type == Type.Multiple);
        this.multipleConstExps.add(constExp);
    }

    public static ConstInitVal parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        ConstInitVal initVal;
        if (tokenStream.checkPoll(TokenType.LeftBrace)) {
            initVal = new ConstInitVal();
            if (!tokenStream.check(TokenType.RightBrace)) {
                do {
                    initVal.addConstExp(
                        ConstExp.parse(tokenStream, errors)
                    );
                } while (tokenStream.checkPoll(TokenType.Comma));
            }
            tokenStream.next(TokenType.RightBrace);
        } else {
            initVal = new ConstInitVal(
                ConstExp.parse(tokenStream, errors)
            );
        }
        tokenStream.logParse("<ConstInitVal>");
        return initVal;
    }

    public void visit() {
        if (type == Type.Single) {
            singleConstExp.visit();
        } else {
            multipleConstExps.forEach(ConstExp::visit);
        }
    }

    public int singleCalc() {
        return singleConstExp.calc();
    }
}
