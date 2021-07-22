package org.dataexchange.vertx;

import io.vertx.core.*;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class App extends AbstractVerticle
{
    private final String Secret = "NPd6Q176avia-6oPbJ_jUITPrNEMTOEfUd7PYxZkSrY";
    HttpServerResponse response;

    private void getDataInstanceHandler(RoutingContext ctx){
        response = ctx.response();
        JsonObject query = new JsonObject()
            .put("_id", ctx.request().getParam("id"));
        if(query.getString("_id") == null) {
            response.setStatusCode(400).end("id Required");
            return;
        }

        vertx.eventBus().request("get.data.addr", query, reply -> {
            if(reply.result().body() instanceof JsonObject){
                JsonObject data = (JsonObject) reply.result().body();
                ctx.response().end(data.encodePrettily());
            } else {
                response.setStatusCode(404).end("IoT data with matching id not found");
            }
        });
    }

    private void postDataInstanceHandler(RoutingContext ctx){
        response = ctx.response();
        JsonObject newData = ctx.getBodyAsJson();
        if(newData == null) {
            response.setStatusCode(400).end("request body required");
            return;
        } else {
            if(LocalDateTime.parse(newData.getString("observationDateTime"), DateTimeFormatter.ISO_DATE_TIME).isAfter(LocalDateTime.now())) {
                response.setStatusCode(400).end("observation date, time cannot be in the future");
                return;
            }
            if(newData.getDouble("currentLevel") < 0 || newData.getDouble("referenceLevel") < 0 || newData.getDouble("measuredDistance") < 0) {
                response.setStatusCode(400).end("current/reference levels or measured distance cannot be negative values");
                return;
            }
        }

        vertx.eventBus().request("post.data.addr", newData, reply -> {
            if(reply.result().body()!=null && !("failure".equals(reply.result().body()))) {
                ctx.response().end("Created new data instance with id: "+reply.result().body()+"\n\n");
            }
            else if(reply.result().body() == null) {
                ctx.response().end("Created new data instance with id: "+newData.getString("_id")+"\n\n");
            }
            else {
               response.setStatusCode(409).end("Object already exists");
            }
        });
    }

    private void putDataInstanceHandler(RoutingContext ctx){
        JsonObject query = new JsonObject()
            .put("_id", ctx.request().getParam("id"));
        response = ctx.response();
        if(query.getString("_id") == null) {
            response.setStatusCode(400).end("id Required");
            return;
        }

        JsonObject requestBody = ctx.getBodyAsJson();
        JsonObject update = new JsonObject().put("$set", requestBody);
        if(update == null) {
            response.setStatusCode(400).end("request body required");
            return;
        } else {
            if(LocalDateTime.parse(requestBody.getString("observationDateTime"), DateTimeFormatter.ISO_DATE_TIME).isAfter(LocalDateTime.now())) {
                response.setStatusCode(400).end("observation date/time cannot be in the future");
                return;
            }
            if(requestBody.getDouble("currentLevel") < 0 || requestBody.getDouble("referenceLevel") < 0 || requestBody.getDouble("measuredDistance") < 0) {
                response.setStatusCode(400).end("current/reference levels or measured distance cannot be negative values");
                return;
            }
        }

        JsonArray messageArray = new JsonArray()
                .add(0,query)
                .add(1, update);

        vertx.eventBus().request("put.data.addr", messageArray, reply -> {
            if("success".equals(reply.result().body())) {
                ctx.response().end("Updated data instance with id: " + query.getString("_id")+"\n\n");
            } else {
                response.setStatusCode(500).end("Something went wrong");
            }
        });
    }

    private void deleteDataInstanceHandler(RoutingContext ctx){
        JsonObject query = new JsonObject()
            .put("_id", ctx.request().getParam("id"));
        response = ctx.response();
        if(query.getString("_id") == null) {
            response.setStatusCode(400).end("id Required");
            return;
        }
        vertx.eventBus().request("delete.data.addr", query, reply ->{
           if("success".equals(reply.result().body())){
                ctx.response().end("Data instance with id: " + query.getString("_id") + " deleted\n\n");
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

    @Override
    public void start(Promise<Void> promise) throws Exception {
        vertx.deployVerticle(new MongoDbVerticle());
        Future<Void> ft = serverStart();
        ft.onComplete(promise);
    }
}
