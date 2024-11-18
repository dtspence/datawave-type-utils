package datawave.data.type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.InputChunked;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.io.OutputChunked;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import datawave.data.normalizer.Normalizer;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

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
        String normalizedValue = t.getNormalizedValue();
        chunkedOut.writeString(normalizedValue);
        kryo.writeClass(chunkedOut, t.getNormalizer().getClass());
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
            Registration normalizerRegistration = kryo.readClass(chunkedInput);
            if (normalizerRegistration == null) {
                throw new KryoException("Unable to find normalizer class from Kryo input");
            }
            
            S delegateObj = kryo.readObject(chunkedInput, delegateType, delegateSerializer);
            
            typeObj = tryCreateType(cache, typeClass, normalizerRegistration);
            typeObj.setDelegateAndNormalizedValue(delegateObj, normalizedValue);
        } finally {
            chunkedInput.nextChunks();
        }
        return typeObj;
    }
    
    private Constructor<T> getConstructor(DelegateTypeSerializerCache cache, Class<T> typeClass, Class<Normalizer<S>> normalizerClass) {
        Constructor<T> constructor = (Constructor<T>) cache.getConstructorMap().get(typeClass);
        if (constructor == null) {
            Constructor<?>[] constructors = typeClass.getConstructors();
            Optional<Constructor<?>> selectedConstructor = Arrays.stream(constructors)
                            .filter(c -> c.getParameterTypes().length == 1 && c.getParameterTypes()[0].isAssignableFrom(normalizerClass.getClass()))
                            .findFirst();
            
            if (!selectedConstructor.isPresent()) {
                selectedConstructor = Arrays.stream(constructors).filter(c -> c.getParameterTypes().length == 0).findFirst();
                if (!selectedConstructor.isPresent()) {
                    throw new KryoException("Unable to find normalizer-based or default empty constructor: " + typeClass.getName());
                }
            }
            constructor = (Constructor<T>) selectedConstructor.get();
            cache.getConstructorMap().put(typeClass, constructor);
        }
        return constructor;
    }
    
    private T tryCreateType(DelegateTypeSerializerCache cache, Class<T> typeClass, Registration normalizerRegistration) {
        Class<Normalizer<S>> normalizerClass = (Class<Normalizer<S>>) normalizerRegistration.getType();
        Normalizer<S> normalizer;
        try {
            normalizer = normalizerClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new KryoException("Unable to create new normalizer instance: " + e.getMessage(), e);
        }
        
        Constructor<T> constructor = getConstructor(cache, typeClass, normalizerClass);
        T typeObj;
        try {
            typeObj = constructor.getParameterTypes().length == 0 ? constructor.newInstance(null) : constructor.newInstance(normalizer);
        } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
            throw new KryoException("Unable to create type for normalizer: " + normalizerClass.getName(), e);
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
