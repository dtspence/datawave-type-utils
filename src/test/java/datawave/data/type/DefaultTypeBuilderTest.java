package datawave.data.type;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.aggregator.AggregateWith;
import org.junit.jupiter.params.aggregator.ArgumentsAccessor;
import org.junit.jupiter.params.aggregator.ArgumentsAggregationException;
import org.junit.jupiter.params.aggregator.ArgumentsAggregator;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DefaultTypeBuilderTest {
    private final static Logger LOG = LoggerFactory.getLogger(DefaultTypeBuilderTest.class);
    
    @Test
    public void testBuildsStringType() {
        String expectedDelegate = "Test";
        String expectedNormalized = "test";
        StringType expectedType = new StringType();
        expectedType.setDelegateFromString(expectedDelegate);
        
        // Check the normalized value set by delegateFromString is same as test expectation
        assertEquals(expectedNormalized, expectedType.getNormalizedValue());
        
        StringType type = TypeBuilder.of(StringType.class).delegate(expectedDelegate).normalizedValue(expectedNormalized).build();
        
        assertEquals(expectedNormalized, type.getNormalizedValue());
        assertEquals(expectedDelegate, type.getDelegate());
    }
    
    @ParameterizedTest
    @CsvSource({"datawave.data.type.DateType, java.util.Date, 1679798782983, 2023-03-26T02:46:22.983Z",
            "datawave.data.type.NumberType, java.math.BigDecimal, 1, +aE1", "datawave.data.type.HitTermType, java.lang.String, Test, test",
            "datawave.data.type.StringType, java.lang.String, Test, test", "datawave.data.type.NoOpType, java.lang.String, Test, Test",})
    public void testDefaultBuilderEqualsTypeCreateAndDelegateAsString(@AggregateWith(TypeArgumentAggregator.class) TypeArgument typeArgument) throws Exception {
        if (LOG.isDebugEnabled()) {
            BaseType debugDelegate = typeArgument.typeClass.newInstance();
            debugDelegate.normalizeAndSetNormalizedValue((Comparable) typeArgument.delegate);
            LOG.debug("Normalized value for delegate object: " + debugDelegate.getNormalizedValue());
        }
        
        BaseType<?> expectedType = typeArgument.typeClass.newInstance();
        expectedType.setDelegateFromString(typeArgument.normalizedValue);
        
        // Check the normalized value set by delegateFromString is same as test expectation
        assertEquals(typeArgument.normalizedValue, expectedType.getNormalizedValue(), "expected normalized check");
        
        BaseType<?> type = TypeBuilder.of(typeArgument.typeClass).delegate((Comparable) typeArgument.delegate).normalizedValue(typeArgument.normalizedValue)
                        .build();
        
        assertEquals(typeArgument.normalizedValue, type.getNormalizedValue(), "normalized value");
        assertEquals(typeArgument.delegate, type.getDelegate(), "delegate value");
    }
    
    static class TypeArgument {
        Class<? extends BaseType> typeClass;
        Class delegateClass;
        Object delegate;
        String normalizedValue;
    }
    
    static class TypeArgumentAggregator implements ArgumentsAggregator {
        private final static Class<?>[] IMPLICIT_CONVERT = new Class<?>[] {Number.class, BigDecimal.class, String.class};
        
        @Override
        public TypeArgument aggregateArguments(ArgumentsAccessor argumentsAccessor, ParameterContext parameterContext) throws ArgumentsAggregationException {
            TypeArgument typeArg = new TypeArgument();
            try {
                typeArg.typeClass = (Class<? extends BaseType>) Class.forName((String) argumentsAccessor.get(0));
                typeArg.delegateClass = Class.forName((String) argumentsAccessor.get(1));
            } catch (Exception e) {
                throw new IllegalStateException("Unable to map to class: " + e.getMessage(), e);
            }
            
            if (isImplicitConversion(typeArg.delegateClass)) {
                typeArg.delegate = argumentsAccessor.get(2, typeArg.delegateClass);
            } else if (typeArg.delegateClass.equals(Date.class)) {
                Long epochMilli = argumentsAccessor.get(2, Long.class);
                Instant epocInstant = Instant.ofEpochMilli(epochMilli);
                Date dateValue = Date.from(epocInstant);
                typeArg.delegate = dateValue;
            } else {
                throw new IllegalStateException("Unmapped non-implicit delegate class: " + typeArg.delegateClass);
            }
            
            typeArg.normalizedValue = (String) argumentsAccessor.get(3);
            return typeArg;
        }
        
        private static boolean isImplicitConversion(Class<?> classType) {
            return Arrays.stream(IMPLICIT_CONVERT).filter(x -> x.isAssignableFrom(classType)).findAny().isPresent();
        }
    }
}
