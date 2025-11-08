package frontend.syntax.statement;

import frontend.IrBuilder;
import frontend.Tabulator;
import frontend.error.ErrorEntry;
import frontend.error.ErrorType;
import frontend.llvm.tools.ValueConverter;
import frontend.llvm.value.Value;
import frontend.llvm.value.constant.IntConstant;
import frontend.llvm.value.instruction.ICall;
import frontend.syntax.expression.Exp;
import frontend.token.Token;
import frontend.token.TokenStream;
import frontend.token.TokenType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class PrintfStmt extends Stmt {
    private Token label;
    private String formatString;
    private ArrayList<Exp> args = new ArrayList<>();

    public PrintfStmt(Token label, String formatString) {
        super(Type.Printf);
        this.label = label;
        this.formatString = formatString;
    }

    public void addArgument(Exp exp) {
        args.add(exp);
    }

    public static PrintfStmt parse(TokenStream tokenStream, List<ErrorEntry> errors) {
        Token token = tokenStream.poll();
        tokenStream.next(TokenType.LeftParen);
        Token str = tokenStream.next(TokenType.StringConst);
        PrintfStmt retStmt = new PrintfStmt(token, str.getValue());
        while (tokenStream.checkPoll(TokenType.Comma)) {
            retStmt.addArgument(Exp.parse(tokenStream, errors));
        }
        if (!tokenStream.checkPoll(TokenType.RightParen)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingRParen, ")", tokenStream.getPrevToken().getFileLoc())
            );
        }
        if (!tokenStream.checkPoll(TokenType.Semicolon)) {
            errors.add(
                new ErrorEntry(ErrorType.MissingSemicolon, ";", tokenStream.getPrevToken().getFileLoc())
            );
        }
        return retStmt;
    }

    @Override
    public void visit() {
        int cnt = 0;
        for (int i = 0; i < formatString.length(); i++) {
            if (formatString.charAt(i) == '%' &&
                i + 1 < formatString.length() &&
                formatString.charAt(i + 1) == 'd') {
                cnt++;
            }
        }
        if (cnt != args.size()) {
            Tabulator.recordError(
                new ErrorEntry(ErrorType.FormatCharMismatch, label.getFileLoc())
            );
        }
        for (Exp arg : args) {
            arg.visit();
        }
    }

    @Override
    public void build(IrBuilder builder) {
        List<Value> argValues = args.stream().map(exp -> exp.build(builder)).toList();
        int index = 0;
        for (int i = 0; i < formatString.length(); i++) {
            if (
                formatString.charAt(i) == '%' &&
                i < formatString.length() - 1 &&
                formatString.charAt(i + 1) == 'd'
            ) {
                builder.insertInst(
                    new ICall(
                        builder.getFunction("putint"),
                        List.of(
                            ValueConverter.toInteger(
                                argValues.get(index++)
                            )
                        )
                    )
                );
                i++;
            } else {
                builder.insertInst(
                    new ICall(
                        builder.getFunction("putch"),
                        List.of(
                            new IntConstant(formatString.charAt(i))
                        )
                    )
                );
            }
        }
    }
}
