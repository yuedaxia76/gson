package com.google.gson.functional;

import java.io.IOException;
import java.lang.reflect.Type;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import junit.framework.TestCase;

public class TypeAdapterRuntimeTypeWrapperTest extends TestCase {
  private static class Base {
  }
  private static class Subclass extends Base {
    @SuppressWarnings("unused")
    String f = "test";
  }
  private static class Container {
    @SuppressWarnings("unused")
    Base b = new Subclass();
  }
  private static class Deserializer implements JsonDeserializer<Base> {
    @Override
    public Base deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
      throw new UnsupportedOperationException();
    }
  }

  /**
   * When custom {@link JsonSerializer} is registered for Base should
   * prefer that over reflective adapter for Subclass for serialization.
   */
  public void testJsonSerializer() {
    Gson gson = new GsonBuilder()
      .registerTypeAdapter(Base.class, new JsonSerializer<Base>() {
        @Override
        public JsonElement serialize(Base src, Type typeOfSrc, JsonSerializationContext context) {
          return new JsonPrimitive("serializer");
        }
      })
      .create();
    String json = gson.toJson(new Container());
    assertEquals("{\"b\":\"serializer\"}", json);
  }

  /**
   * When only {@link JsonDeserializer} is registered for Base, then on
   * serialization should prefer reflective adapter for Subclass since
   * Base would use reflective adapter as delegate.
   */
  public void testJsonDeserializer_ReflectiveSerializerDelegate() {
    Gson gson = new GsonBuilder()
      .registerTypeAdapter(Base.class, new Deserializer())
      .create();
    String json = gson.toJson(new Container());
    assertEquals("{\"b\":{\"f\":\"test\"}}", json);
  }

  /**
   * When {@link JsonDeserializer} with custom adapter as delegate is
   * registered for Base, then on serialization should prefer custom adapter
   * delegate for Base over reflective adapter for Subclass.
   */
  public void testJsonDeserializer_CustomSerializerDelegate() {
    Gson gson = new GsonBuilder()
       // Register custom delegate
      .registerTypeAdapter(Base.class, new TypeAdapter<Base>() {
        @Override
        public Base read(JsonReader in) throws IOException {
          throw new UnsupportedOperationException();
        }
        @Override
        public void write(JsonWriter out, Base value) throws IOException {
          out.value("custom delegate");
        }
      })
      .registerTypeAdapter(Base.class, new Deserializer())
      .create();
    String json = gson.toJson(new Container());
    assertEquals("{\"b\":\"custom delegate\"}", json);
  }

  /**
   * When two (or more) {@link JsonDeserializer}s are registered for Base
   * which eventually fall back to reflective adapter as delegate, then on
   * serialization should prefer reflective adapter for Subclass.
   */
  public void testJsonDeserializer_ReflectiveTreeSerializerDelegate() {
    Gson gson = new GsonBuilder()
       // Register delegate which itself falls back to reflective serialization
      .registerTypeAdapter(Base.class, new Deserializer())
      .registerTypeAdapter(Base.class, new Deserializer())
      .create();
    String json = gson.toJson(new Container());
    assertEquals("{\"b\":{\"f\":\"test\"}}", json);
  }

  /**
   * When {@link JsonDeserializer} with {@link JsonSerializer} as delegate
   * is registered for Base, then on serialization should prefer
   * {@code JsonSerializer} over reflective adapter for Subclass.
   */
  public void testJsonDeserializer_JsonSerializerDelegate() {
    Gson gson = new GsonBuilder()
        // Register JsonSerializer as delegate
      .registerTypeAdapter(Base.class, new JsonSerializer<Base>() {
        @Override
        public JsonElement serialize(Base src, Type typeOfSrc, JsonSerializationContext context) {
          return new JsonPrimitive("custom delegate");
        }
      })
      .registerTypeAdapter(Base.class, new Deserializer())
      .create();
    String json = gson.toJson(new Container());
    assertEquals("{\"b\":\"custom delegate\"}", json);
  }
}
