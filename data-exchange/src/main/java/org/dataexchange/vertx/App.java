package org.dataexchange.vertx;

/**
 * Hello world!
 *
 */

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Route;
import io.vertx.ext.web.Router;

public class App 
{
    public static void main( String[] args )
    {
        JsonObject sampleData = new JsonObject();
        sampleData.put("currentLevel", 0.57);
        sampleData.put("id", "04a15c9960ffda227e9546f3f46e629e1fe4132b");
        sampleData.put("observationDateTime", "2021-05-02T20:45:00+05:30");
        sampleData.put("measuredDistance", 9.43);
        sampleData.put("referenceLevel", 10.0);
        Vertx vertx = Vertx.vertx();
        HttpServer httpServer = vertx.createHttpServer();

        Router router = Router.router(vertx);
        Route route = router.route("/here");
        router
                .get("/path")
                .respond(
                        ctx -> Future.succeededFuture(new JsonObject().put("here","1")));
        router
                .get("/server/:id")
                .produces("application/json")
                .handler(ctx -> {
                    String id = ctx.request().getParam("id");
                    HttpServerResponse response = ctx.response();
                    response.setChunked(true);
                    if(id.equals(sampleData.getString("id")))
                        response.write(String.valueOf(sampleData));
                    else
                        response.write("Data instance with id: "+id+" NOT FOUND\n\n");
            ctx.response().end();
        });

        router
                .post("/server")
                .consumes("*/json")
                .handler(ctx -> {
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
            response.write("Created new data instance with id: \n\n");
            ctx.response().end();
        });

        router
                .put("/server/:id")
                .consumes("*/json")
                .handler(ctx -> {
                    String id = ctx.request().getParam("id");
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
            response.write("Data instance with id: "+id+" updated\n\n");
            ctx.response().end();
        });

        router
                .delete("/server/:id")
                .handler(ctx -> {
                    String id = ctx.request().getParam("id");
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
            response.write("Data instance with id: "+id+" deleted\n\n");
            ctx.response().end();
        });

        httpServer
                .requestHandler(router)
                .listen(8881);
    }
}
