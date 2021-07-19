package org.dataexchange.vertx;

import io.vertx.core.*;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import  io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class App extends AbstractVerticle
{
    private final String Secret = "NPd6Q176avia-6oPbJ_jUITPrNEMTOEfUd7PYxZkSrY";
    private MongoClient mongoClient;

    public void getDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("_id", id);
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end("id Required");
        mongoClient.find("coll", query, res -> {
           if(res.succeeded() && res.result().size() > 0){
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

    public void deleteDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("_id", id);
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end("id Required");

        mongoClient.findOneAndDelete("coll", query, res -> {
           if(res.succeeded() && res.result()!=null){
                ctx.response().end("Data instance with id: " + id + " deleted\n\n");
           } else {
            response.setStatusCode(404).end("IoT data with matching id can't be deleted or does not exist");
           }
        });
    }

    private Future<Void> serverStart(){
        Promise<Void> promise = Promise.promise();
        Router router = Router.router(vertx);

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

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(8881);

        return promise.future();
    }

    public static void main( String[] args )
    {
        Vertx.vertx().deployVerticle(new App());
    }

    private Future<Void> connectMongoDatabase() {
        Promise<Void> promise = Promise.promise();
        final String uri = "mongodb://localhost:27017";
        final String db = "iot-data";

        final JsonObject mongoconfig = new JsonObject()
          .put("connection_string", uri)
          .put("db_name", db);

        mongoClient = MongoClient.create(vertx, mongoconfig);
        return promise.future();
    }

    @Override
    public void start(Promise<Void> promise) throws Exception {
        // FIXME: future and promise async not working as expected
        connectMongoDatabase();
        serverStart();
//        Future<Void> pipeline = connectMongoDatabase().compose(v -> serverStart());
//        pipeline.onComplete(promise);
    }
}
