package no.nav.sak.infrastruktur.authentication;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

public class KeyStore {
	private static final Logger log = LoggerFactory.getLogger(KeyStore.class);
	private final java.security.KeyStore keyStore;
	private final String privateKeyPassword;

	public KeyStore(String trustStore, String trustStorePassword, String privateKeyPassword) {
		this.privateKeyPassword = privateKeyPassword;

		try {
			keyStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());

			log.debug("Trying to load Truststore with classloader");
			try (InputStream classPathStream = KeyStore.class.getResourceAsStream(trustStore)) {
				if (classPathStream != null) {
					keyStore.load(classPathStream, trustStorePassword.toCharArray());
				} else {
					log.debug("Trying to load Truststore from filesystem");
					try (InputStream is = new FileInputStream(trustStore)) {
						keyStore.load(is, trustStorePassword.toCharArray());
					}
				}
			}
		} catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException e) {
			throw new IllegalStateException("Failed while initiating Truststore", e);
		}
	}

	java.security.KeyStore getKeyStore() {
		return keyStore;
	}

	public Collection<X509Certificate> getCertificates() {
		try {
			Collection<X509Certificate> certificates = new ArrayList<>();
			Enumeration<String> aliases = getKeyStore().aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				X509Certificate certificate = (X509Certificate) getKeyStore().getCertificate(alias);
				certificates.add(certificate);
			}
			return certificates;
		} catch (KeyStoreException e) {
			throw new RuntimeException("Failed while reading certificates from truststore", e);
		}
	}

	public java.security.KeyStore.PrivateKeyEntry getSSLCertificate() {
		if (StringUtils.isEmpty(privateKeyPassword)) {
			throw new IllegalStateException("Password for accessing privatekey entry in keystore was not provided");
		}

		try {
			java.security.KeyStore.PrivateKeyEntry privateKeyEntry = null;
			Enumeration<String> aliases = getKeyStore().aliases();
			while (aliases.hasMoreElements()) {
				String alias = aliases.nextElement();
				if (keyStore.isKeyEntry(alias)) {
					java.security.KeyStore.PasswordProtection protParam = new java.security.KeyStore.PasswordProtection(privateKeyPassword.toCharArray());
					privateKeyEntry = (java.security.KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, protParam);
				}
			}
			return privateKeyEntry;
		} catch (KeyStoreException | UnrecoverableEntryException | NoSuchAlgorithmException e) {
			throw new RuntimeException("Failed while reading certificates from truststore", e);
		}
	}
}
