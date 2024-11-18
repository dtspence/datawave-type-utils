package datawave.data.type;

import com.google.common.base.Preconditions;

import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Optional;

public interface TypeBuilder<S extends Comparable<S> & Serializable,T extends BaseType<S>> {
    
    static <S extends Comparable<S> & Serializable,T extends BaseType<S>> TypeBuilder<S,T> of(Class<T> typeClass) {
        return new DefaultTypeBuilder(typeClass);
    }
    
    TypeBuilder<S,T> delegate(S delegate);
    
    TypeBuilder<S,T> normalizedValue(String normalizedValue);
    
    boolean isBuildable();
    
    T build();
    
    class DefaultTypeBuilder<S extends Comparable<S> & Serializable,T extends BaseType<S>> implements TypeBuilder<S,T> {
        private final Class<T> typeClass;
        private Constructor<T> constructor;
        private S delegate;
        private String normalizedValue;
        
        DefaultTypeBuilder(Class<T> typeClass) {
            this.typeClass = typeClass;
        }
        
        @Override
        public TypeBuilder<S,T> delegate(S delegate) {
            this.delegate = delegate;
            return this;
        }
        
        @Override
        public TypeBuilder<S,T> normalizedValue(String normalizedValue) {
            this.normalizedValue = normalizedValue;
            return this;
        }
        
        @Override
        public boolean isBuildable() {
            return tryFindConstructor(typeClass).isPresent();
        }
        
        @Override
        public T build() {
            Preconditions.checkNotNull(delegate);
            Preconditions.checkNotNull(normalizedValue);
            
            T typeObj;
            try {
                typeObj = typeClass.newInstance();
                typeObj.setDelegateAndNormalizedValue(delegate, normalizedValue);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            
            return typeObj;
        }
        
        private Optional<Constructor<?>> tryFindConstructor(Class<T> typeClass) {
            Constructor<?>[] constructors = typeClass.getConstructors();
            Optional<Constructor<?>> selectedConstructor = Arrays.stream(constructors).filter(c -> c.getParameterTypes().length == 0).findFirst();
            return selectedConstructor;
        }
    }
}
