package frontend.syntax.misc;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.llvm.value.Value;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.Exp;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Exp> getParameters() { return exps; }

    public void visit() {
        for (Exp exp : exps) {
            exp.visit();
        }
    }

    public List<Value> build(IrBuilder builder) {
        return exps.stream().map(exp -> exp.build(builder)).collect(Collectors.toList());
    }
}
