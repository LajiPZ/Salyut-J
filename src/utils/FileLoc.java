package utils;

public class FileLoc {
    private Pair<Integer, Integer> start;
    private Pair<Integer, Integer> end;

    public FileLoc(Pair<Integer, Integer> start, Pair<Integer, Integer> end) {
        this.start = start;
        this.end = end;
    }

    public int getStartLine() {
        return start.getValue1();
    }
}
