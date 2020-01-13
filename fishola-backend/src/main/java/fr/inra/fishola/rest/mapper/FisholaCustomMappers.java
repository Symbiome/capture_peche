package fr.inra.fishola.rest.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.google.common.base.Preconditions;
import io.quarkus.jackson.ObjectMapperCustomizer;
import org.nuiton.util.pagination.PaginationOrder;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

@Singleton
public class FisholaCustomMappers implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new FisholaModule());
    }

    public static class PaginationParameterDeserializer extends StdDeserializer<PaginationParameter> {

        protected PaginationParameterDeserializer() {
            super(PaginationParameter.class);
        }

        @Override
        public PaginationParameter deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
            TreeNode node = jp.readValueAsTree();
            TreeNode pageNumberNode = node.get("pageNumber");
            TreeNode pageSizeNode = node.get("pageSize");

            TreeNode orderClausesNode = node.get("orderClauses");
            Preconditions.checkState(orderClausesNode.isArray());
            List<PaginationOrder> orders = new LinkedList<>();
            for (JsonNode n : (ArrayNode) orderClausesNode) {
                // TODO AThimel 13/01/2020 : Ça aurait été mieux de réutiliser le DeserializationContext
                JsonNode clauseNode = n.get("clause");
                JsonNode descNode = n.get("desc");

                PaginationOrder order = new PaginationOrder(clauseNode.asText(), descNode.asBoolean());
                orders.add(order);
            }

            int pageNumberValue = ((IntNode) pageNumberNode).intValue();
            int pageSizeValue = ((IntNode) pageSizeNode).intValue();
            PaginationParameter.PaginationParameterBuilder builder = PaginationParameter.builder(pageNumberValue, pageSizeValue);
            builder.addOrderClauses(orders);
            PaginationParameter result = builder.build();
            return result;
        }
    }

    public static class FisholaModule extends SimpleModule {

        public FisholaModule() {
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

            addDeserializer(PaginationParameter.class, new PaginationParameterDeserializer());
        }

    }

}