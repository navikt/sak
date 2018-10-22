package no.nav.sak;

import static io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation.HEADER;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static no.nav.sak.infrastruktur.ContextExtractor.getSubjectType;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import io.swagger.annotations.*;
import no.nav.sak.infrastruktur.EnableApiFilters;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sak.infrastruktur.abac.AuthorizationRequest;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sikkerhet.abac.ABACResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

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
        description = "Her dokumenteres tjenestegrensesnittet for Sak.\n\n" +
            "Tjenesten leveres kontinuerlig til produksjon. For å sikre oss mot å innføre regresjon som påvirker våre konsumenter, benytter vi " +
            "Pact. Det er konsumentens ansvar å gi oss pact-test, men ta gjerne kontakt ved behov for bistand ifm. dette. \n\n" +
            "Vi ber nye konsumenter om å ta kontakt med teamet, dette for å få gjennomført ev. avklaringer, sikre korrekte tilganger, pact-test, og for å sikre at tjenesten støtter " +
            "forventet volum og ev. SLA.\n\n" +
            "Merk at vi forventer at Headeren <strong>\"X-Correlation-ID\"</strong> er angitt for alle tjenestekall. Denne logges alltid i Sak, og benyttes for å kunne sammenstille hendelser " +
            "på tvers av kallkjeder. X-Correlation-ID skal oppgis ved forespørsel om bistand fra Team Oppgavehåntering vedr. feilsøk ifm. bruk av tjenesten\n" +
            "Vi anbefaler at korrelasjonsID genereres så tidlig som mulig hos konsument, bindes til tråden, og logges sammen med alle hendelser som danner grunnlaget for kallet mot Sak,\n\n" +
            "KorrelasjonsIDen skal være unik, og kan enten genereres med f.eks UUID.randomUUID() eller hvis aktuelt, hentes ut fra inngående tjenestekall (i.e. callId via modig-biblioteket)",
        contact = @Contact(
            name = "Team Oppgavehåndtering"
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

    SakResource(
          final SakRepository sakRepository
        , final SakPEP sakPEP) {

        this.sakRepository = sakRepository;
        this.sakPEP = sakPEP;
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
    public Response hentSak(
          @PathParam("id") final Long id
        , @Context final ContainerRequestContext ctx) {

        log.info("Henter sak med id: {}", id);
        final Optional<Sak> sak = sakRepository.hentSak(id);

        final Response response;
        if (!sak.isPresent()) {
            log.warn("Mottatt oppslag på sak som ikke eksisterer, id: {}, consumer: {}", id, ctx.getProperty(REQUEST_CONSUMERID));
            response = Response.status(NOT_FOUND).entity(
                new ErrorResponse(MDC.get("uuid"), String.format("Fant ingen sak med id: %s", id))
            ).build();
        }
        else {
            final Sak eksisterendeSak = sak.get();
            final ABACResult abacResult = sakPEP.autoriser(ctx, new AuthorizationRequest(eksisterendeSak.getAktoerId()));
            final ABACResult.Code abacResultCode = abacResult.getCode();
            if (ABACResult.Code.OK.equals(abacResultCode)) {
                if (!abacResult.hasAccess()) {
                    response = Response.status(Response.Status.FORBIDDEN)
                        .entity(new ErrorResponse(MDC.get("uuid"), "Bruker kunne ikke autoriseres for denne operasjonen"))
                        .build();
                } else {
                    response = Response.ok(new SakJson(eksisterendeSak)).build();
                }
            } else {
                response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new ErrorResponse(MDC.get("uuid"), abacResultCode.getDescription())
                ).build();
            }
        }

        return response;
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
    public Response finnSaker(
          @Valid @BeanParam final SakSearchRequest sakSearchRequest
        , @Context final ContainerRequestContext ctx) {

        log.info("Søker etter saker for: {}", sakSearchRequest);

        final ABACResult abacResult = sakPEP.autoriser(ctx, new AuthorizationRequest(sakSearchRequest.getAktoerId()));
        final ABACResult.Code abacResultCode = abacResult.getCode();
        final Response response;
        if (ABACResult.Code.OK.equals(abacResultCode)) {
            if (!abacResult.hasAccess()) {
                response = Response.ok(new ArrayList<>()).build();
            } else {
                final List<Sak> saker = sakRepository.finnSaker(sakSearchRequest.toCriteria());
                response = Response.ok(
                    saker.stream()
                        .filter(s -> harTilgangTilSakInterneRegler(ctx, s))
                        .map(SakJson::new)
                        .collect(toList()))
                    .build();
            }
        } else {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                new ErrorResponse(MDC.get("uuid"), abacResultCode.getDescription())
            ).build();
        }

        return response;
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
    public Response opprettSak(
          @Valid @ApiParam(value = "Saken som skal opprettes", required = true) final SakJson sakJson
        , @Context final UriInfo uriInfo
        , @Context final ContainerRequestContext ctx
    ) throws URISyntaxException {

        final String user = (String) ctx.getProperty(REQUEST_USERNAME);
        final Sak innsendtSak = sakJson.toSak(user);
        final String aktoerId = innsendtSak.getAktoerId();

        log.info("Oppretter sak for {}", aktoerId);

        final ABACResult abacResult = sakPEP.autoriser(ctx, new AuthorizationRequest(aktoerId));

        final ABACResult.Code abacResultCode = abacResult.getCode();
        final Response response;
        if (ABACResult.Code.OK.equals(abacResultCode)) {
            if (!abacResult.hasAccess()) {
                response = Response.status(Response.Status.FORBIDDEN)
                    .entity(new ErrorResponse(MDC.get("uuid"), "Bruker kunne ikke autoriseres for denne operasjonen"))
                    .build();
            } else {
                if (fagSakFinnesFraFoer(innsendtSak)) {
                    response = Response.status(Response.Status.CONFLICT).entity(
                        new ErrorResponse(MDC.get("uuid"), String.format("Det finnes allerede en sak for fagsaksnr: %s, applikasjon: %s, aktør: %s orgnr: %s",
                            innsendtSak.getFagsakNr(),
                            innsendtSak.getApplikasjon(),
                            innsendtSak.getAktoerId(),
                            innsendtSak.getOrgnr())))
                        .build();
                } else {
                    final Sak opprettetSak = sakRepository.lagre(innsendtSak);
                    log.info("Opprettet: {}", opprettetSak);
                    response = Response.created(new URI(uriInfo.getAbsolutePathBuilder().path(String.valueOf(opprettetSak.getId())).build().getPath()))
                        .entity(new SakJson(opprettetSak))
                        .build();
                }
            }
        } else {
            response = Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                new ErrorResponse(MDC.get("uuid"), abacResultCode.getDescription())
            ).build();
        }

        return response;
    }

    private boolean harTilgangTilSakInterneRegler(
          final ContainerRequestContext ctx
        , final Sak sak) {

        final boolean temaKontroll = Objects.equals("KTR", sak.getTema());
        final boolean harTilgang = !(temaKontroll && Objects.equals(getSubjectType(ctx), SUBJECT_TYPE_EKSTERNBRUKER));
        if (!harTilgang) {
            log.info("Filtrerer ut sak med id: {} for ekstern bruker fordi den har tema: {} ", sak.getId(), sak.getTema());
        }

        return harTilgang;
    }

    private boolean fagSakFinnesFraFoer(Sak sak) {
        final SakSearchCriteria sakSearchCriteria = SakSearchCriteria.create().medOrgnr(sak.getOrgnr()).medAktoerId(sak.getAktoerId()).medFagsakNr(sak.getFagsakNr()).medApplikasjon(sak.getApplikasjon());
        return sak.getFagsakNr() != null &&
            !sakRepository.finnSaker(sakSearchCriteria).isEmpty();
    }
}
