package server;

import java.util.HashMap;
import java.util.Map;

public class ObjectRegistry {

    private static final Map<Class<?>, Object> INSTANCE_REGISTRY = new HashMap<>();

    public static void reg(Class<?> clazz, Object instance) {
        INSTANCE_REGISTRY.put(clazz, instance);
    }

    public static <T> T getInstance(Class<T> clazz) {
        return (T) INSTANCE_REGISTRY.get(clazz);
    }
}

