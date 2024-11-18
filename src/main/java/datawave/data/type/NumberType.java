package datawave.data.type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import datawave.data.normalizer.Normalizer;

import java.math.BigDecimal;

public class NumberType extends BaseType<BigDecimal> implements KryoSerializable {
    
    private static final long serialVersionUID = 1398451215614987988L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF + PrecomputedSizes.BIGDECIMAL_STATIC_REF + Sizer.REFERENCE;
    private static final Serializer<BigDecimal> DELEGATE_SERIALIZER = new DefaultSerializers.BigDecimalSerializer();
    private static final Class<BigDecimal> DELEGATE_CLASS = BigDecimal.class;
    
    public NumberType() {
        super(Normalizer.NUMBER_NORMALIZER);
    }
    
    public NumberType(String delegateString) {
        super(delegateString, Normalizer.NUMBER_NORMALIZER);
    }
    
    /**
     * one String, one BigDecimal and one reference to a normalizer
     */
    @Override
    public long sizeInBytes() {
        return STATIC_SIZE + (2 * normalizedValue.length());
    }
    
    @Override
    public void write(Kryo kryo, Output output) {
        writeMetadata(kryo, output, DELEGATE_SERIALIZER);
    }
    
    @Override
    public void read(Kryo kryo, Input input) {
        readMetadata(kryo, input, DELEGATE_SERIALIZER, DELEGATE_CLASS);
    }
}
