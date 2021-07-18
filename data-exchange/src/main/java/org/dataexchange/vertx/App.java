package org.dataexchange.vertx;

/**
 * Hello world!
 *
 */

import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import  io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class App
{
    private JsonArray data;
    private String Secret = "NPd6Q176avia-6oPbJ_jUITPrNEMTOEfUd7PYxZkSrY";

    Vertx vertx = Vertx.vertx();
    HttpServer httpServer = vertx.createHttpServer();

    Router router = Router.router(vertx);

    String uri = "mongodb://localhost:27017";
    String db = "iot-data";

    JsonObject mongoconfig = new JsonObject()
      .put("connection_string", uri)
      .put("db_name", db);

    MongoClient mongoClient = MongoClient.create(vertx, mongoconfig);

    App() {
        data = new JsonArray();
    }

    public void getDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("_id", id);
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end("id Required");
        mongoClient.find("coll", query, res -> {
           if(res.succeeded()){
               for (JsonObject data : res.result()){
                   ctx.response().end(data.encodePrettily());
               }
           } else {
               response.setStatusCode(404).end("IoT data with matching id not found");
           }
        });
    }

    public void postDataInstanceHandler(RoutingContext ctx){
        JsonObject newData = ctx.getBodyAsJson();
        HttpServerResponse response = ctx.response();
        if(newData == null)
            response.setStatusCode(400).end("request body required");

        mongoClient.insert("coll", newData, res -> {
            if (res.succeeded()) {
                System.out.println("Success");
                ctx.response().end("Created new data instance with id: "+newData.getString("_id")+"\n\n");
            } else {
                res.cause().printStackTrace();
               response.setStatusCode(409).end("Object already exists");
            }
        });
    }

    public void putDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("_id", id);
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end("id Required");

        JsonObject updateData = ctx.getBodyAsJson();
        JsonObject update = new JsonObject().put("$set", updateData);
        if(updateData == null)
            response.setStatusCode(400).end("request body required");

        mongoClient.updateCollection("coll", query, update, res -> {
            if (res.succeeded()) {
                System.out.println("Success");
                ctx.response().end("Updated data instance with id: "+updateData.getString("_id")+"\n\n");
            } else {
                res.cause().printStackTrace();
                response.setStatusCode(500).end("Something went wrong");
            }
        });
    }

    // FIXME: showing delete successful even on non-occurance of delete i.e. res.succeeded() is always returning true
    public void deleteDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("_id", id);
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end("id Required");

        mongoClient.findOneAndDelete("coll", query, res -> {
           if(res.succeeded()){
                ctx.response().end("Data instance with id: " + id + " deleted\n\n");
           } else {
            response.setStatusCode(404).end("IoT data with matching id can't be deleted or does not exist");
           }
        });
    }

    public void serverStart(){

        // BodyHandler added to router to ensure http request body gets parsed... otherwise ctx.request.getBodyAsJson() will always return null
        router.route().handler(BodyHandler.create());
        // FIND: use of session handler and local session store
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        // Basic Auth Token implementation
        router.route().handler(ctx -> {
            String AuthToken = ctx.request().getHeader("AUTH_TOKEN");
            if(AuthToken!=null && AuthToken.equals(Secret))
                ctx.next();
            else
                ctx.response().setStatusCode(401).setStatusMessage("UNAUTHORIZED").end();
        });

//        router.route("/").handler(routingContext -> {
//          routingContext.response().putHeader("content-type", "text/html").end(
//              "<form action=\"/server\" method=\"get\">\n" +
//              "    <div>\n" +
//              "        <label for=\"name\">Get IoT data based on id :</label>\n" +
//              "        <input type=\"text\" id=\"id\" name=\"id\" />\n" +
//              "    </div>\n" +
//              "    <div class=\"button\">\n" +
//              "        <button type=\"submit\">READ</button>\n" +
//              "    </div>" +
//              "</form>"
//          );
//        });


        router
                .get("/server/:id")
                .produces("application/json")
                .handler(this::getDataInstanceHandler);

        router
                .post("/server")
                .consumes("*/json")
                .handler(this::postDataInstanceHandler);

        router
                .put("/server/:id")
                .consumes("*/json")
                .handler(this::putDataInstanceHandler);

        router
                .delete("/server/:id")
                .handler(this::deleteDataInstanceHandler);

        httpServer
                .requestHandler(router)
                .listen(8881);

    }

    public static void main( String[] args )
    {
        App appInstance = new App();
        appInstance.data
                .add(new JsonObject()
                    .put("currentLevel", 0.57)
                    .put("id", "04a15c9960ffda227e9546f3f46e629e1fe4132b")
                    .put("observationDateTime", "2021-05-02T20:45:00+05:30")
                    .put("measuredDistance", 9.43)
                    .put("referenceLevel", 10.0)
                );
        appInstance.serverStart();
    }
}
