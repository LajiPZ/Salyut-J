package frontend.syntax.expression;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.datatype.VoidType;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.value.Value;
import frontend.llvm.value.instruction.ICalc;
import frontend.llvm.value.instruction.ICall;
import frontend.symbol.FuncSymbol;
import frontend.datatype.DataType;
import frontend.syntax.misc.FuncRParams;
import frontend.token.Token;

import java.util.stream.IntStream;

public class UnaryCallExp extends UnaryExp {
    private Token ident;
    private FuncRParams params = null;
    private FuncSymbol funcSymbol = null;

    public UnaryCallExp(Token ident) {
        super(Type.Call);
        this.ident = ident;
    }

    public void setFuncRParams(FuncRParams params) { this.params = params; }

    public void visit() {
        FuncSymbol symbol = Tabulator.getFuncSymbol(ident.getValue());
        if (symbol == null) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.UndefinedName, ident.getFileLoc())
            );
        } else {
            funcSymbol = symbol;
            if (params != null) params.visit();
            int paramsCount = (params == null) ? 0 : params.getParameterCount();
            if (paramsCount != funcSymbol.getParameterCount()) {
                Tabulator.recordError(
                    new ErrorEntry(ErrorType.MissingArgument, ident.getFileLoc())
                );
            } else {
                if (IntStream.range(0, paramsCount).anyMatch(
                    i ->
                    !funcSymbol.getParameters().get(i).getDataType().compatibleWith(
                        params.getParameters().get(i).calcType()
                    )
                )) {
                    Tabulator.recordError(
                        new ErrorEntry(ErrorType.ArgumentTypeMismatch, ident.getFileLoc())
                    );
                }
            }
        }
    }

    public int calc() {
        throw new RuntimeException("Calc() of UnaryCall not implemented yet");
    }

    public DataType calcType() {
        return funcSymbol.getType();
    }

    @Override
    public Value build(IrBuilder builder) {
        Value ret = builder.insertInst(
            new ICall(
                builder.getFunction(ident.getValue()),
                params.build(builder)
            )
        );
        return funcSymbol.getType() instanceof VoidType ? null : ret;
    }
}
