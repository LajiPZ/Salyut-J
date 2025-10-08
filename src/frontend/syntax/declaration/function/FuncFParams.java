package frontend.syntax.declaration.function;

import frontend.syntax.ASTNode;

import java.util.ArrayList;

public class FuncFParams extends ASTNode {
    private FuncFParam lFuncFParam;
    private ArrayList<FuncFParam> rFuncFParams = new ArrayList<>();

    public FuncFParams(FuncFParam lFuncFParam) {
        this.lFuncFParam = lFuncFParam;
    }

    public void addRFuncFParam(FuncFParam rFuncFParam) {
        rFuncFParams.add(rFuncFParam);
    }
}
