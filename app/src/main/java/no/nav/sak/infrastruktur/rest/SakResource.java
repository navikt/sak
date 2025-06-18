package no.nav.sak.infrastruktur.rest;

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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import no.nav.sak.infrastruktur.ErrorResponse;
import no.nav.sak.infrastruktur.abac.ABACResult;
import no.nav.sak.infrastruktur.abac.AuthorizationRequest;
import no.nav.sak.infrastruktur.abac.SakPEP;
import no.nav.sak.infrastruktur.authentication.AuthenticationFilter;
import no.nav.sak.repository.Sak;
import no.nav.sak.repository.SakRepository;
import no.nav.sak.repository.SakSearchCriteria;
import no.nav.security.token.support.core.api.Protected;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static io.swagger.v3.oas.annotations.enums.SecuritySchemeIn.HEADER;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static no.nav.sak.infrastruktur.ContextExtractor.getSubjectType;
import static no.nav.sak.infrastruktur.SubjectType.SUBJECT_TYPE_EKSTERNBRUKER;
import static no.nav.sak.infrastruktur.authentication.AuthenticationFilter.REQUEST_CONSUMERID;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Protected
@RestController
@RequestMapping({SakResource.API_V1_SAKER, SakResource.API_V1_SAKER_TRAILING_SLASH})
@Tag(name = "/api/v1/saker")
@OpenAPIDefinition(
		info = @Info(
				title = "Sak API",
				version = "1",
				description = """
						Tjenestene her er deprekerte. Journalpostapi håndterer oppretting av arkivsaker under journalføringen.
						
						Konsumenter som behøver å hente arkivsaker skal bruke <a href="https://confluence.adeo.no/spaces/BOA/pages/402023600/Query+saker">saf saker GraphQL query</a> i stedet.
						
						Nye konsumenter som skal opprette arkivsaker må avklare bruk med <a href="https://nav-it.slack.com/archives/C6W9E5GPJ">Team Dokumentløsninger</a>.
						
						Sak API forventer at header <strong>"X-Correlation-ID"</strong> er angitt for alle tjenestekall. Denne logges alltid i Sak, og benyttes for å kunne sammenstille hendelser
						på tvers av kallkjeder. Bruk en korrelasjonsId
						""",
				contact = @Contact(
						name = "Team Dokumentløsninger"
				)))
@SecuritySchemes(
		value = {
				@SecurityScheme(
						name = "bearerAuth",
						in = HEADER,
						description = """
								Dette er preferert metode for autorisasjon.
								Eksempel på verdi i input-felt: <strong>eYdmifml0ejugm</strong>
								""",
						scheme = "bearer",
						bearerFormat = "JWT",
						type = SecuritySchemeType.HTTP),
				@SecurityScheme(
						name = "Saml",
						in = HEADER,
						description = """
								Autorisasjon med SAML Assertions er deprekert.
								""",
						type = SecuritySchemeType.APIKEY
				),
				@SecurityScheme(
						name = "Basic Auth",
						type = SecuritySchemeType.HTTP,
						scheme = "basic",
						description = """
								Autorisasjon med basic auth er deprekert. Brukes kun av teamet.
								"""
				)
		}
)
@Slf4j
public class SakResource {

	public static final String API_V1_SAKER_PATH_SEGMENT = "api/v1/saker";
	public static final String API_V1_SAKER = "/" + API_V1_SAKER_PATH_SEGMENT;
	public static final String API_V1_SAKER_TRAILING_SLASH = API_V1_SAKER + "/";
	private final SakRepository sakRepository;
	private final SakPEP sakPEP;

	SakResource(final SakRepository sakRepository, final SakPEP sakPEP) {
		this.sakRepository = sakRepository;
		this.sakPEP = sakPEP;
	}

	@GetMapping(path = "/{id}", produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Henter informasjon om sak, gitt en id",
			parameters = {@Parameter(name = "X-Correlation-ID", required = true, in = ParameterIn.HEADER)},
			security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "Saml"), @SecurityRequirement(name = "Basic Auth")},
			responses = {
					@ApiResponse(responseCode = "200",
							description = "OK",
							content = @Content(schema = @Schema(implementation = SakJson.class))),
					@ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Konsument mangler gyldig token"),
					@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Konsument har ikke tilgang til å gjennomføre handlingen"),
					@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Det finnes ingen sak for angitt id"),
					@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Ukjent feilsituasjon har oppstått i Sak"),
					@ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "En eller flere tjenester som sak er avhengig av er ikke tilgjengelige eller svarer ikke.")

			})
	public ResponseEntity<?> hentSak(
			@PathVariable("id") final Long id
			, HttpServletRequest ctx) {

		log.info("hentSak henter arkivsakId={}", id);
		final Optional<Sak> sak = sakRepository.hentSak(id);

		if (sak.isPresent()) {
			final Sak eksisterendeSak = sak.get();
			log.info("hentSak har hentet arkivsakId={}", id);
			return ResponseEntity.ok().body(
					new SakJson(eksisterendeSak));
		} else {
			log.warn("Mottatt oppslag på sak som ikke eksisterer, id: {}, consumer: {}", id, ctx.getAttribute(REQUEST_CONSUMERID));
			return ResponseEntity
					.status(NOT_FOUND)
					.body(new ErrorResponse(
							MDC.get("uuid"),
							String.format("Fant ingen sak med id: %s", id)));
		}
	}

	@GetMapping(produces = APPLICATION_JSON_VALUE)
	@Operation(summary = "Finner saker for angitte søkekriterier",
			security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "Saml"), @SecurityRequirement(name = "Basic Auth")},
			parameters = {@Parameter(name = "X-Correlation-ID", required = true, in = ParameterIn.HEADER)},
			responses = {
					@ApiResponse(responseCode = "200",
							description = "OK",
							content = @Content(schema = @Schema(implementation = SakJson.class, type = "list"))),
					@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Ugyldig input"),
					@ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Konsument mangler gyldig token"),
					@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Ukjent feilsituasjon har oppstått i Sak"),
					@ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "En eller flere tjenester som sak er avhengig av er ikke tilgjengelige eller svarer ikke.")
			})
	public ResponseEntity<?> finnSaker(
			@Valid final SakSearchRequest sakSearchRequest
			, HttpServletRequest ctx) {

		log.info("finnSak Søker etter saker for: {}", sakSearchRequest);
		for (String aktoerId : sakSearchRequest.getAktoerId()) {
			final ABACResult abacResult =
					sakPEP.autoriser(ctx, new AuthorizationRequest(aktoerId));
			final ABACResult.Code abacResultCode = abacResult.getResultCode();
			if (!ABACResult.Code.OK.equals(abacResultCode)) {
				return makeResponseUponAbacFailure(abacResultCode);
			} else if (!abacResult.hasAccess()) {
				return ResponseEntity.ok(new ArrayList<>());
			}
		}

		final List<Sak> saker =
				sakRepository.finnSaker(sakSearchRequest.toCriteria());
		log.info("finnSak hentet antall_arkivsaker={}", saker.size());
		return ResponseEntity.ok(
				saker.stream()
						.filter(s -> harTilgangTilSakInterneRegler(ctx, s))
						.map(SakJson::new)
						.collect(toList()));
	}

	@PostMapping(produces = APPLICATION_JSON_VALUE, consumes = APPLICATION_JSON_VALUE)
	@Operation(summary = "Oppretter en ny sak",
			description = """
					Dette var tidligere noe fagsystemene måtte gjøre for å kunne journalføre, men er ikke lenger nødvendig lenger.
					""",
			security = {@SecurityRequirement(name = "bearerAuth"), @SecurityRequirement(name = "Saml"), @SecurityRequirement(name = "Basic Auth")},
			responses = {
					@ApiResponse(responseCode = "201",
							description = "Saken er opprettet",
							headers = @Header(name = "location", description = "Angir URI til den opprettede saken")),
					@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Ugyldig input"),
					@ApiResponse(responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Konsument mangler gyldig token"),
					@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Konsument har ikke tilgang til å gjennomføre handlingen"),
					@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Det finnes allerede en sak for angitt kombinasjon av fagsaknr og applikasjon for aktør eller orgnr"),
					@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "Ukjent feilsituasjon har oppstått i Sak"),
					@ApiResponse(responseCode = "503", content = @Content(schema = @Schema(implementation = ErrorResponse.class)), description = "En eller flere tjenester som sak er avhengig av er ikke tilgjengelige eller svarer ikke.")
			})
	public ResponseEntity<?> opprettSak(
			@RequestBody @Valid @Parameter(name = "Saken som skal opprettes", required = true) SakJson sakJson
			, ServletUriComponentsBuilder servletUriComponentsBuilder
			, HttpServletRequest ctx
	) {

		final String user = (String) ctx.getAttribute(AuthenticationFilter.REQUEST_USERNAME);
		final Sak innsendtSak = sakJson.toSak(user);
		final String aktoerId = innsendtSak.getAktoerId();

		log.info("opprettSak for aktoerId={}", aktoerId);

		if (fagSakFinnesFraFoer(innsendtSak)) {
			return ResponseEntity
					.status(CONFLICT)
					.body(new ErrorResponse(
							MDC.get("uuid"),
							String.format(
									"Det finnes allerede en sak for fagsaksnr: %s, applikasjon: %s, aktør: %s orgnr: %s",
									innsendtSak.getFagsakNr(),
									innsendtSak.getApplikasjon(),
									innsendtSak.getAktoerId(),
									innsendtSak.getOrgnr())));
		} else {

			final long opprettetSakId = sakRepository.lagre(innsendtSak);
			log.info("opprettSak har opprettet arkivsakId={}", opprettetSakId);
			URI path = servletUriComponentsBuilder
					.pathSegment(API_V1_SAKER_PATH_SEGMENT)
					.pathSegment(String.valueOf(opprettetSakId))
					.build()
					.toUri();
			return ResponseEntity
					.created(path)
					.body(new SakJson(innsendtSak, opprettetSakId));
		}
	}

	private boolean harTilgangTilSakInterneRegler(
			final HttpServletRequest ctx
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

	private ResponseEntity<?> makeResponseUponAbacFailure(ABACResult.Code abacResultCode) {
		return ResponseEntity
				.status(mapABACResultCodeToResponseStatus(abacResultCode))
				.body(
						new ErrorResponse(
								MDC.get("uuid"),
								abacResultCode.getDescription()
						)
				);
	}

	private HttpStatus mapABACResultCodeToResponseStatus(ABACResult.Code abacResultCode) {
		if (ABACResult.Code.OK.equals(abacResultCode)) {
			return OK;
		} else {
			return INTERNAL_SERVER_ERROR;
		}

	}
}
