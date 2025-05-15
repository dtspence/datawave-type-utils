package datawave.data.type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class DelegateTypeSerializer<S extends Comparable<S> & Serializable,T extends BaseType<S>> extends Serializer<T> {
    private final Serializer<S> delegateSerializer;
    private final Class<S> delegateType;

    public DelegateTypeSerializer(Serializer<S> delegateSerializer, Class<S> delegateType) {
        this.delegateSerializer = delegateSerializer;
        this.delegateType = delegateType;
    }

    public static Map<Class<?>,DelegateTypeSerializer<?,? extends BaseType<?>>> defaultSerializers() {
        Map<Class<?>,DelegateTypeSerializer<?,? extends BaseType<?>>> map = new LinkedHashMap<>();
        map.put(StringType.class, new DelegateTypeSerializer<>(new DefaultSerializers.StringSerializer(), String.class));
        map.put(RawDateType.class, new DelegateTypeSerializer<>(new DefaultSerializers.StringSerializer(), String.class));
        map.put(TrimLeadingZerosType.class, new DelegateTypeSerializer<>(new DefaultSerializers.StringSerializer(), String.class));
        return map;
    }

    @Override
    public void write(Kryo kryo, Output output, T t) {
        OutputChunked chunkedOut = new OutputChunked(output);
        String normalizedValue = t.normalizedValue;
        chunkedOut.writeString(normalizedValue);
        kryo.writeObject(chunkedOut, t.getDelegate(), delegateSerializer);
        chunkedOut.endChunks();
    }

    @Override
    public T read(Kryo kryo, Input input, Class<T> typeClass) {
        T typeObj;
        InputChunked chunkedInput = new InputChunked(input);
        DelegateTypeSerializerCache cache = getCache(kryo);
        try {
            String normalizedValue = chunkedInput.readString();
            S delegateObj = kryo.readObject(chunkedInput, delegateType, delegateSerializer);
            typeObj = tryCreateType(typeClass);
            typeObj.delegate = delegateObj;
            typeObj.normalizedValue = normalizedValue;
        } finally {
            chunkedInput.nextChunks();
        }
        return typeObj;
    }

    private T tryCreateType(Class<T> typeClass) {
        T typeObj;
        try {
            Constructor<T> constructor = typeClass.getDeclaredConstructor();
            typeObj = constructor.newInstance();
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException | NoSuchMethodException e) {
            throw new KryoException("Unable to create type: " + typeClass.getName(), e);
        }
        return typeObj;
    }

    private DelegateTypeSerializerCache getCache(Kryo kryo) {
        DelegateTypeSerializerCache cache = (DelegateTypeSerializerCache) kryo.getContext().get(DelegateTypeSerializerCache.class);
        if (cache == null) {
            cache = new DelegateTypeSerializerCache();
            kryo.getContext().put(DelegateTypeSerializerCache.class, cache);
        }
        return cache;
    }

    private static class DelegateTypeSerializerCache {
        private final Map<Class<? extends BaseType<?>>,Constructor<? extends BaseType<?>>> constructorMap = new IdentityHashMap<>();

        private Map<Class<? extends BaseType<?>>,Constructor<? extends BaseType<?>>> getConstructorMap() {
            return constructorMap;
        }
    }
}
