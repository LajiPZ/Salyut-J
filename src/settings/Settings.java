package settings;

final public class Settings {
    public static class FilePath {
        public static String src = "testfile.txt";
        public static String lexerOut = "lexer.txt";
        public static String parserOut = "parser.txt";
        public static String tabulatorOut = "symbol.txt";
        public static String errOut = "error.txt";
        public static String IROut = "llvm_ir.txt";
        public static String MIPSOut = "mips.txt";
        // Debug
        public static String MIPSBeforePRegAlloc = "mipsBeforePRegAlloc.txt";
    }

    public static class PrintConfig {
        public static boolean printTokenStream = false;
        public static boolean printParseProcess = false;
        public static boolean printTabulation = false;
        public static boolean printError = true;
        public static boolean printIR = true;
        public static boolean printMIPS = true;
    }

    public static class DebugConfig {
        public static boolean printMIPSBeforePRegAlloc = true;
    }

    public static class OptimizeConfig {
        public static boolean enableOptimization = true;
        public static boolean allowGlobalVarInCP1 = true;
        public static boolean allowCallSaveToCP1 = false; // This is problematic, only use it when you need extra performance；e.g. recursion
        public static int maxLoopExtendIterations = 100;
        public static int oscillateIterations = 7; // This may lead to Xtra-long processing time, consider setting it to 0
        public static boolean ignoreGlobalArrayCheck = false; // This is problematic as well, when the globalArray is written, it may cause problems
        public static boolean useConservativeRemovePhi = true;
    }
}
