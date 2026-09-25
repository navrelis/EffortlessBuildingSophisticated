package sophisticated.building.config;

/**
 * A number option with the range it was defined with ({@link IConfigBuilder#defineInRange}).
 */
public interface NumberConfigValue<T extends Number> extends ConfigValue<T> {

    T getMin();

    T getMax();
}
