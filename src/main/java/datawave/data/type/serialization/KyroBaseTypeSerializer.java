package datawave.data.type.serialization;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import datawave.data.type.BaseType;

import java.io.Serializable;

public class KyroBaseTypeSerializer<U extends Comparable<U> & Serializable,T extends BaseType<U>> extends Serializer<T> {
    
    @Override
    public void write(Kryo kryo, Output output, T t) {
        
    }
    
    @Override
    public T read(Kryo kryo, Input input, Class<T> aClass) {
        return null;
    }
}
