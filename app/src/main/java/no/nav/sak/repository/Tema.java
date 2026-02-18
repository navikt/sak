package no.nav.sak.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;

import static java.time.LocalDate.now;

@Entity
@Data
@Builder
@AllArgsConstructor
@Table(name = "T_K_FAGOMRADE")
public class Tema {

	@Id
	@Column(name = "k_fagomrade", nullable = false)
	private @NonNull String kode;

	@Column(name = "dekode", length = 200, nullable = false)
	private @NonNull String dekode;

	@Column(name = "er_gyldig", length = 1, nullable = false)
	private boolean erGyldig;

	@Column(name = "dato_tom")
	private @Nullable LocalDate datoTilOgMed;

	public Tema() {
	}

	public boolean isInaktiv() {
		return !isErGyldig() && getDatoTilOgMed() != null && now().isAfter(getDatoTilOgMed());
	}
}
