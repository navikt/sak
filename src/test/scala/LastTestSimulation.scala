import io.gatling.core.Predef._
import io.gatling.http.Predef._
import no.nav.sak.infrastruktur.GatlingJDBCCleaner
import no.nav.sak.infrastruktur.oicd.OidcLogin
import no.nav.sak.infrastruktur.sts.STSSupport

import scala.concurrent.duration._

class LastTestSimulation extends Simulation {

    private val authHeaderOidc = "Bearer " + new OidcLogin().getIdToken
    private val authHeaderSaml = "Saml " + new STSSupport().getSystemSAMLTokenFromSTS

    private val httpProtocol = http.baseURL("https://sak-t8.nais.preprod.local/api/v1").warmUp("http://confluence.adeo.no")

    private val orgnrFeeder = csv("data/orgnr.csv").circular
    private val temaFeeder = csv("data/tema.csv").circular
    private val applikasjonFeeder = csv("data/applikasjon.csv").circular

    before {
        new GatlingJDBCCleaner().resetState()
    }


    private val opprettOgHentSakScenario = scenario("Opprett og hent sak")
        .group("OIDC") {
            feed(temaFeeder)
                .feed(orgnrFeeder)
                .feed(applikasjonFeeder)
                .exec(
                    SakTests.createSak(authHeaderOidc),
                    SakTests.getSak(authHeaderOidc)
                )
        }
        .group("SAML") {
            feed(temaFeeder)
                .feed(orgnrFeeder)
                .feed(applikasjonFeeder)
                .exec(
                    SakTests.createSak(authHeaderSaml),
                    SakTests.getSak(authHeaderSaml)
                )
        }

    private val soekSakerScenario = scenario("Søk etter saker")
        .group("OIDC") {
            feed(temaFeeder)
                .feed(orgnrFeeder)
                .feed(applikasjonFeeder)
                .exec(SakTests.searchSaker(authHeaderOidc))
        }
        .group("SAML") {
            feed(temaFeeder)
                .feed(orgnrFeeder)
                .feed(applikasjonFeeder)
                .exec(SakTests.searchSaker(authHeaderSaml))
        }

    setUp(opprettOgHentSakScenario.inject(constantUsersPerSec(2) during (2 minutes))
        .protocols(httpProtocol),
        soekSakerScenario.inject(constantUsersPerSec(40) during (5 minutes))
            .protocols(httpProtocol)).assertions(
        global.successfulRequests.percent.gt(99)
    )

}
