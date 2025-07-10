package no.nav.sak.validering;

import lombok.extern.slf4j.Slf4j;
import no.nav.sak.repository.Tema;
import no.nav.sak.repository.TemaRepository;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class TemaService {

	private final TemaRepository temaRepository;
	private final Map<String, Boolean> inaktivtTemaCache = new ConcurrentHashMap<>();

	public TemaService(TemaRepository temaRepository) {
		this.temaRepository = temaRepository;
	}

	public boolean isTemaInaktivt(String tema) {
		return inaktivtTemaCache.computeIfAbsent(tema, this::doCheckIsTemaInaktivt);
	}

	private boolean doCheckIsTemaInaktivt(String tema) {
		Optional<Tema> temaOpt = temaRepository.findById(tema);

		if (temaOpt.isEmpty()) {
			return true;
		}

		boolean inaktivtTema = temaOpt.get().isInaktiv();
		if (inaktivtTema) {
			log.error("Tema={} er ikke lenger aktivt etter datoTilOgMed={}", tema, temaOpt.get().getDatoTilOgMed());
		}

		return inaktivtTema;
	}
}