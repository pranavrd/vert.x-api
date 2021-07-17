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
                .handler(ctx -> {
                    String id = ctx.request().getParam("id");
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
//            response.putHeader("content-type","text/plain");
            response.write("Read data instance with id: "+id+"\n\n");
            ctx.response().end();
//            ctx.vertx().setTimer(5000, tid -> ctx.next());
        });

        router
                .post("/server")
                .consumes("*/json")
                .handler(ctx -> {
//                    String id = ctx.request().getParam("id");
            HttpServerResponse response = ctx.response();
            response.setChunked(true);
            response.write("Created new data instance with id: \n\n");
            ctx.response().end();
//            ctx.vertx().setTimer(5000, tid -> ctx.next());
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
//            ctx.vertx().setTimer(5000, tid -> ctx.next());
            ctx.response().end();
        });

        httpServer
                .requestHandler(router)
                .listen(8881);
    }
}
