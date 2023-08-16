package yan.lx.bedrockminer.model;

public class TickInfo {
    private final int maxValue;
    private int value;

    public TickInfo(int maxValue) {
        this.maxValue = maxValue;
    }

    public TickInfo() {
        this(Integer.MAX_VALUE);
    }

    public void addOne() {
        ++value;
    }

    public void reset() {
        value = 0;
    }

    public boolean isAllow() {
        return value < maxValue;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
