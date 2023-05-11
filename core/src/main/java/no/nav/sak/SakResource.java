package no.nav.sak;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.sak.infrastruktur.EnableApiFilters;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sak.infrastruktur.abac.AuthorizationRequest;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakRepository;
import no.nav.sak.repository.SakSearchCriteria;
import no.nav.sikkerhet.abac.ABACResult;
import org.slf4j.MDC;

import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
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

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.Response.Status.NOT_FOUND;
import static no.nav.sak.infrastruktur.ContextExtractor.getSubjectType;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;

@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@EnableApiFilters
@Path("/v1/saker")
@Tag(name = "v1/saker")
@OpenAPIDefinition(
		info = @Info(
				title = "Sak API",
				version = "1",
				description = """
						Her dokumenteres tjenestegrensesnittet for Sak.
						            
						Tjenesten leveres kontinuerlig til produksjon. For å sikre oss mot å innføre regresjon som påvirker våre konsumenter, benytter vi
						Pact. Det er konsumentens ansvar å gi oss pact-test, men ta gjerne kontakt ved behov for bistand ifm. dette.
						           
						Vi ber nye konsumenter om å ta kontakt med teamet, dette for å få gjennomført ev. avklaringer, sikre korrekte tilganger, pact-test, og for å sikre at tjenesten støtter 
						forventet volum og ev. SLA.
						            
						Merk at vi forventer at Headeren <strong>"X-Correlation-ID"</strong> er angitt for alle tjenestekall. Denne logges alltid i Sak, og benyttes for å kunne sammenstille hendelser
						på tvers av kallkjeder. X-Correlation-ID skal oppgis ved forespørsel om bistand fra Team Oppgavehåntering vedr. feilsøk ifm. bruk av tjenesten
						Vi anbefaler at korrelasjonsID genereres så tidlig som mulig hos konsument, bindes til tråden, og logges sammen med alle hendelser som danner grunnlaget for kallet mot Sak.
						            
						KorrelasjonsIDen skal være unik, og kan enten genereres med f.eks UUID.randomUUID() eller hvis aktuelt, hentes ut fra inngående tjenestekall (i.e. callId via modig-biblioteket)
						""",
				contact = @Contact(
						name = "Team Dokumentløsninger"
				)))
@SecuritySchemes(
		value = {
				@SecurityScheme(
						name = "Bearer",
						in = HEADER,
						description = """
								OIDC-token (JWT via OAuth2.0). Dette preferert autentiseringsmekanisme, og <strong>skal</strong>
								benyttes ved tjenestekall initiert av en bruker for å propagere konteksten (unntatt i særtilfeller - se Saml)
								Følgende format må brukes i input-feltet "Value" under: <strong>"Bearer {token}"</strong>.
								Eksempel på verdi i input-felt: <strong>Bearer eYdmifml0ejugm</strong>
								                                
								Et gyldig token kommer til å ha mange flere karakterer enn i eksempelet.
								""",
						type = SecuritySchemeType.APIKEY),
				@SecurityScheme(
						name = "Saml",
						in = HEADER,
						description = """
								P.t støttes ikke konvertering fra SAML til OIDC-token og det er derfor implementert støtte for Saml for å propagere brukercontext fra legacy-systemer
								(i.e. fra et system som kun eksponerer soap-tjenester og som skal gjøre tjenestekall videre mot Oppgave.
								I denne konteksten er et SAML token en SAML assertion som er Base 64 enkodet.
								På grunn av begrensninger i header-lengde, må saml-assertion strippes for whitespaces før den encodes
								Formatet skal være som følger: <strong>"Saml {token}"</strong>.
								Eksempel på verdi i input-felt: <strong>Saml eYdmifml0ejugm</strong>
								                                
								Et gyldig token kommer til å ha mange flere karakterer enn i eksempelet.
								""",
						type = SecuritySchemeType.APIKEY
				),
				@SecurityScheme(
						name = "Basic Auth",
						type = SecuritySchemeType.HTTP,
						scheme = "basic",
						description = "Basic auth kan brukes når det er snakk om system-til-system kommunikasjon"
				)
		}
)
@Slf4j
public class SakResource {

	private final SakRepository sakRepository;
	private final SakPEP sakPEP;

	SakResource(final SakRepository sakRepository, final SakPEP sakPEP) {
		this.sakRepository = sakRepository;
		this.sakPEP = sakPEP;
	}

	@GET
	@Path("/{id}")
	@Operation(summary = "Henter sak for en gitt id",
			parameters = {@Parameter(name = "X-Correlation-ID", required = true, in = ParameterIn.HEADER)},
			responses = {
					@ApiResponse(responseCode = "200",
							description = "OK",
							content = @Content(schema = @Schema(implementation = SakJson.class))),
					@ApiResponse(responseCode = "401", description = "Konsument mangler gyldig token"),
					@ApiResponse(responseCode = "403", description = "Konsument har ikke tilgang til å gjennomføre handlingen"),
					@ApiResponse(responseCode = "404", description = "Det finnes ingen sak for angitt id"),
					@ApiResponse(responseCode = "500", description = "Ukjent feilsituasjon har oppstått i Sak"),
					@ApiResponse(responseCode = "503", description = "En eller flere tjenester som sak er avhengig av er ikke tilgjengelige eller svarer ikke.")

			})
	public Response hentSak(
			@PathParam("id") final Long id
			, @Context final ContainerRequestContext ctx) {

		log.info("hentSak henter arkivsakId={}", id);
		final Optional<Sak> sak = sakRepository.hentSak(id);
		final Response response;

		if (sak.isPresent()) {
			final Sak eksisterendeSak = sak.get();
			response = Response.ok(
					new SakJson(eksisterendeSak)).build();
			log.info("hentSak har hentet arkivsakId={}", id);
		} else {
			log.warn("Mottatt oppslag på sak som ikke eksisterer, id: {}, consumer: {}", id, ctx.getProperty(REQUEST_CONSUMERID));
			response = Response
					.status(NOT_FOUND)
					.entity(new ErrorResponse(
							MDC.get("uuid"),
							String.format("Fant ingen sak med id: %s", id)))
					.build();
		}
		return response;
	}

	@GET
	@Operation(summary = "Finner saker for angitte søkekriterier",
			parameters = {@Parameter(name = "X-Correlation-ID", required = true, in = ParameterIn.HEADER)},
			responses = {
					@ApiResponse(responseCode = "200",
							description = "OK",
							content = @Content(schema = @Schema(implementation = SakJson.class, type = "list"))),
					@ApiResponse(responseCode = "400", description = "Ugyldig input"),
					@ApiResponse(responseCode = "401", description = "Konsument mangler gyldig token"),
					@ApiResponse(responseCode = "500", description = "Ukjent feilsituasjon har oppstått i Sak"),
					@ApiResponse(responseCode = "503", description = "En eller flere tjenester som sak er avhengig av er ikke tilgjengelige eller svarer ikke.")
			})
	public Response finnSaker(
			@Valid @BeanParam final SakSearchRequest sakSearchRequest
			, @Context final ContainerRequestContext ctx) {

		log.info("finnSak Søker etter saker for: {}", sakSearchRequest);
		for (String aktoerId : sakSearchRequest.getAktoerId()) {
			final ABACResult abacResult =
					sakPEP.autoriser(ctx, new AuthorizationRequest(aktoerId));
			final ABACResult.Code abacResultCode = abacResult.getResultCode();
			if (!ABACResult.Code.OK.equals(abacResultCode)) {
				return makeResponseUponAbacFailure(abacResultCode);
			} else if (!abacResult.hasAccess()) {
				return Response.ok(new ArrayList<>()).build();
			}
		}

		final List<Sak> saker =
				sakRepository.finnSaker(sakSearchRequest.toCriteria());
		log.info("finnSak hentet antall_arkivsaker={}", saker.size());
		return Response.ok(
				saker.stream()
						.filter(s -> harTilgangTilSakInterneRegler(ctx, s))
						.map(SakJson::new)
						.collect(toList())).build();
	}

	@POST
	@Operation(summary = "Oppretter en ny sak",
			description = "Merk at en sak enten skal tilhøre en aktør <b>eller</b> et foretak. Begge er p.t. ikke tillatt. ",
			responses = {
					@ApiResponse(responseCode = "201",
							description = "Saken er opprettet",
							headers = @Header(name = "location", description = "Angir URI til den opprettede saken")),
					@ApiResponse(responseCode = "400", description = "Ugyldig input"),
					@ApiResponse(responseCode = "401", description = "Konsument mangler gyldig token"),
					@ApiResponse(responseCode = "403", description = "Konsument har ikke tilgang til å gjennomføre handlingen"),
					@ApiResponse(responseCode = "409", description = "Det finnes allerede en sak for angitt kombinasjon av fagsaknr og applikasjon for aktør eller orgnr"),
					@ApiResponse(responseCode = "500", description = "Ukjent feilsituasjon har oppstått i Sak"),
					@ApiResponse(responseCode = "503", description = "En eller flere tjenester som sak er avhengig av er ikke tilgjengelige eller svarer ikke.")
			})
	public Response opprettSak(
			@Valid @Parameter(name = "Saken som skal opprettes", required = true) final SakJson sakJson
			, @Context final UriInfo uriInfo
			, @Context final ContainerRequestContext ctx
	) throws URISyntaxException {

		final String user = (String) ctx.getProperty(AuthenticationFilter.REQUEST_USERNAME);
		final Sak innsendtSak = sakJson.toSak(user);
		final String aktoerId = innsendtSak.getAktoerId();

		log.info("opprettSak for aktoerId={}", aktoerId);

		final Response response;
		if (fagSakFinnesFraFoer(innsendtSak)) {
			response = Response
					.status(Response.Status.CONFLICT)
					.entity(new ErrorResponse(
							MDC.get("uuid"),
							String.format(
									"Det finnes allerede en sak for fagsaksnr: %s, applikasjon: %s, aktør: %s orgnr: %s",
									innsendtSak.getFagsakNr(),
									innsendtSak.getApplikasjon(),
									innsendtSak.getAktoerId(),
									innsendtSak.getOrgnr()))).build();
		} else {

			final Sak opprettetSak = sakRepository.lagre(innsendtSak);
			log.info("Opprettet: {}", opprettetSak);
			response = Response.created(
							new URI(uriInfo.getAbsolutePathBuilder()
									.path(String.valueOf(opprettetSak.getId()))
									.build()
									.getPath()))
					.entity(new SakJson(opprettetSak))
					.build();
			log.info("opprettSak har opprettet arkivsakId={}", opprettetSak.getId());
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

		final SakSearchCriteria sakSearchCriteria =
				SakSearchCriteria
						.create()
						.medOrgnr(sak.getOrgnr())
						.medAktoerId(sak.getAktoerId() != null ? singletonList(sak.getAktoerId()) : emptyList())
						.medFagsakNr(sak.getFagsakNr())
						.medTema(sak.getTema() != null ? singletonList(sak.getTema()) : emptyList())
						.medApplikasjon(sak.getApplikasjon());

		return sak.getFagsakNr() != null &&
				!sakRepository.finnSaker(sakSearchCriteria).isEmpty();
	}

	private Response makeResponseUponAbacFailure(final ABACResult.Code abacResultCode) {

		final Response.Status responseStatus =
				mapABACResultCodeToResponseStatus(abacResultCode);
		return Response
				.status(responseStatus)
				.entity(
						new ErrorResponse(
								MDC.get("uuid"),
								abacResultCode.getDescription()
						)
				)
				.build();
	}

	private Response.Status mapABACResultCodeToResponseStatus(final ABACResult.Code abacResultCode) {

		final Response.Status responseStatus;
		if (ABACResult.Code.OK.equals(abacResultCode)) {
			responseStatus = Response.Status.OK;
		} else {
			responseStatus = Response.Status.INTERNAL_SERVER_ERROR;
		}

		return responseStatus;
	}
}
