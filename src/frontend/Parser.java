package frontend;

import frontend.error.ErrorEntry;
import frontend.syntax.CompileUnit;
import frontend.token.TokenStream;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

    public CompileUnit getCompileUnit() {
        return compileUnit;
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }
}
