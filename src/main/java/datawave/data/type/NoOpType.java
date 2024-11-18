package datawave.data.type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import datawave.data.normalizer.Normalizer;

public class NoOpType extends BaseType<String> implements KryoSerializable {
    
    private static final long serialVersionUID = 5316252096230974722L;
    private static final long STATIC_SIZE = PrecomputedSizes.STRING_STATIC_REF * 2 + Sizer.REFERENCE;
    private static final Serializer<String> DELEGATE_SERIALIZER = new DefaultSerializers.StringSerializer();
    private static final Class<String> DELEGATE_CLASS = String.class;
    
    public NoOpType() {
        super(Normalizer.NOOP_NORMALIZER);
    }
    
    public NoOpType(String value) {
        this();
        this.setDelegate(value);
        super.setNormalizedValue(normalizer.normalize(value));
    }
    
    /**
     * two identical strings + normalizer reference
     * 
     * @return
     */
    @Override
    public long sizeInBytes() {
        return STATIC_SIZE + (4 * normalizedValue.length());
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
