package frontend.syntax.declaration.object;

import frontend.error.ErrorEntry;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.ConstExp;
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
    private ArrayList<InitVal> subInitVals;

    public InitVal() {
        this.type = Type.Multiple;
        this.singleExp = null;
        this.subInitVals = new ArrayList<>();
    }

    public InitVal(Exp singleExp) {
        this.type = Type.Single;
        this.singleExp = singleExp;
        this.subInitVals = null;
    }

    public void addSubInitVal(InitVal subInitVal) {
        assert(this.type == Type.Multiple);
        this.subInitVals.add(subInitVal);
    }

    public static InitVal parse(TokenStream tokenStream, List<ErrorEntry> errors, boolean isSub) {
        InitVal initVal;
        if (tokenStream.checkPoll(TokenType.LeftBrace)) {
            initVal = new InitVal();
            if (!tokenStream.check(TokenType.RightBrace)) {
                do {
                    initVal.addSubInitVal(
                        InitVal.parse(tokenStream, errors, true)
                    );
                } while (tokenStream.checkPoll(TokenType.Comma));
            }
            tokenStream.next(TokenType.RightBrace);
        } else {
            initVal = new InitVal(
                Exp.parse(tokenStream, errors)
            );
        }
        if (!isSub) tokenStream.logParse("<InitVal>");
        return initVal;
    }

    public void visit() {
        if (type == Type.Single) {
            singleExp.visit();
        } else {
            subInitVals.forEach(InitVal::visit);
        }
    }

    public Exp getSingleExp() {
        return singleExp;
    }

    public ConstInitVal convert() {
        if (type == Type.Single) {
            return new ConstInitVal(new ConstExp(singleExp.getAddExp()));
        } else {
            ConstInitVal constInitVal = new ConstInitVal();
            for (InitVal initVal : subInitVals) {
                constInitVal.addSubInitVal(initVal.convert());
            }
            return constInitVal;
        }
    }

    public Type getType() {
        return type;
    }

    public List<InitVal> getSubInitVals() {
        return subInitVals;
    }
}
