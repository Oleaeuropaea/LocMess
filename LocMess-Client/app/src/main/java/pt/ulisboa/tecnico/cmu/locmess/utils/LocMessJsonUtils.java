package pt.ulisboa.tecnico.cmu.locmess.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import pt.ulisboa.tecnico.cmu.locmess.background.managers.WiFiDirectManager;
import pt.ulisboa.tecnico.cmu.locmess.features.locations.LocationLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.posts.PostLocMess;
import pt.ulisboa.tecnico.cmu.locmess.features.shared.InterestLocMess;

public final class LocMessJsonUtils {
    private static final GsonBuilder builder = new GsonBuilder()
            .registerTypeAdapter(DateTime.class, new LocMessJsonUtils.DateTimeConverter());
    private static final Gson GSON = builder.create();

    private static final JsonParser PARSER = new JsonParser();

    //-- -------------------------------------------------------------------------------------------

    public static JsonObject toJsonObj(String json) {
        return PARSER.parse(json).getAsJsonObject();
    }

    public static JsonArray toJsonArray(String json) {
        return PARSER.parse(json).getAsJsonArray();
    }

    public static String getTimestampJson(String json) {
        JsonArray jsonArray = toJsonArray(json);

        return jsonArray.get(0).getAsJsonObject().get("timestamp").toString();
    }

    public static String getResultJson(String json) {
        JsonArray jsonArray = toJsonArray(json);

        return jsonArray.get(1).getAsJsonObject().get("result").toString();
    }

    //-- Authentication ----------------------------------------------------------------------------

    public static String toLoginJson(String username, String password) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("password", password);

        return jsonObject.toString();
    }

    public static String toTokenJson(String token) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("token", token);

        return jsonObject.toString();
    }

    public static String toRegisterJson(String username, String email,
                                        String password1, String password2) {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("username", username);
        jsonObject.addProperty("email", email);
        jsonObject.addProperty("password1", password1);
        jsonObject.addProperty("password2", password2);

        return jsonObject.toString();
    }

    //-- Post --------------------------------------------------------------------------------------

    public static List<PostLocMess> toPostsList(String json) {
        Type type = new TypeToken<List<PostLocMess>>() {
        }.getType();

        return GSON.fromJson(json, type);
    }

    public static PostLocMess toPostObj(String json) {
        Type type = new TypeToken<PostLocMess>() {
        }.getType();

        return GSON.fromJson(json, type);
    }

    public static String toCentralizePostJson(PostLocMess postLocMess) {
        LocationLocMess location = postLocMess.getLocation();
        postLocMess.setLocation(null);

        Type type = new TypeToken<PostLocMess>() {
        }.getType();

        JsonObject jsonObject = (JsonObject) GSON.toJsonTree(postLocMess, type);
        jsonObject.addProperty("location_url", location.getUrl());

        return jsonObject.toString();
    }

    public static String toDecentralizePostJson(PostLocMess postLocMess) {
        Type type = new TypeToken<PostLocMess>() {
        }.getType();

        return GSON.toJson(postLocMess, type);
    }

    //-- WiFi Direct Message -----------------------------------------------------------------------

    public static String toWiFiDirectMsgJson(WiFiDirectManager.WiFIDirectMessage message){
        Gson gson = new Gson();
        String messageJson = gson.toJson(message);
        return messageJson;
    }

    public static WiFiDirectManager.WiFIDirectMessage toWiFiDirectMsgObject(String message){
        Gson gson = new Gson();
        WiFiDirectManager.WiFIDirectMessage wiFIDirectMessage =
                                gson.fromJson(message, WiFiDirectManager.WiFIDirectMessage.class);
        return wiFIDirectMessage;
    }

    //-- InterestLocMess ----------------------------------------------------------------------------------

    public static List<InterestLocMess> toInterestsList(String json) {
        Type type = new TypeToken<ArrayList<InterestLocMess>>() {
        }.getType();

        return GSON.fromJson(json, type);
    }

    public static String toInterestsJson(List<InterestLocMess> interests) {
        Type type = new TypeToken<List<InterestLocMess>>() {
        }.getType();

        return GSON.toJson(interests, type);
    }

    public static String toInterestsJson(InterestLocMess interest) {
        Type type = new TypeToken<InterestLocMess>() {
        }.getType();

        return GSON.toJson(interest, type);
    }

    public static InterestLocMess toInterestsObj(String json) {
        Type type = new TypeToken<InterestLocMess>() {
        }.getType();

        return GSON.fromJson(json, type);
    }

    //-- Location ----------------------------------------------------------------------------------

    public static List<LocationLocMess> toLocationsList(String json) {
        JsonArray jsonArray = toJsonArray(json);

        Type type = new TypeToken<List<LocationLocMess>>() {
        }.getType();

        return GSON.fromJson(jsonArray, type);
    }

    public static String toLocationJson(List<LocationLocMess> locations) {
        Type type = new TypeToken<List<LocationLocMess>>() {
        }.getType();

        return GSON.toJson(locations, type);
    }

    public static String toLocationJson(LocationLocMess locationLocMess) {
        Type type = new TypeToken<LocationLocMess>() {
        }.getType();

        return GSON.toJson(locationLocMess, type);
    }

    public static LocationLocMess toLocationLocMessObj(String json) {
        Type type = new TypeToken<LocationLocMess>() {
        }.getType();

        return GSON.fromJson(json, type);
    }

    //-- Serializers -------------------------------------------------------------------------------
    /**
     * GSON serialiser/deserialiser for converting Joda {@link DateTime} objects.
     */
    private static class DateTimeConverter implements JsonSerializer<DateTime>, JsonDeserializer<DateTime>
    {
        /**
         * Gson invokes this call-back method during serialization when it encounters a field of the
         * specified type. <p>
         *
         * In the implementation of this call-back method, you should consider invoking
         * {@link JsonSerializationContext#serialize(Object, Type)} method to create JsonElements for any
         * non-trivial field of the {@code src} object. However, you should never invoke it on the
         * {@code src} object itself since that will cause an infinite loop (Gson will call your
         * call-back method again).
         * @param src the object that needs to be converted to Json.
         * @param typeOfSrc the actual type (fully genericized version) of the source object.
         * @return a JsonElement corresponding to the specified object.
         */
        @Override
        public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context)
        {
            final DateTimeFormatter fmt = ISODateTimeFormat.dateTime();
            return new JsonPrimitive(fmt.print(src));
        }

        /**
         * Gson invokes this call-back method during deserialization when it encounters a field of the
         * specified type. <p>
         *
         * In the implementation of this call-back method, you should consider invoking
         * {@link JsonDeserializationContext#deserialize(JsonElement, Type)} method to create objects
         * for any non-trivial field of the returned object. However, you should never invoke it on the
         * the same type passing {@code json} since that will cause an infinite loop (Gson will call your
         * call-back method again).
         * @param json The Json data being deserialized
         * @param typeOfT The type of the Object to deserialize to
         * @return a deserialized object of the specified type typeOfT which is a subclass of {@code T}
         * @throws JsonParseException if json is not in the expected format of {@code typeOfT}
         */
        @Override
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException
        {
            // Do not try to deserialize null or empty values
            if (json.getAsString() == null || json.getAsString().isEmpty())
            {
                return null;
            }

            final DateTimeFormatter fmt = ISODateTimeFormat.dateTimeParser();
            return fmt.parseDateTime(json.getAsString());
        }
    }
}
