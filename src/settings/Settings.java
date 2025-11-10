package settings;

final public class Settings {
    public static class FilePath {
        public static String src = "testfile.txt";
        public static String lexerOut = "lexer.txt";
        public static String parserOut = "parser.txt";
        public static String tabulatorOut = "symbol.txt";
        public static String errOut = "error.txt";
        public static String IROut = "llvm_ir.txt";
    }

    public static class PrintConfig {
        public static boolean printTokenStream = false;
        public static boolean printParseProcess = false;
        public static boolean printTabulation = false;
        public static boolean printError = false;
        public static boolean printIR = true;
    }
}
