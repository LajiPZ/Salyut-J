package frontend.syntax.expression;

import frontend.IrBuilder;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.datatype.DataType;
import frontend.datatype.IntType;
import frontend.llvm.value.Value;
import frontend.syntax.ASTNode;
import frontend.syntax.misc.LVal;
import frontend.syntax.misc.Number;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.List;

public class PrimaryExp extends ASTNode {
    public enum Type {
        Exp, LVal, Number
    }

    private final Type type;
    private final Object value;

    public PrimaryExp(Exp exp) {
        this.type = Type.Exp;
        this.value = exp;
    }

    public PrimaryExp(LVal lval) {
        this.type = Type.LVal;
        this.value = lval;
    }

    public PrimaryExp(Number number) {
        this.type = Type.Number;
        this.value = number;
    }

    public static PrimaryExp parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        PrimaryExp retExp;
        if (tokenStream.checkPoll(TokenType.LeftParen)) {
            Exp exp = Exp.parse(tokenStream, errors);
            if (!tokenStream.checkPoll(TokenType.RightParen)) {
                errors.add(
                    new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
                );
            }
            retExp = new PrimaryExp(exp);
        } else if (tokenStream.check(TokenType.IntConst)) {
            retExp = new PrimaryExp(Number.parse(tokenStream, errors));
        } else {
            retExp = new PrimaryExp(LVal.parse(tokenStream, errors));
        }
        tokenStream.logParse("<PrimaryExp>");
        return retExp;
    }

    public void visit() {
        switch (type) {
            case Exp -> {
                ((Exp) value).visit();
            }
            case LVal -> {
                ((LVal) value).visit(false);
            }
            case Number -> {}
        }
    }

    public int calc() {
        switch (type) {
            case Exp -> {
                return ((Exp)value).calc();
            }
            case LVal -> {
                return ((LVal)value).calc();
            }
            case Number -> {
                return ((Number) value).getValue();
            }
        }
        return 0;
    }

    public DataType calcType() {
        switch (type) {
            case Exp -> {
                return ((Exp)value).calcType();
            }
            case LVal -> {
                return ((LVal)value).calcType();
            }
            case Number -> {
                return new IntType();
            }
        }
        return null;
    }

    public Value build(IrBuilder builder) {
        // todo
    }
}
