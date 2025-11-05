package utils;

public class Counter {
    private int counter;

    public Counter() {
        counter = 0;
    }

    public int get() {
        return counter++;
    }

    public int reset() {
        int prev = counter;
        counter = 0;
        return prev;
    }

    public void set(int value) {
        counter = value;
    }
}
