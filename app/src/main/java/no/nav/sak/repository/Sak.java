package no.nav.sak.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.time.LocalDateTime;
import java.util.Objects;

import static jakarta.persistence.GenerationType.SEQUENCE;

/**
 * Entitet basert på Sak fra dokarkiv. Kun relevante felt er beholdt
 */
@Entity
@Table(name = "SAK")
@Builder(toBuilder = true, setterPrefix = "med")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sak {

    private static final String SAK_SEQUENCE = "seq_sak";
    private static final String DATABASE_SAK_SEQUENCE = "seq_sak";

    @Id
    @GeneratedValue(strategy = SEQUENCE, generator = SAK_SEQUENCE)
    @SequenceGenerator(name = SAK_SEQUENCE, sequenceName = DATABASE_SAK_SEQUENCE, allocationSize = 1)
    @Column(name = "id", nullable = false, length = 11)
    private Long sakId;

    @Column(name = "tema", nullable = false, length = 40)
    private String tema;

    @Column(name = "applikasjon", length = 40)
    private String applikasjon;

    @Column(name = "fagsaknr", length = 40)
    private String fagsakNr;

    @ToString.Exclude
    @Column(name = "aktoerid", length = 40)
    private String aktoerId;

    @Column(name = "orgnr", length = 9)
    private String orgnr;

    @Column(name = "opprettet_av", nullable = false, length = 40)
    private String opprettetAv;

    @Column(name = "opprettet_tidspunkt", nullable = false)
    private LocalDateTime opprettetTidspunkt;

    /**
     * Modellert som enum i dokarkiv
     */
    @Column(name = "k_sak_status", length = 40)
    private String sakStatus;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Sak sak = (Sak) o;
        return Objects.equals(sakId, sak.sakId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(sakId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("sakId", sakId)
                .toString();
    }
}
