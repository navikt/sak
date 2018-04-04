import io.gatling.core.Predef._
import io.gatling.http.Predef._

object SakTests {

    val createSak = (authHeaderValue: String) => exec(
        http("createSak")
            .post("/saker")
            .header("Content-Type", "application/json")
            .header("Authorization", authHeaderValue)
            .header("X-Correlation-ID", "gatling")
            .body(ElFileBody("bodies/create_sak_request.json"))
            .asJSON
            .check(jsonPath("$.id").saveAs("sakId"))
            .check(status is 201)
    )

    val getSak = (authHeaderValue: String) => exec(
        http("getSak")
            .get("/saker/${sakId}")
            .header("Content-Type", "application/json")
            .header("Authorization", authHeaderValue)
            .header("X-Correlation-ID", "gatling")
            .asJSON
            .check(status is 200)
            .check(jsonPath("$.id"))

    )

    val searchSaker = (authHeaderValue: String) => exec(
        http("searchSaker")
            .get("/saker?orgnr=${orgnr}")
            .header("Content-Type", "application/json")
            .header("Authorization", authHeaderValue)
            .header("X-Correlation-ID", "gatling")
            .asJSON
            .check(status is 200)
    )
}
