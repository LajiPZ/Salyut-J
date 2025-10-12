package settings;

final public class Settings {
    public static class FilePath {
        public static String src = "testfile.txt";
        public static String lexerOut = "lexer.txt";
        public static String parserOut = "parser.txt";
        public static String errOut = "error.txt";
    }

    public static class PrintConfig {
        public static boolean printTokenStream = false;
        public static boolean printParseProcess = true;
        public static boolean printError = true;
    }
}
