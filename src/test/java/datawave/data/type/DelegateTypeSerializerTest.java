package datawave.data.type;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.DefaultSerializers;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DelegateTypeSerializerTest {
    private final static Serializer<String> STRING_DELEGATE_SERIALIZER = new DefaultSerializers.StringSerializer();
    
    public void setUp() {}
    
    @Test
    public void testStringTypeSerializeAndDeserialize() {
        DelegateTypeSerializer<String,StringType> serializer = new DelegateTypeSerializer<>(STRING_DELEGATE_SERIALIZER, String.class);
        Kryo kryoOutput = new Kryo();
        ByteArrayOutputStream outputBytes = new ByteArrayOutputStream();
        Output output = new Output(outputBytes);
        StringType expectedType = new StringType();
        expectedType.setDelegateFromString("TEST_Value");
        
        serializer.write(kryoOutput, output, expectedType);
        output.flush();
        
        Kryo kryoInput = new Kryo();
        Input input = new Input(new ByteArrayInputStream(outputBytes.toByteArray()));
        StringType actualType = serializer.read(kryoInput, input, StringType.class);
        
        assertEquals(expectedType.getDelegate(), actualType.getDelegate());
        assertEquals(expectedType.getNormalizedValue(), actualType.getNormalizedValue());
        assertEquals(expectedType.getNormalizer().getClass(), actualType.getNormalizer().getClass());
    }
}
