package frontend;

import frontend.error.ErrorEntry;
import frontend.syntax.CompileUnit;
import frontend.token.TokenStream;

import java.util.ArrayList;

final public class Parser {
    private TokenStream tokenStream;
    private ArrayList<ErrorEntry> errors;
    private CompileUnit compileUnit; // Parser内记录分析完成后的顶层单元

    public Parser(TokenStream tokenStream) {
        this.tokenStream = tokenStream;
        this.errors = new ArrayList<>();
        this.compileUnit = new CompileUnit();
    }

    public boolean parse() {
        this.compileUnit = CompileUnit.parse(tokenStream, errors);
        return errors.isEmpty();
    }
}
