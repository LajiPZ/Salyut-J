package frontend.syntax.logical;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.ICompare;
import frontend.llvm.value.instruction.Operator;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class EqExp extends ASTNode {
    public enum EqOperator {
        EQ, NE
    }

    private final RelationExp LRelExp;
    private final ArrayList<EqOperator> operators = new ArrayList<>();
    private final ArrayList<RelationExp> RRelExps = new ArrayList<>();

    public EqExp(RelationExp LRelExp) {
        this.LRelExp = LRelExp;
    }

    public void addRRelExp(EqOperator EqOperator, RelationExp RRelExp) {
        this.operators.add(EqOperator);
        this.RRelExps.add(RRelExp);
    }

    public static EqExp parse(TokenStream ts, List<ErrorEntry> errors) {
        EqExp exp = new EqExp(RelationExp.parse(ts, errors));
        while (ts.check(TokenType.EQ, TokenType.NE)) {
            ts.logParse("<EqExp>");
            Token operator = ts.poll();
            exp.addRRelExp(
                operator.ofType(TokenType.EQ) ? EqOperator.EQ : EqOperator.NE,
                RelationExp.parse(ts,errors)
            );
        }
        ts.logParse("<EqExp>");
        return exp;
    }

    public void visit() {
        LRelExp.visit();
        for (RelationExp exp : RRelExps) {
            exp.visit();
        }
    }

    public Value build(IrBuilder builder) {
        Value val = LRelExp.build(builder);
        val = ValueConverter.toInteger(val);
        for (int i = 0; i < operators.size(); i++) {
            Value right = RRelExps.get(i).build(builder);
            right = ValueConverter.toInteger(right);
            switch (operators.get(i)) {
                case EQ -> val = builder.insertInst(
                    new ICompare(Operator.EQ, val, right)
                );
                case NE -> val = builder.insertInst(
                    new ICompare(Operator.NE, val, right)
                );
            }
        }
        return val;
    }
}
