package fr.inra.fishola.rest.mapper;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.google.common.base.Preconditions;
import fr.inra.fishola.entities.enums.Gender;
import fr.inra.fishola.rest.security.ImmutableUserProfile;
import fr.inra.fishola.rest.security.ImmutableUserSettings;
import fr.inra.fishola.rest.security.UserProfile;
import fr.inra.fishola.rest.security.UserSettings;
import io.quarkus.jackson.ObjectMapperCustomizer;
import org.apache.commons.lang3.StringUtils;
import org.nuiton.util.pagination.PaginationOrder;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Singleton
public class FisholaCustomMappers implements ObjectMapperCustomizer {

    public void customize(ObjectMapper mapper) {
        mapper.registerModule(new FisholaModule());
    }

    static boolean readBoolean(TreeNode node, String name) {
        TreeNode subNode = node.get(name);
        boolean result;
        if (subNode instanceof BooleanNode) {
            result = ((BooleanNode) subNode).booleanValue();
        } else if (subNode instanceof TextNode) {
            String resultString = ((TextNode) subNode).textValue();
            result = Boolean.parseBoolean(resultString);
        } else {
            throw new IllegalArgumentException("Unexpected type:" + subNode.getClass().getName());
        }
        return result;
    }

    static int readInt(TreeNode node, String name) {
        TreeNode subNode = node.get(name);
        int result;
        if (subNode instanceof IntNode) {
            result = ((IntNode) subNode).intValue();
        } else if (subNode instanceof TextNode) {
            String resultString = ((TextNode) subNode).textValue();
            result = Integer.parseInt(resultString);
        } else {
            throw new IllegalArgumentException("Unexpected type:" + subNode.getClass().getName());
        }
        return result;
    }

    static Integer readInteger(TreeNode node, String name) {
        TreeNode subNode = node.get(name);
        if (subNode == null || (subNode instanceof NullNode) || subNode.isMissingNode()) {
            return null;
        }
        int result;
        if (subNode instanceof IntNode) {
            result = ((IntNode) subNode).intValue();
        } else if (subNode instanceof TextNode) {
            String resultString = ((TextNode) subNode).textValue();
            if (StringUtils.isEmpty(resultString)) {
                return null;
            }
            result = Integer.parseInt(resultString);
        } else {
            throw new IllegalArgumentException("Unexpected type:" + subNode.getClass().getName());
        }
        return result;
    }

    static String readText(TreeNode node, String name) {
        TreeNode subNode = node.get(name);
        if (subNode == null || (subNode instanceof NullNode) || subNode.isMissingNode()) {
            return null;
        }
        String result = ((TextNode) subNode).textValue();
        return result;
    }

    public static class PaginationParameterDeserializer extends StdDeserializer<PaginationParameter> {

        protected PaginationParameterDeserializer() {
            super(PaginationParameter.class);
        }

        @Override
        public PaginationParameter deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            TreeNode node = jp.readValueAsTree();

            TreeNode orderClausesNode = node.get("orderClauses");
            Preconditions.checkState(orderClausesNode.isArray());
            List<PaginationOrder> orders = new LinkedList<>();
            for (JsonNode n : (ArrayNode) orderClausesNode) {
                // TODO AThimel 13/01/2020 : Ça aurait été mieux de réutiliser le DeserializationContext
                String clause = readText(n, "clause");
                boolean desc = readBoolean(n, "desc");
                PaginationOrder order = new PaginationOrder(clause, desc);
                orders.add(order);
            }

            int pageNumberValue = readInt(node, "pageNumber");
            int pageSizeValue = readInt(node, "pageSize");
            PaginationParameter.PaginationParameterBuilder builder = PaginationParameter.builder(pageNumberValue, pageSizeValue);
            builder.addOrderClauses(orders);
            PaginationParameter result = builder.build();
            return result;
        }
    }

    public static class UserProfileDeserializer extends StdDeserializer<UserProfile> {

        protected UserProfileDeserializer() {
            super(PaginationParameter.class);
        }

        @Override
        public UserProfile deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            TreeNode node = jp.readValueAsTree();

            String firstName = readText(node, "firstName");
            String email = readText(node, "email");
            String lastName = readText(node, "lastName");
            Integer birthYear = readInteger(node, "birthYear");
            String genderString = readText(node, "gender");

            ImmutableUserProfile.Builder builder = ImmutableUserProfile.builder();

            builder.firstName(firstName);
            builder.email(email);
            Optional.ofNullable(StringUtils.trimToNull(lastName)).ifPresent(builder::lastName);
            Optional.ofNullable(birthYear).ifPresent(builder::birthYear);
            Optional.ofNullable(StringUtils.trimToNull(genderString)).map(Gender::valueOf).ifPresent(builder::gender);

            UserProfile result = builder.build();
            return result;
        }
    }

    public static class UserSettingsDeserializer extends StdDeserializer<UserSettings> {

        protected UserSettingsDeserializer() {
            super(PaginationParameter.class);
        }

        @Override
        public UserSettings deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            TreeNode node = jp.readValueAsTree();

            boolean promptSamples = readBoolean(node, "promptSamples");
            boolean promptWeight = readBoolean(node, "promptWeight");

            UserSettings result = ImmutableUserSettings.builder()
                    .promptSamples(promptSamples)
                    .promptWeight(promptWeight)
                    .build();
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
            addDeserializer(UserProfile.class, new UserProfileDeserializer());
            addDeserializer(UserSettings.class, new UserSettingsDeserializer());
        }

    }

}