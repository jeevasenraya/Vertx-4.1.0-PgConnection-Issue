import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.client.WebClient;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.rxjava.pgclient.PgPool;
import io.vertx.sqlclient.PoolOptions;

public class Reproducer extends AbstractVerticle {
    private PgPool pgPool;

    @Override
    public void start(Promise<Void> startPromise) {
        JsonObject pgConnectionConfig = new JsonObject()
                .put("port", 5444)
                .put("host", "localhost")
                .put("database", "")
                .put("user", "")
                .put("password", "");
        pgPool = new PgPool(io.vertx.pgclient.PgPool.pool(vertx, new PgConnectOptions(pgConnectionConfig), new PoolOptions().setMaxSize(10)));
        createRestEndPoint(startPromise);
        createWebClient();
    }

    private void createWebClient() {
        WebClient webClient = WebClient.create(vertx);

        vertx.setPeriodic(1000, handler->{
            webClient.getAbs("http://localhost:8080/").send().onComplete(result -> {
                if(result.succeeded()){
                    System.out.println(result.result().body().toString());
                }else{
                    System.out.println(result.cause().toString());
                }

            });
        });
    }

    private void createRestEndPoint(Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route("/").handler(routingContext -> {
            HttpServerResponse response = routingContext.response();
            pgPool.preparedQuery("select count(*) as count from pg_stat_activity where application_name = 'vertx-pg-client'")
                    .rxExecute().subscribe(result -> response
                            .putHeader("content-type", "text/html")
                            .end("Vertx-pg-client connections: " + result.iterator().next().getInteger("count")),
                    error -> System.out.println("Failed to get application connections " + error));
        });

        vertx.createHttpServer()
                .requestHandler(router::handle)
                .listen(config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                startPromise.complete();
                            } else {
                                startPromise.fail(result.cause());
                            }
                        }
                );
    }

    @Override
    public void stop(Promise<Void> stopPromise) throws Exception {
        pgPool.close();
        super.stop(stopPromise);
    }
}
