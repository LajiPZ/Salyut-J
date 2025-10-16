package frontend;

import frontend.error.ErrorEntry;
import frontend.symbol.FuncSymbol;
import frontend.symbol.SymbolTable;
import frontend.syntax.CompileUnit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

/**
 * 用于建立符号表。
 */
public class Tabulator {
    private CompileUnit astTop;
    private HashMap<String, FuncSymbol> funcSymbols;

    private static int scopeCnt = 1;
    private Stack<SymbolTable> symbolTables;

    private static int loopDepth = 0; // for break; and continue;

    private ArrayList<ErrorEntry> errors;

    public boolean tabulate() {

    }
}
