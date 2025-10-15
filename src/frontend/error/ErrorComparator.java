package frontend.error;

import java.util.Comparator;

public class ErrorComparator implements Comparator<ErrorEntry> {
    @Override
    public int compare(ErrorEntry o1, ErrorEntry o2) {
        int line1 = o1.getErrorLine();
        int line2 = o2.getErrorLine();
        if (line1 < line2) {
            return -1;
        }
        if (line1 > line2) {
            return 1;
        }
        return (o1.getType().ordinal() - o2.getType().ordinal());
    }
}
