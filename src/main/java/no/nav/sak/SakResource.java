package no.nav.sak;

import io.swagger.annotations.*;
import no.nav.sak.infrastruktur.EnableApiFilters;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sak.infrastruktur.abac.AuthorizationRequest;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sikkerhet.abac.ABACResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation.HEADER;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static no.nav.sak.infrastruktur.ContextExtractor.getSubjectType;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@EnableApiFilters
@Path("/v1/saker")
@Api(value = "v1/saker", authorizations = {
    @Authorization(value = "Bearer"),
    @Authorization(value = "Saml"),
    @Authorization(value = "Basic")
})
@SwaggerDefinition(
    info = @Info(
        title = "Sak API",
        version = "1",
        description = "Her dokumenteres tjenestegrensesnittet for Sak\n\n. Tjenesten leveres kontinuerlig til produksjon. For å sikre oss mot å innføre regresjon som påvirker våre konsumenter, benytter vi " +
            " Pact. Det er konsumentens ansvar å gi oss pact-test, men ta gjerne kontakt ved behov for bistand ifm. dette." +
            " Vi ber nye konsumenter om å ta kontakt med teamet, dette for å få gjennomført ev. avklaringer, sikre korrekte tilganger, pact-test, og for å sikre at tjenesten støtter " +
            " forventet volum og ev. SLA",
        contact = @Contact(
            name = "Team Gosys"
        ))
    , securityDefinition =
@SecurityDefinition(apiKeyAuthDefinitions = {
    @ApiKeyAuthDefinition(
        name = "Authorization",
        key = "Bearer",
        in = HEADER,
        description = "OIDC-token (JWT via OAuth2.0). Dette preferert autentiseringsmekanisme, og <strong>skal</strong>" +
            " benyttes ved tjenestekall initiert av en bruker for å propagere konteksten (unntatt i særtilfeller - se Saml) \n" +
            " Følgende format må brukes i input-feltet \"Value\" under: <strong>\"Bearer {token}\"</strong>.\n" +
            " Eksempel på verdi i input-felt: <strong>Bearer eYdmifml0ejugm</strong>\n\n" +
            " Et gyldig token kommer til å ha mange flere karakterer enn i eksempelet."),

    @ApiKeyAuthDefinition(
        name = "Authorization",
        key = "Saml",
        in = HEADER,
        description = "P.t støttes ikke konvertering fra SAML til OIDC-token og det er derfor implementert støtte for Saml for å propagere brukercontext fra legacy-systemer " +
            " (i.e. fra et system som kun eksponerer soap-tjenester og som skal gjøre tjenestekall videre mot Oppgave.\n" +
            " I denne konteksten er et SAML token en SAML assertion som er Base 64 enkodet. \n" +
            " På grunn av begrensninger i header-lengde, må saml-assertion strippes for whitespaces før den encodes \n" +
            " Formatet skal være som følger: <strong>\"Saml {token}\"</strong>.\n" +
            " Eksempel på verdi i input-felt: <strong>Saml eYdmifml0ejugm</strong>\n\n" +
            " Et gyldig token kommer til å ha mange flere karakterer enn i eksempelet.")
},
    basicAuthDefinitions = {
        @BasicAuthDefinition(
            key = "Basic",
            description = "Basic auth kan brukes når det er snakk om system-til-system kommunikasjon")
    }
)
)
public class SakResource {
    private static final Logger log = LoggerFactory.getLogger(SakResource.class);
    private final SakRepository sakRepository;
    private final SakPEP sakPEP;
    private SakConfiguration sakConfiguration;

    SakResource(SakRepository sakRepository, SakPEP sakPEP, SakConfiguration sakConfiguration) {
        this.sakRepository = sakRepository;
        this.sakPEP = sakPEP;
        this.sakConfiguration = sakConfiguration;
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Henter sak for en gitt id", response = SakJson.class)
    @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true, dataType = "string", paramType = "header")})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 401, message = "Konsument mangler gyldig token"),
        @ApiResponse(code = 403, message = "Konsument har ikke tilgang til å gjennomføre handlingen"),
        @ApiResponse(code = 404, message = "Det finnes ingen sak for angitt id"),
        @ApiResponse(code = 500, message = "Ukjent feilsituasjon har oppstått i Sak")
    }
    )
    public Response hentSak(@PathParam("id") Long id, @Context ContainerRequestContext ctx) {
        log.info("Henter sak med id: {}", id);
        Optional<Sak> sak = sakRepository.hentSak(id);

        if (!sak.isPresent()) {
            log.warn("Mottatt oppslag på sak som ikke eksisterer, id: {}, consumer: {}", id, ctx.getProperty(REQUEST_CONSUMERID));
            return Response.status(NOT_FOUND).entity(
                new ErrorResponse(MDC.get("uuid"), String.format("Fant ingen sak med id: %s", id))
            ).build();
        }

        Sak eksisterendeSak = sak.get();
        ABACResult abacResult = sakPEP.autoriser(ctx, new AuthorizationRequest(eksisterendeSak.getAktoerId(), eksisterendeSak.getTema()));
        if (!abacResult.hasAccess()) {
            String user = (String) ctx.getProperty(REQUEST_USERNAME);
            log.warn("Autorisering feilet for: {}", user);
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(MDC.get("uuid"), "Bruker kunne ikke autoriseres for denne operasjonen"))
                .build();
        } else {
            return Response.ok(new SakJson(eksisterendeSak)).build();
        }
    }


    @GET
    @ApiOperation(value = "Finner saker for angitte søkekriterier",
        response = SakJson.class, responseContainer = "List")
    @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true, dataType = "string", paramType = "header")})
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Ugyldig input"),
        @ApiResponse(code = 401, message = "Konsument mangler gyldig token"),
        @ApiResponse(code = 500, message = "Ukjent feilsituasjon har oppstått i Sak")
    }
    )
    public Response finnSaker(@Valid @BeanParam SakSearchRequest sakSearchRequest, @Context ContainerRequestContext ctx) {
        log.info("Søker etter saker for: {}", sakSearchRequest);
        if(sakConfiguration.getBoolean("ABAC_ENABLED_TEMA", true)) {
            List<Sak> saker = sakRepository.finnSaker(sakSearchRequest.toCriteria());
            return Response.ok(
                saker.stream()
                    .filter(s -> sakPEP.autoriser(ctx, new AuthorizationRequest(s.getAktoerId(), s.getTema())).hasAccess())
                    .map(SakJson::new)
                    .collect(toList()))
                .build();
        } else {
            if(!sakPEP.autoriser(ctx, new AuthorizationRequest(sakSearchRequest.getAktoerId(), sakSearchRequest.getTema())).hasAccess()) {
                return Response.ok(new ArrayList<>()).build();
            }
            List<Sak> saker = sakRepository.finnSaker(sakSearchRequest.toCriteria());
            return Response.ok(
                saker.stream()
                    .filter(s -> harTilgangTilSakInterneRegler(ctx, s))
                    .map(SakJson::new)
                    .collect(toList()))
                .build();
        }
    }

    private boolean harTilgangTilSakInterneRegler(ContainerRequestContext ctx, Sak sak) {
        boolean temaKontroll = Objects.equals("KTR", sak.getTema());
        boolean harTilgang = !(temaKontroll && Objects.equals(getSubjectType(ctx), SUBJECT_TYPE_EKSTERNBRUKER));
        if(!harTilgang) {
            log.info("Filtrerer ut sak for ekstern bruker: {} fordi den har tema {} ", sak.getId(), sak.getTema());
        }
        return harTilgang;
    }


    @POST
    @ApiOperation(value = "Oppretter en ny sak", notes = "Merk at en sak enten skal tilhøre en aktør <b>eller</b> et foretak. Begge er p.t. ikke tillatt. ")
    @ApiImplicitParams({@ApiImplicitParam(name = "X-Correlation-ID", required = true, dataType = "string", paramType = "header")})
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Saken er opprettet", responseHeaders = @ResponseHeader(name = "location", description = "Angir URI til den opprettede saken")),
        @ApiResponse(code = 400, message = "Ugyldig input"),
        @ApiResponse(code = 401, message = "Konsument mangler gyldig token"),
        @ApiResponse(code = 403, message = "Konsument har ikke tilgang til å gjennomføre handlingen"),
        @ApiResponse(code = 409, message = "Det finnes allerede en sak for angitt kombinasjon av fagsaknr og applikasjon for aktør eller orgnr"),
        @ApiResponse(code = 500, message = "Ukjent feilsituasjon har oppstått i Sak")
    }
    )
    public Response opprettSak(@Valid
                               @ApiParam(value = "Saken som skal opprettes", required = true) SakJson sakJson, @Context UriInfo uriInfo, @Context ContainerRequestContext ctx) throws URISyntaxException {
        String user = (String) ctx.getProperty(REQUEST_USERNAME);
        Sak innsendtSak = sakJson.toSak(user);
        ABACResult abacResult = sakPEP.autoriser(ctx, new AuthorizationRequest(innsendtSak.getAktoerId(), innsendtSak.getTema()));
        if (!abacResult.hasAccess()) {
            log.warn("Autorisering feilet for: {}", user);
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(MDC.get("uuid"), "Bruker kunne ikke autoriseres for denne operasjonen"))
                .build();
        } else {
            if (fagSakFinnesFraFoer(innsendtSak)) {
                return Response.status(Response.Status.CONFLICT).entity(
                    new ErrorResponse(MDC.get("uuid"), String.format("Det finnes allerede en sak for fagsaksnr: %s, applikasjon: %s, aktør: %s orgnr: %s",
                        innsendtSak.getFagsakNr(),
                        innsendtSak.getApplikasjon(),
                        innsendtSak.getAktoerId(),
                        innsendtSak.getOrgnr())))
                    .build();
            }
            Sak opprettetSak = sakRepository.lagre(innsendtSak);
            log.info("Opprettet: {}", opprettetSak);
            return Response.created(new URI(uriInfo.getAbsolutePathBuilder().path(String.valueOf(opprettetSak.getId())).build().getPath()))
                .entity(new SakJson(opprettetSak))
                .build();
        }
    }

    private boolean fagSakFinnesFraFoer(Sak sak) {
        SakSearchCriteria sakSearchCriteria = SakSearchCriteria.create().medOrgnr(sak.getOrgnr()).medAktoerId(sak.getAktoerId()).medFagsakNr(sak.getFagsakNr()).medApplikasjon(sak.getApplikasjon());
        return sak.getFagsakNr() != null &&
            !sakRepository.finnSaker(sakSearchCriteria).isEmpty();
    }
}
