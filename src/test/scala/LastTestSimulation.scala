import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sak.infrastruktur.GatlingJDBCCleaner
import no.nav.sak.infrastruktur.oicd.OidcLogin
import no.nav.sak.infrastruktur.sts.STSSupport
import no.nav.sak.infrastruktur.basic.BasicAuthTestHeaderProvider

import scala.concurrent.duration._

class LastTestSimulation extends Simulation {

    private val authHeaderOidc = "Bearer " + new OidcLogin().getIdToken
    private val authHeaderSaml = "Saml " + new STSSupport().getSystemSAMLTokenFromSTS
    private val authHeaderBasic = new BasicAuthTestHeaderProvider().getHeader

    private val httpProtocol = http.baseURL("https://sak-t8.nais.preprod.local/api/v1").warmUp("https://sak-t8.nais.preprod.local/internal/alive")

    private val aktoerIdFeeder = csv("data/aktoerId.csv").circular
    private val temaFeeder = csv("data/tema.csv").circular
    private val applikasjonFeeder = csv("data/applikasjon.csv").circular

    before {
        new GatlingJDBCCleaner().resetState()
    }

    private val opprettOgHentSakScenario = scenario("Opprett og hent sak")
        .group("OIDC") {
            feed(temaFeeder)
                .feed(aktoerIdFeeder)
                .feed(applikasjonFeeder)
                .exec(
                    SakTests.createSak(authHeaderOidc),
                    SakTests.getSak(authHeaderOidc)
                )
        }
        .group("SAML") {
            feed(temaFeeder)
                .feed(aktoerIdFeeder)
                .feed(applikasjonFeeder)
                .exec(
                    SakTests.createSak(authHeaderSaml),
                    SakTests.getSak(authHeaderSaml)
                )
        }
        .group("BASIC") {
            feed(temaFeeder)
                .feed(aktoerIdFeeder)
                .feed(applikasjonFeeder)
                .exec(
                    SakTests.createSak(authHeaderBasic),
                    SakTests.getSak(authHeaderBasic)
                )
        }

    private val soekSakerScenario = scenario("Søk etter saker")
        .group("OIDC") {
            feed(temaFeeder)
                .feed(aktoerIdFeeder)
                .feed(applikasjonFeeder)
                .exec(SakTests.searchSaker(authHeaderOidc))
        }
        .group("SAML") {
            feed(temaFeeder)
                .feed(aktoerIdFeeder)
                .feed(applikasjonFeeder)
                .exec(SakTests.searchSaker(authHeaderSaml))
        }
        .group("BASIC") {
            feed(temaFeeder)
                .feed(aktoerIdFeeder)
                .feed(applikasjonFeeder)
                .exec(SakTests.searchSaker(authHeaderBasic))
        }

    setUp(opprettOgHentSakScenario.inject(constantUsersPerSec(2) during (2 minutes))
        .protocols(httpProtocol),
        soekSakerScenario.inject(constantUsersPerSec(10) during (5 minutes))
            .protocols(httpProtocol)).assertions(
        global.successfulRequests.percent.gt(99)
    )

}
