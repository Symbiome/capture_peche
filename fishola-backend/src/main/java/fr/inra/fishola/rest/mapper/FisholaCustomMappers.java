package fr.inra.fishola.rest.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.quarkus.jackson.ObjectMapperCustomizer;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class FisholaCustomMappers implements ObjectMapperCustomizer {

    public class PaginationResultModule extends SimpleModule {

        public PaginationResultModule() {
            addSerializer(PaginationResult.class, new JsonSerializer<>() {
                @Override
                public void serialize(PaginationResult value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    gen.writeStartObject();
                    gen.writeObjectField("elements", value.getElements());
                    gen.writeObjectField("count", value.getCount());
                    gen.writeObjectField("currentPage", value.getCurrentPage());
                    gen.writeEndObject();
                }
            });
            addSerializer(PaginationParameter.class, new JsonSerializer<>() {
                @Override
                public void serialize(PaginationParameter value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
                    gen.writeStartObject();
                    gen.writeObjectField("pageNumber", value.getPageNumber());
                    gen.writeObjectField("pageSize", value.getPageSize());
                    gen.writeObjectField("orderClauses", value.getOrderClauses());
                    gen.writeEndObject();
                }
            });
        }
    }

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new PaginationResultModule());
    }
}