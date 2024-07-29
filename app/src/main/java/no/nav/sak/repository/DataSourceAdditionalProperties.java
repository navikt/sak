package no.nav.sak.repository;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Ekstra konfigurasjon for databasen
 */
@Data
@ConfigurationProperties("database")
public class DataSourceAdditionalProperties {
	/**
	 * Oracle Notification Service (ONS) hosts
	 */
	private String onshosts;
}
