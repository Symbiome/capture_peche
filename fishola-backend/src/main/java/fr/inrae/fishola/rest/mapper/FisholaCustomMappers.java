package fr.inrae.fishola.rest.mapper;

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
import fr.inrae.fishola.entities.enums.Gender;
import fr.inrae.fishola.rest.feedback.Feedback;
import fr.inrae.fishola.rest.feedback.ImmutableFeedback;
import fr.inrae.fishola.rest.security.ImmutableUserProfile;
import fr.inrae.fishola.rest.security.ImmutableUserSettings;
import fr.inrae.fishola.rest.security.UserProfile;
import fr.inrae.fishola.rest.security.UserSettings;
import io.quarkus.jackson.ObjectMapperCustomizer;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuiton.util.pagination.PaginationOrder;
import org.nuiton.util.pagination.PaginationParameter;
import org.nuiton.util.pagination.PaginationResult;

import javax.inject.Singleton;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

@Singleton
public class FisholaCustomMappers implements ObjectMapperCustomizer {

    private static final Log log = LogFactory.getLog(FisholaCustomMappers.class);

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

    static Integer readIntegerOrNull(TreeNode node, String name) {
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

    static Optional<Integer> readInteger(TreeNode node, String name) {
        Integer value = readIntegerOrNull(node, name);
        Optional<Integer> result = Optional.ofNullable(value);
        return result;
    }

    static String readTextOrNull(TreeNode node, String name) {
        TreeNode subNode = node.get(name);
        if (subNode == null || (subNode instanceof NullNode) || subNode.isMissingNode()) {
            return null;
        }
        String value = ((TextNode) subNode).textValue();
        String result = StringUtils.trimToNull(value);
        return result;
    }

    static Optional<String> readText(TreeNode node, String name) {
        String value = readTextOrNull(node, name);
        Optional<String> result = Optional.ofNullable(value);
        return result;
    }

    protected static Optional<LocalDateTime> readIso8601AtZone(String dateString, ZoneId zoneId) {
        try {
            Optional<Instant> instant = iso8601ToInstant(dateString);
            Optional<ZonedDateTime> zonedDateTime = instant.map(i -> i.atZone(zoneId));
            Optional<LocalDateTime> localDateTime = zonedDateTime.map(ZonedDateTime::toLocalDateTime);
            return localDateTime;
        } catch (Exception eee) {
            log.error("Unable to read date: " + dateString, eee);
            return Optional.empty();
        }
    }

    protected static Optional<Instant> iso8601ToInstant(String dateString) {
        try {
            TemporalAccessor temporalAccessor = DateTimeFormatter.ISO_INSTANT.parse(dateString);
            Instant instant = Instant.from(temporalAccessor);
            Optional<Instant> result = Optional.of(instant);
            return result;
        } catch (Exception eee) {
            log.error("Unable to read date: " + dateString, eee);
            return Optional.empty();
        }
    }

    public static class FeedbackBeanDeserializer extends StdDeserializer<Feedback> {

        protected FeedbackBeanDeserializer() {
            super(Feedback.class);
        }

        @Override
        public Feedback deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException {
            TreeNode node = jp.readValueAsTree();
            ImmutableFeedback.Builder builder = ImmutableFeedback.builder();
            String category = readTextOrNull(node, "category");
            Preconditions.checkArgument(StringUtils.isNotEmpty(category), "La catégorie est obligatoire");
            builder.category(category);
            readText(node, "userId").ifPresent(builder::userId);
            readText(node, "email").ifPresent(builder::email);
            readText(node, "description").ifPresent(builder::description);
            readText(node, "screenshot").ifPresent(builder::screenshot);
            readText(node, "browser").ifPresent(builder::browser);
            readText(node, "os").ifPresent(builder::os);
            readText(node, "platform").ifPresent(builder::platform);
            readText(node, "screenResolution").ifPresent(builder::screenResolution);
            readText(node, "displaySize").ifPresent(builder::displaySize);
            readText(node, "locale").ifPresent(builder::locale);
            readText(node, "frontendVersion").ifPresent(builder::frontendVersion);
            readText(node, "backendVersion").ifPresent(builder::backendVersion);
            readText(node, "location").ifPresent(builder::location);
            readText(node, "locationTitle").ifPresent(builder::locationTitle);
            readText(node, "date").flatMap(str -> FisholaCustomMappers.readIso8601AtZone(str, ZoneId.systemDefault())).ifPresent(builder::date);
            readText(node, "device").ifPresent(builder::device);
            Feedback result = builder.build();
            return result;
        }
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
                String clause = readTextOrNull(n, "clause");
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

            String firstName = readTextOrNull(node, "firstName");
            String email = readTextOrNull(node, "email");
            Optional<String> lastName = readText(node, "lastName");
            Optional<Integer> birthYear = readInteger(node, "birthYear");
            Optional<String> genderString = readText(node, "gender");

            ImmutableUserProfile.Builder builder = ImmutableUserProfile.builder();

            builder.firstName(firstName);
            builder.email(email);
            lastName.ifPresent(builder::lastName);
            birthYear.ifPresent(builder::birthYear);
            genderString.map(Gender::valueOf).ifPresent(builder::gender);

            // Le champ est nécessaire côté Java mais on attend rien de la part du front
            builder.sampleBaseId("########");

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
            addDeserializer(Feedback.class, new FeedbackBeanDeserializer());
        }

    }

}