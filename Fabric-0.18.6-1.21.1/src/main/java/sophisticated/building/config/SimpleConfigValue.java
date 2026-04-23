package sophisticated.building.config;

public class SimpleConfigValue<T> {
    private T value;

    public SimpleConfigValue(T defaultValue) {
        this.value = defaultValue;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }
}
