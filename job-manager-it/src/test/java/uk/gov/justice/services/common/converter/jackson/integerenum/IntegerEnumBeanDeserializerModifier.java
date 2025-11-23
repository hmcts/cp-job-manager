package uk.gov.justice.services.common.converter.jackson.integerenum;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.BeanDeserializerModifier;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.util.EnumResolver;

public class IntegerEnumBeanDeserializerModifier extends BeanDeserializerModifier {

    @SuppressWarnings("unchecked")
    @Override
    public JsonDeserializer<?> modifyEnumDeserializer(
            final DeserializationConfig config,
            final JavaType type,
            final BeanDescription beanDesc,
            final JsonDeserializer<?> deserializer) {

        final AnnotatedClass annotatedClass = beanDesc.getClassInfo();

        final EnumResolver enumResolver = EnumResolver.constructFor(config, annotatedClass);

        return new IntegerEnumDeserializer(
                enumResolver,
                new EnumObjectUtil());
    }
}
