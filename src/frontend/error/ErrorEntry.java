package frontend.error;

import utils.FileLoc;

public class ErrorEntry {
    private ErrorType type;
    private String content;
    private FileLoc fileLoc;

    public ErrorEntry(ErrorType type, String content, FileLoc fileLoc) {
        this.type = type;
        this.content = content;
        this.fileLoc = fileLoc;
    }

    public int getErrorLine() {
        return fileLoc.getStartLine();
    }

    public ErrorType getType() {
        return type;
    }

    @Override
    public String toString() {
        char typeCode = (char) ('a' + type.ordinal());
        return (getErrorLine() + " " + typeCode + "\n");
    }
}

