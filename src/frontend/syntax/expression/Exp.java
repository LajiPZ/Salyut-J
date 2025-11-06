package frontend.syntax.expression;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.datatype.DataType;
import frontend.llvm.value.Value;
import frontend.syntax.ASTNode;
import frontend.token.TokenStream;

import java.util.List;

final public class Exp extends ASTNode {
    private final AddExp addExp;

    public Exp(AddExp addExp) {
        this.addExp = addExp;
    }

    public AddExp getAddExp() {
        return this.addExp;
    }

    public static Exp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Exp exp = new Exp(AddExp.parse(tokenStream, errors));
        tokenStream.logParse("<Exp>");
        return exp;
    }

    public void visit() {
        addExp.visit();
    }

    public int calc() {
        return addExp.calc();
    }

    public DataType calcType() {
        return addExp.calcType();
    }

    public Value build(IrBuilder builder) {
        return addExp.build(builder);
    }
}
