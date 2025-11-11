package frontend.syntax.expression;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.datatype.DataType;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.ICalc;
import frontend.llvm.value.instruction.Operator;
import frontend.syntax.ASTNode;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.List;

final public class MulExp extends ASTNode {
    public enum MulOperator {
        MUL, DIV, MOD
    }

    private final UnaryExp LUnaryExp;
    private final ArrayList<MulOperator> operators = new ArrayList<>();
    private final ArrayList<UnaryExp> RUnaryExps = new ArrayList<>();

    public MulExp(UnaryExp LUnaryExp) {
        this.LUnaryExp = LUnaryExp;
    }

    public void addRUnaryExp(MulOperator operator, UnaryExp RUnaryExp) {
        operators.add(operator);
        RUnaryExps.add(RUnaryExp);
    }

    public static MulExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        MulExp mulExp = new MulExp(UnaryExp.parse(tokenStream,errors));
        while (tokenStream.check(TokenType.Mul, TokenType.Div, TokenType.Mod)) {
            tokenStream.logParse("<MulExp>");
            Token type = tokenStream.poll();
            MulOperator operator = type.ofType(TokenType.Mul) ? MulOperator.MUL :
                                   type.ofType(TokenType.Div) ? MulOperator.DIV :
                                   MulOperator.MOD;
            mulExp.addRUnaryExp(
                operator,
                UnaryExp.parse(tokenStream, errors)
            );
        }
        tokenStream.logParse("<MulExp>");
        return mulExp;
    }

    public void visit() {
        LUnaryExp.visit();
        for (UnaryExp exp : RUnaryExps) {
            exp.visit();
        }
    }

    public int calc() {
        int sum = LUnaryExp.calc();
        for (int i = 0; i < operators.size(); i++) {
            if (operators.get(i).equals(MulOperator.MUL)) {
                sum *= RUnaryExps.get(i).calc();
            } else if (operators.get(i).equals(MulOperator.DIV)) {
                sum /= RUnaryExps.get(i).calc();
            } else {
                sum %= RUnaryExps.get(i).calc();
            }
        }
        return sum;
    }

    public DataType calcType() {
        return LUnaryExp.calcType();
    }

    public Value build(IrBuilder builder) {
        Value val = LUnaryExp.build(builder);
        for (int i = 0; i < operators.size(); i++) {
            Value operand = RUnaryExps.get(i).build(builder);
            val = ValueConverter.toInteger(val);
            operand = ValueConverter.toInteger(operand);
            switch (operators.get(i)) {
                case MUL -> val = builder.insertInst(
                    new ICalc(Operator.MUL, val, operand)
                );
                case DIV -> val = builder.insertInst(
                    new ICalc(Operator.DIV, val, operand)
                );
                case MOD -> val = builder.insertInst(
                    new ICalc(Operator.MOD, val, operand)
                );
            }
        }
        return val;
    }
}