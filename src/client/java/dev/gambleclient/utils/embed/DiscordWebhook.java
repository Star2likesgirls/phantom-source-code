package dev.gambleclient.utils.embed;

import java.awt.Color;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.net.ssl.HttpsURLConnection;

public class DiscordWebhook {
   private final String webhookUrl;
   private String content;
   private String username;
   private String avatarUrl;
   private boolean tts;
   private final List embeds = new ArrayList();

   public DiscordWebhook(String webhookUrl) {
      this.webhookUrl = webhookUrl;
   }

   public void setContent(String content) {
      this.content = content;
   }

   public void setUsername(String username) {
      this.username = username;
   }

   public void setAvatarUrl(String avatarUrl) {
      this.avatarUrl = avatarUrl;
   }

   public void setTts(boolean tts) {
      this.tts = tts;
   }

   public void addEmbed(EmbedObject embed) {
      this.embeds.add(embed);
   }

   public void execute() throws Throwable {
      if (this.content == null && this.embeds.isEmpty()) {
         throw new IllegalArgumentException("Set content or add at least one EmbedObject");
      } else {
         JSONObject jsonSerializer = new JSONObject();
         jsonSerializer.put("content", this.content);
         jsonSerializer.put("username", this.username);
         jsonSerializer.put("avatar_url", this.avatarUrl);
         jsonSerializer.put("tts", this.tts);
         if (!this.embeds.isEmpty()) {
            ArrayList<JSONObject> embedList = new ArrayList();

            for(Object embedObj : this.embeds) {
               EmbedObject embed = (EmbedObject)embedObj;
               JSONObject jsonEmbed = new JSONObject();
               jsonEmbed.put("title", embed.title);
               jsonEmbed.put("description", embed.description);
               jsonEmbed.put("url", embed.url);
               if (embed.color != null) {
                  Color color = embed.color;
                  jsonEmbed.put("color", ((color.getRed() << 8) + color.getGreen() << 8) + color.getBlue());
               }

               Footer footer = embed.footer;
               Image image = embed.image;
               Thumbnail thumbnail = embed.thumbnail;
               Author author = embed.author;
               List<Field> fields = embed.fields;
               if (footer != null) {
                  JSONObject jsonFooter = new JSONObject();
                  jsonFooter.put("text", footer.text);
                  jsonFooter.put("icon_url", footer.iconUrl);
                  jsonEmbed.put("footer", jsonFooter);
               }

               if (image != null) {
                  JSONObject jsonImage = new JSONObject();
                  jsonImage.put("url", image.url);
                  jsonEmbed.put("image", jsonImage);
               }

               if (thumbnail != null) {
                  JSONObject jsonThumbnail = new JSONObject();
                  jsonThumbnail.put("url", thumbnail.url);
                  jsonEmbed.put("thumbnail", jsonThumbnail);
               }

               if (author != null) {
                  JSONObject jsonAuthor = new JSONObject();
                  jsonAuthor.put("name", author.name);
                  jsonAuthor.put("url", author.url);
                  jsonAuthor.put("icon_url", author.iconUrl);
                  jsonEmbed.put("author", jsonAuthor);
               }

               ArrayList<JSONObject> jsonFields = new ArrayList();

               for(Field field : fields) {
                  JSONObject jsonField = new JSONObject();
                  jsonField.put("name", field.name());
                  jsonField.put("value", field.value());
                  jsonField.put("inline", field.inline());
                  jsonFields.add(jsonField);
               }

               jsonEmbed.put("fields", jsonFields.toArray());
               embedList.add(jsonEmbed);
            }

            jsonSerializer.put("embeds", embedList.toArray());
         }

         URLConnection connection = (new URL(this.webhookUrl)).openConnection();
         connection.addRequestProperty("Content-Type", "application/json");
         connection.addRequestProperty("User-Agent", "YourLocalLinuxUser");
         connection.setDoOutput(true);
         ((HttpsURLConnection)connection).setRequestMethod("POST");
         OutputStream outputStream = connection.getOutputStream();
         outputStream.write(jsonSerializer.toString().getBytes(StandardCharsets.UTF_8));
         outputStream.flush();
         outputStream.close();
         connection.getInputStream().close();
         ((HttpsURLConnection)connection).disconnect();
      }
   }

   static class JSONObject {
      private final HashMap data = new HashMap();

      void put(String key, Object value) {
         if (value != null) {
            this.data.put(key, value);
         }

      }

      public String toString() {
         StringBuilder stringBuilder = new StringBuilder();
         Set<Map.Entry<String, Object>> entrySet = this.data.entrySet();
         stringBuilder.append("{");
         int count = 0;

         for(Map.Entry entry : entrySet) {
            Object value = entry.getValue();
            stringBuilder.append(this.escapeString((String)entry.getKey())).append(":");
            if (value instanceof String) {
               stringBuilder.append(this.escapeString(String.valueOf(value)));
            } else if (value instanceof Integer) {
               stringBuilder.append(Integer.valueOf(String.valueOf(value)));
            } else if (value instanceof Boolean) {
               stringBuilder.append(value);
            } else if (value instanceof JSONObject) {
               stringBuilder.append(value);
            } else if (value.getClass().isArray()) {
               stringBuilder.append("[");
               int length = Array.getLength(value);

               for(int i = 0; i < length; ++i) {
                  StringBuilder append = stringBuilder.append(Array.get(value, i).toString());
                  String separator;
                  if (i != length - 1) {
                     separator = ",";
                  } else {
                     separator = "";
                  }

                  append.append(separator);
               }

               stringBuilder.append("]");
            }

            ++count;
            stringBuilder.append(count == entrySet.size() ? "}" : ",");
         }

         return stringBuilder.toString();
      }

      private String escapeString(String str) {
         return "\"" + str;
      }
   }

   public static class EmbedObject {
      public String title;
      public String description;
      public String url;
      public Color color;
      public Footer footer;
      public Thumbnail thumbnail;
      public Image image;
      public Author author;
      public final List fields = new ArrayList();

      public EmbedObject setDescription(String description) {
         this.description = description;
         return this;
      }

      public EmbedObject setColor(Color color) {
         this.color = color;
         return this;
      }

      public EmbedObject setTitle(String title) {
         this.title = title;
         return this;
      }

      public EmbedObject setUrl(String url) {
         this.url = url;
         return this;
      }

      public EmbedObject setFooter(String text, String iconUrl) {
         this.footer = new Footer(text, iconUrl);
         return this;
      }

      public EmbedObject setImage(Image image) {
         this.image = image;
         return this;
      }

      public EmbedObject setThumbnail(String url) {
         this.thumbnail = new Thumbnail(url);
         return this;
      }

      public EmbedObject setAuthor(Author author) {
         this.author = author;
         return this;
      }

      public EmbedObject addField(String name, String value, boolean inline) {
         this.fields.add(new Field(name, value, inline));
         return this;
      }
   }

   static record Image(String url) {
   }

   static record Footer(String text, String iconUrl) {
   }

   static record Field(String name, String value, boolean inline) {
   }

   static record Author(String name, String url, String iconUrl) {
   }

   static record Thumbnail(String url) {
   }
}
