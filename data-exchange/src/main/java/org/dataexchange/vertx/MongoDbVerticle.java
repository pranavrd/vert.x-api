package org.dataexchange.vertx;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

public class MongoDbVerticle extends AbstractVerticle {

    MongoClient mongoClient;

    private void getDataInstanceHandler(Message msg) {
       JsonObject query = (JsonObject) msg.body();
       mongoClient.find("coll", query, res -> {
           if(res.succeeded() && res.result().size() > 0){
               for (JsonObject data : res.result()){
                   msg.reply(data);
               }
           } else {
               msg.reply("IoT data with matching id not found");
           }
       });
    }

    private void postDataInstanceHandler(Message msg) {
        mongoClient.insert("coll", (JsonObject) msg.body(), res -> {
            if (res.succeeded()) {
                msg.reply("success");
            } else {
                msg.reply("failure");
                res.cause().printStackTrace();
            }
        });
    }

    private void putDataInstanceHandler(Message msg) {
       JsonArray messageArray = (JsonArray) msg.body();
       mongoClient.updateCollection("coll", messageArray.getJsonObject(0), messageArray.getJsonObject(1), res -> {
          if (res.succeeded()) {
              msg.reply("success");
          } else {
              msg.reply("failure");
              res.cause().printStackTrace();
          }
       });
    }

    private void deleteInstanceHandler(Message msg) {
        mongoClient.findOneAndDelete("coll", (JsonObject) msg.body(), res -> {
            if (res.succeeded() && res.result() != null) {
                msg.reply("success");
            } else {
                msg.reply("failure");
                res.cause().printStackTrace();
            }
        });
    }

    @Override
    public void start() {
        final String uri = "mongodb://localhost:27017";
        final String db = "iot-data";

        final JsonObject mongoconfig = new JsonObject()
          .put("connection_string", uri)
          .put("db_name", db);

        mongoClient = MongoClient.create(vertx, mongoconfig);

        vertx.eventBus().consumer("get.data.addr", this::getDataInstanceHandler);

        vertx.eventBus().consumer("post.data.addr", this::postDataInstanceHandler);

        vertx.eventBus().consumer("put.data.addr", this::putDataInstanceHandler);

        vertx.eventBus().consumer("delete.data.addr", this::deleteInstanceHandler);

    }
}
