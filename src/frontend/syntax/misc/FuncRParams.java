package frontend.syntax.misc;

import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.symbol.FuncSymbol;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class FuncRParams extends ASTNode {
    private ArrayList<Exp> exps = new ArrayList<>();

    public FuncRParams(Exp exp) { this.exps.add(exp); }

    public void addExp (Exp exp) {
        exps.add(exp);
    }

    public static FuncRParams parse(TokenStream ts, List<ErrorEntry> errors) {
        FuncRParams retValue =  new FuncRParams(Exp.parse(ts, errors));
        while (ts.checkPoll(TokenType.Comma)) {
            retValue.addExp(Exp.parse(ts, errors));
        }
        ts.logParse("<FuncRParams>");
        return retValue;
    }

    public int getParameterCount() { return exps.size(); }

    public void visit() {
        for (Exp exp : exps) {
            exp.visit();
        }

    }
}
