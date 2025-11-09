package frontend.syntax.logical;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.ICompare;
import frontend.llvm.value.instruction.Operator;
import frontend.syntax.ASTNode;
import frontend.syntax.expression.AddExp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

final public class RelationExp extends ASTNode {
    public enum RelationOperator {
        LT, GT, LE, GE
    }

    private final AddExp LAddExp;
    private final ArrayList<RelationOperator> operators = new ArrayList<>();
    private final ArrayList<AddExp> RAddExps = new ArrayList<>();

    public RelationExp(AddExp LAddExp) {
        this.LAddExp = LAddExp;
    }

    public void addRAddExp(RelationOperator operator, AddExp RAddExp) {
        operators.add(operator);
        RAddExps.add(RAddExp);
    }

    public static RelationExp parse(TokenStream ts, List<ErrorEntry> errors) {
        RelationExp exp = new RelationExp(AddExp.parse(ts, errors));
        while (ts.check(TokenType.LT, TokenType.GT, TokenType.LE, TokenType.GE)) {
            ts.logParse("<RelExp>");
            Token t = ts.poll();
            RelationOperator operator = t.ofType(TokenType.LT) ? RelationOperator.LT :
                                        t.ofType(TokenType.GT) ? RelationOperator.GT :
                                        t.ofType(TokenType.LE) ? RelationOperator.LE :
                                        RelationOperator.GE;
            exp.addRAddExp(operator, AddExp.parse(ts, errors));
        }
        ts.logParse("<RelExp>");
        return exp;
    }

    public void visit() {
        LAddExp.visit();
        for (AddExp exp : RAddExps) {
            exp.visit();
        }
    }

    public Value build(IrBuilder builder) {
        Value val = LAddExp.build(builder);
        val = ValueConverter.toInteger(val);
        for (int i = 0; i < operators.size(); i++) {
            Value right = RAddExps.get(i).build(builder);
            right = ValueConverter.toInteger(right);
            switch (operators.get(i)) {
                case LT -> val = builder.insertInst(
                    new ICompare(Operator.LT, val, right)
                );
                case GT -> val = builder.insertInst(
                    new ICompare(Operator.GT, val, right)
                );
                case LE -> val = builder.insertInst(
                    new ICompare(Operator.LE, val, right)
                );
                case GE -> val = builder.insertInst(
                    new ICompare(Operator.GE, val, right)
                );
            }
        }
        return val;
    }
}
