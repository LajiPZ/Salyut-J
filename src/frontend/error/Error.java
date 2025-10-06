package frontend.error;

import utils.FileLoc;

public class Error {
    private ErrorType type;
    private String content;
    private FileLoc fileLoc;

    public Error(ErrorType type, String content, FileLoc fileLoc) {
        this.type = type;
        this.content = content;
        this.fileLoc = fileLoc;
    }

    @Override
    public String toString() {
        char typeCode = (char) ('a' + type.ordinal());
        return (fileLoc.getStartLine() + " " + typeCode);
    }
}

