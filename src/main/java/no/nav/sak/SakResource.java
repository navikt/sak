package no.nav.sak;

import io.swagger.annotations.*;
import no.nav.sak.infrastruktur.EnableApiFilters;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sikkerhet.abac.ABACResult;
import no.nav.sikkerhet.abac.ABACService;
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
import java.util.List;
import java.util.Optional;

import static io.swagger.annotations.ApiKeyAuthDefinition.ApiKeyLocation.HEADER;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_USERNAME;


@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@EnableApiFilters
@Path("/v1/saker")
@Api(value = "v1/oppgaver", authorizations = {
    @Authorization(value = "Bearer"),
    @Authorization(value = "Saml"),
    @Authorization(value = "Basic")
})
@SwaggerDefinition(securityDefinition =
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
    private static final String READ = "read";
    private static final String CREATE = "create";
    private static final String SAK_DOMENE = "sak";
    private static final String RESOURCE_TYPE_SAK = "no.nav.abac.attributter.resource.sak.sak";

    private static final Logger log = LoggerFactory.getLogger(SakResource.class);
    private final SakRepository sakRepository;
    private final ABACService abacService;
    private final boolean abacEnabled;

    SakResource(SakRepository sakRepository, ABACService abacService, boolean abacEnabled) {
        this.sakRepository = sakRepository;
        this.abacService = abacService;
        this.abacEnabled = abacEnabled;
    }

    @GET
    @Path("/{id}")
    @ApiOperation(value = "Henter sak for en gitt id", response = SakJson.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 401, message = "Konsument mangler gyldig token"),
        @ApiResponse(code = 404, message = "Det finnes ingen sak for angitt id"),
        @ApiResponse(code = 500, message = "Ukjent feilsituasjon har oppstått i Sak")
    }
    )
    public Response hentSak(@PathParam("id") Long id, @Context ContainerRequestContext ctx) {
        ABACResult abacResult = hasAccess(ctx, READ);
        if (!abacResult.hasAccess()) {
            log.warn("Autorisering feilet: {}", abacResult.getErrorMessage());
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(MDC.get("uuid"), "Autorisering feilet - se Kibana for årsak"))
                .build();
        } else {
            log.debug("Henter sak med id: {}", id);
            Optional<Sak> sak = sakRepository.hentSak(id);

            return sak.map(s -> Response.ok(new SakJson(s)).build())
                .orElseGet(() -> {
                        log.warn("Mottatt oppslag på sak som ikke eksisterer, id: {}, consumer: {}", id, ctx.getProperty(REQUEST_CONSUMERID));
                        return Response.status(NOT_FOUND).entity(
                            new ErrorResponse(MDC.get("uuid"), String.format("Fant ingen sak med id: %s", id))
                        ).build();
                    }
                );
        }
    }

    @GET
    @ApiOperation(value = "Finner saker for angitte søkekriterier",
        response = SakJson.class, responseContainer = "List")
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 400, message = "Ugyldig input"),
        @ApiResponse(code = 401, message = "Konsument mangler gyldig token"),
        @ApiResponse(code = 500, message = "Ukjent feilsituasjon har oppstått i Sak")
    }
    )
    public Response finnSaker(@Valid @BeanParam SakSearchRequest sakSearchRequest, @Context ContainerRequestContext ctx) {
        log.debug("Søker etter saker for: {}", sakSearchRequest);
        List<Sak> saker = sakRepository.finnSaker(sakSearchRequest.toCriteria());
        return Response.ok(
            saker.stream()
                .map(SakJson::new)
                .collect(toList()))
            .build();
    }

    @POST
    @ApiOperation(value = "Oppretter en ny sak", notes = "Merk at en sak enten skal tilhøre en aktør <b>eller</b> et foretak. Begge er p.t. ikke tillatt. ")
    @ApiResponses(value = {
        @ApiResponse(code = 201, message = "Saken er opprettet", responseHeaders = @ResponseHeader(name = "location", description = "Angir URI til den opprettede saken")),
        @ApiResponse(code = 400, message = "Ugyldig input"),
        @ApiResponse(code = 401, message = "Konsument mangler gyldig token"),
        @ApiResponse(code = 409, message = "Det finnes allerede en sak for angitt kombinasjon av fagsaknr og applikasjon for aktør eller orgnr"),
        @ApiResponse(code = 500, message = "Ukjent feilsituasjon har oppstått i Sak")
    }
    )
    public Response opprettSak(@Valid
                               @ApiParam(value = "Saken som skal opprettes", required = true) SakJson sakJson, @Context UriInfo uriInfo, @Context ContainerRequestContext ctx) throws URISyntaxException {
        ABACResult abacResult = hasAccess(ctx, CREATE);
        if (!abacResult.hasAccess()) {
            log.warn("Autorisering feilet: {}", abacResult.getErrorMessage());
            return Response.status(Response.Status.FORBIDDEN)
                .entity(new ErrorResponse(MDC.get("uuid"), "Autorisering feilet - se Kibana for årsak"))
                .build();
        } else {
            String user = (String) ctx.getProperty(REQUEST_USERNAME);
            Sak innsendtSak = sakJson.toSak(user);
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

    private ABACResult hasAccess(@Context ContainerRequestContext ctx, String operation) {
        if (abacEnabled) {
            if ("read".equals(operation)) {
                return abacService.hasReadAccess(ctx, RESOURCE_TYPE_SAK, SAK_DOMENE);
            } else {
                return abacService.hasCreateAccess(ctx, RESOURCE_TYPE_SAK, SAK_DOMENE);
            }
        }

        return ABACResult.success();
    }

    private boolean fagSakFinnesFraFoer(Sak sak) {
        SakSearchCriteria sakSearchCriteria = SakSearchCriteria.create().medOrgnr(sak.getOrgnr()).medAktoerId(sak.getAktoerId()).medFagsakNr(sak.getFagsakNr()).medApplikasjon(sak.getApplikasjon());
        return sak.getFagsakNr() != null &&
            !sakRepository.finnSaker(sakSearchCriteria).isEmpty();
    }
}
