package org.dataexchange.vertx;

/**
 * Hello world!
 *
 */

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

public class App 
{
    private JsonArray data;

    App() {
        data = new JsonArray();
    }

    public void getDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end();

        for(int i = 0; i < data.size(); i++){
            if(id.equals(data.getJsonObject(i).getString("id")))
                ctx.response().end(String.valueOf(data.getJsonObject(i)));
        }
        response.setStatusCode(404).end();
    }

    public void postDataInstanceHandler(RoutingContext ctx){
        JsonObject newData = ctx.getBodyAsJson();
        HttpServerResponse response = ctx.response();
        if(newData == null)
            response.setStatusCode(400).end();

        data.add(newData);
        ctx.response().end("Created new data instance with id: "+newData.getString("id")+"\n\n");
    }

    // FIXME: performs a post instead of put
    public void putDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end();

        JsonObject updateData = ctx.getBodyAsJson();
        if(updateData == null)
            response.setStatusCode(400).end();

        data.add(updateData);
        ctx.response().end("Data instance with id: "+id+" updated\n\n");
    }

    public void deleteDataInstanceHandler(RoutingContext ctx){
        String id = ctx.request().getParam("id");
        HttpServerResponse response = ctx.response();
        if(id == null)
            response.setStatusCode(400).end();

        for(int i = 0; i < data.size(); i++) {
            if (id.equals(data.getJsonObject(i).getString("id")))
                data.remove(data.getJsonObject(i));
                ctx.response().end("Data instance with id: "+id+" deleted\n\n");
        }
        response.setStatusCode(404).end();
    }

    public void serverStart(){
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);

        // BodyHandler added to router to ensure http request body gets parsed... otherwise ctx.request.getBodyAsJson() will always return null
        router.route().handler(BodyHandler.create());

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
