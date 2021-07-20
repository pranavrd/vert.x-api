package org.dataexchange.vertx;

import io.vertx.core.*;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

public class App extends AbstractVerticle
{
    private final String Secret = "NPd6Q176avia-6oPbJ_jUITPrNEMTOEfUd7PYxZkSrY";

    public void getDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        JsonObject query = new JsonObject()
            .put("_id", id);
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end("id Required");

        vertx.eventBus().request("get.data.addr", query, reply -> {
            if(reply.result().body() instanceof JsonObject){
                JsonObject data = (JsonObject) reply.result().body();
                ctx.response().end(data.encodePrettily());
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

        vertx.eventBus().request("post.data.addr", newData, reply -> {
            if("success".equals(reply.result().body())) {
                ctx.response().end("Created new data instance with id: "+newData.getString("_id")+"\n\n");
            } else {
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

        JsonArray messageArray = new JsonArray()
                .add(0,query)
                .add(1, update);

        vertx.eventBus().request("put.data.addr", messageArray, reply -> {
            if("success".equals(reply.result().body())) {
                ctx.response().end("Updated data instance with id: "+updateData.getString("_id")+"\n\n");
            } else {
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

        vertx.eventBus().request("delete.data.addr", query, reply ->{
           if("success".equals(reply.result().body())){
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
        serverStart();
    }
}
