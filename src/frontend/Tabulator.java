package frontend;

import frontend.error.ErrorEntry;
import frontend.symbol.*;
import frontend.symbol.datatype.DataType;
import frontend.symbol.datatype.IntType;
import frontend.symbol.datatype.init.InitType;
import frontend.syntax.CompileUnit;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用于“建立符号表”。
 * 准确而言，绑定语法树中的符号关系，为翻译作准备。
 * 按理是跟生成IR一起的，但为了作业要求，将其分开。
 */
public class Tabulator {
    public enum FuncReturnType {
        Void, Int
    }

    private CompileUnit astTop;

    private static int scopeCnt = 1;
    private static Stack<SymbolTable> symbolTables;

    private static HashMap<String, FuncSymbol> funcSymbols;
    private static FuncReturnType expectedReturnType;
    private static FuncReturnType actualReturnType;

    private static int loopDepth = 0; // for break; and continue;

    private static boolean hasReturn = false;

    private static ArrayList<ErrorEntry> errors;
    private static ArrayList<Symbol> allSymbols; // for output

    public Tabulator(CompileUnit astTop) {
        this.astTop = astTop;
        allSymbols = new ArrayList<>();
        errors = new ArrayList<>();
        funcSymbols = new HashMap<>();
        symbolTables = new Stack<>();
        symbolTables.push(new SymbolTable(scopeCnt));
        expectedReturnType = FuncReturnType.Void;
        actualReturnType = FuncReturnType.Void;
    }

    public boolean tabulate() {
        runStartupRoutine();
        this.astTop.visit();
        return errors.isEmpty();
    }

    public static boolean isGlobalDef() {
        return scopeCnt == 1;
    }

    public static void intoLoop() {
        loopDepth++;
    }

    public static void exitLoop() {
        loopDepth--;
    }

    public static boolean inLoop() {
        return loopDepth != 0;
    }

    public static void intoNewScope() {
        scopeCnt++;
        symbolTables.push(new SymbolTable(scopeCnt));
    }

    public static void exitScope() {
        symbolTables.pop();
    }

    public static void setExpectedReturnType(FuncReturnType expectedReturnType) {
        Tabulator.expectedReturnType = expectedReturnType;
        Tabulator.actualReturnType = FuncReturnType.Void;
    }

    public static void setActualReturnType(FuncReturnType actualReturnType) {
        Tabulator.actualReturnType = actualReturnType;
    }

    public static boolean returnTypeMatches() {
        return expectedReturnType == actualReturnType;
    }

    public static void recordError(ErrorEntry e) {
        errors.add(e);
    }

    public static FuncSymbol addFuncSymbol(String ident, DataType type) {
        if (symbolTables.peek().containsSymbol(ident) ||
            funcSymbols.containsKey(ident)
        ) {
            return null;
        } else {
            FuncSymbol funcSymbol = new FuncSymbol(ident, type, symbolTables.peek().getId());
            funcSymbols.put(ident, funcSymbol);
            if (!ident.equals("main"))allSymbols.add(funcSymbol);
            return funcSymbol;
        }
    }

    public static VarSymbol addVarSymbol(String ident, boolean isStatic, DataType dataType) {
        if (symbolTables.peek().containsSymbol(ident) ||
            funcSymbols.containsKey(ident)
        ) {
            return null;
        } else {
            VarSymbol varSymbol = new VarSymbol(ident, isStatic, dataType, symbolTables.peek().getId());
            symbolTables.peek().putSymbol(ident, varSymbol);
            allSymbols.add(varSymbol);
            return varSymbol;
        }
    }

    public static ConstSymbol addConstSymbol(String ident, DataType dataType) {
        if (symbolTables.peek().containsSymbol(ident) ||
            funcSymbols.containsKey(ident)) {
            return null;
        } else {
            ConstSymbol constSymbol = new ConstSymbol(ident, dataType, symbolTables.peek().getId());
            symbolTables.peek().putSymbol(ident, constSymbol);
            allSymbols.add(constSymbol);
            return constSymbol;
        }
    }

    public static ValSymbol getValSymbol(String ident) {
        ListIterator<SymbolTable> iter = symbolTables.listIterator(symbolTables.size());
        while (iter.hasPrevious()) {
            SymbolTable symbolTable = iter.previous();
            if (symbolTable.containsSymbol(ident)) {
                return symbolTable.getSymbol(ident);
            }
        }
        return null;
    }

    public static FuncSymbol getFuncSymbol(String ident) {
        return funcSymbols.get(ident);
    }

    public static VarSymbol addParameter(FuncSymbol funcSymbol, String ident, DataType dataType) {
        VarSymbol symbol = addVarSymbol(ident, false, dataType);
        if (symbol == null) {
            return null;
        } else {
            funcSymbol.addParameter(symbol);
            return symbol;
        }
    }

    private void runStartupRoutine() {
        // 为什么需要这样？因为getint() 本来在文法定义里，现在删掉了！
        FuncSymbol funcSymbol = new FuncSymbol("getint", new IntType(), scopeCnt);
        funcSymbols.put("getint", funcSymbol);
    }

    public List<ErrorEntry> getErrors() {
        return errors;
    }

    public void printTabulationLog(String filePath) throws IOException {
        allSymbols.sort(new SymbolComparator());
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
        writer.write(allSymbols.stream().map(Symbol::toString).collect(Collectors.joining("\n")));
        writer.close();
    }
}
