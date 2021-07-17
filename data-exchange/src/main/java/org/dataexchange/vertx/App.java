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
import io.vertx.ext.web.handler.BodyHandler;

public class App 
{
    public static void main( String[] args )
    {
        JsonArray data = new JsonArray()
                .add(new JsonObject()
                    .put("currentLevel", 0.57)
                    .put("id", "04a15c9960ffda227e9546f3f46e629e1fe4132b")
                    .put("observationDateTime", "2021-05-02T20:45:00+05:30")
                    .put("measuredDistance", 9.43)
                    .put("referenceLevel", 10.0)
                );
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);

        // BodyHandler added to router to ensure http request body gets parsed... otherwise ctx.request.getBodyAsJson() will always return null
        router.route().handler(BodyHandler.create());
        router
                .get("/server/:id")
                .produces("application/json")
                .handler(ctx -> {
                    String id = ctx.request().getParam("id");
                    HttpServerResponse response = ctx.response();
                    response.setChunked(true);
                    for(int i = 0; i < data.size(); i++){
                        if(id.equals(data.getJsonObject(i).getString("id")))
                            response.write(String.valueOf(data.getJsonObject(i)));
                        else
                            response.write("Data instance with id: "+id+" NOT FOUND\n\n");
                    }
            ctx.response().end();
        });

        router
                .post("/server")
                .consumes("*/json")
                .handler(ctx -> {
                    JsonObject newData = ctx.getBodyAsJson();
                    data.add(newData);
                    System.out.println(data);
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
            response.write("Created new data instance with id: "+newData.getString("id")+"\n\n");
            ctx.response().end();
        });

        //FIXME: performs a post instead of put
        router
                .put("/server/:id")
                .consumes("*/json")
                .handler(ctx -> {
                    String id = ctx.request().getParam("id");
                    JsonObject updateData = ctx.getBodyAsJson();
                    data.add(updateData);
                    System.out.println(data);
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
            response.write("Data instance with id: "+id+" updated\n\n");
            ctx.response().end();
        });

        // FIXME: shows data instance deleted even with no matching data
        router
                .delete("/server/:id")
                .handler(ctx -> {
                    HttpServerResponse response = ctx.response();
                    response.setChunked(true);
                    String id = ctx.request().getParam("id");
                    for(int i = 0; i < data.size(); i++) {
                        if (id.equals(data.getJsonObject(i).getString("id")))
                            data.remove(data.getJsonObject(i));
                    }
                    response.write("Data instance with id: "+id+" deleted\n\n");
                    ctx.response().end();
                    System.out.println(data);
        });

        httpServer
                .requestHandler(router)
                .listen(8881);
    }
}
