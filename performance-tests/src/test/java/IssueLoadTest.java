import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class IssueLoadTest extends Simulation {

    HttpProtocolBuilder httpProtocol = http
            .baseUrl("http://localhost:8080")
            .acceptHeader("application/json")
            .contentTypeHeader("application/json");

    ScenarioBuilder issueCreation = scenario("Issue CRUD")
            .exec(
                http("Login")
                    .post("/auth/login")
                    .body(StringBody("{\"username\":\"admin\",\"password\":\"admin\"}"))
                    .check(jsonPath("$.token").saveAs("token"))
            )
            .exec(
                http("List Issues")
                    .get("/api/issues")
                    .header("Authorization", "Bearer #{token}")
                    .check(status().is(200))
            )
            .pause(1)
            .exec(
                http("Search Issues")
                    .get("/search/api/search?query=test")
                    .header("Authorization", "Bearer #{token}")
                    .check(status().is(200))
            );

    {
        setUp(
            issueCreation.injectOpen(
                rampUsers(100).during(30),
                constantUsersPerSec(50).during(60),
                rampUsers(500).during(30)
            )
        ).protocols(httpProtocol)
         .assertions(
            global().responseTime().percentile3().lt(2000),
            global().successfulRequests().percent().gt(99.0)
         );
    }
}
