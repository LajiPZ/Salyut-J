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
    private ArrayList<ConstInitVal> subInitVals;

    public ConstInitVal() {
        this.type = Type.Multiple;
        this.singleConstExp = null;
        this.subInitVals = new ArrayList<>();
    }

    public ConstInitVal(ConstExp singleConstExp) {
        this.type = Type.Single;
        this.singleConstExp = singleConstExp;
        this.subInitVals = null;
    }

    public void addSubInitVal(ConstInitVal constInitVal) {
        assert(this.type == Type.Multiple);
        this.subInitVals.add(constInitVal);
    }

    public static ConstInitVal parse(TokenStream tokenStream, List<ErrorEntry> errors, boolean isSub) {
        ConstInitVal initVal;
        if (tokenStream.checkPoll(TokenType.LeftBrace)) {
            initVal = new ConstInitVal();
            if (!tokenStream.check(TokenType.RightBrace)) {
                do {
                    initVal.addSubInitVal(
                        ConstInitVal.parse(tokenStream, errors, true)
                    );
                } while (tokenStream.checkPoll(TokenType.Comma));
            }
            tokenStream.next(TokenType.RightBrace);
        } else {
            initVal = new ConstInitVal(
                ConstExp.parse(tokenStream, errors)
            );
        }
        if (!isSub) tokenStream.logParse("<ConstInitVal>");
        return initVal;
    }

    public void visit() {
        if (type == Type.Single) {
            singleConstExp.visit();
        } else {
            subInitVals.forEach(ConstInitVal::visit);
        }
    }

    public int singleCalc() {
        return singleConstExp.calc();
    }

    public ConstExp getSingleConstExp() {
        return singleConstExp;
    }

    public List<ConstInitVal> getSubInitVals() {
        return subInitVals;
    }

    public Type getType() {
        return type;
    }
}
