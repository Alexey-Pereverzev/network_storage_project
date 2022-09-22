package client;

import java.util.HashMap;
import java.util.Map;

public class ObjectRegistry {

    private static final Map<Class<?>, Object> INSTANCE_REGISTRY = new HashMap<>();

    static {
        NettyClient nettyClient = new NettyClient();        //  нетти клиент регистрируем здесь, остальные объекты в их классах
        reg(NettyClient.class, nettyClient);
    }

    public static void reg(Class<?> clazz, Object instance) {
        INSTANCE_REGISTRY.put(clazz, instance);
    }

    public static <T> T getInstance(Class<T> clazz) {
        return (T) INSTANCE_REGISTRY.get(clazz);
    }
}
