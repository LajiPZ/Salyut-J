package frontend.syntax.declaration.function;

import frontend.error.ErrorEntry;
import frontend.symbol.FuncSymbol;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

public class FuncFParams extends ASTNode {
    private ArrayList<FuncFParam> funcFParams = new ArrayList<>();

    public FuncFParams() {}

    public void addFuncFParam(FuncFParam funcFParam) {
        funcFParams.add(funcFParam);
    }

    public static FuncFParams parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        FuncFParams funcFParams = new FuncFParams();
        do {
            funcFParams.addFuncFParam(FuncFParam.parse(tokenStream, errors));
        } while(tokenStream.checkPoll(TokenType.Comma));
        tokenStream.logParse("<FuncFParams>");
        return funcFParams;
    }

    public void visit(FuncSymbol funcSymbol) {
        for (FuncFParam funcFParam : funcFParams) {
            funcFParam.visit(funcSymbol);
        }
    }
}
