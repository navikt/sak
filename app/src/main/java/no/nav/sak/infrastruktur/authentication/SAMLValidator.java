package no.nav.sak.infrastruktur.authentication;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.schema.XSString;
import org.opensaml.core.xml.schema.impl.XSAnyImpl;
import org.opensaml.saml.common.SAMLException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.impl.AssertionUnmarshaller;
import org.opensaml.saml.security.impl.SAMLSignatureProfileValidator;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateFactory;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.time.temporal.ChronoUnit.MINUTES;

public class SAMLValidator {

	private static final Logger log = LoggerFactory.getLogger(SAMLValidator.class);
	private static final String ATTR_CONSUMER_ID = "consumerId";
	private static final String CERT_X509 = "X509";

	private Collection<X509Certificate> trustedCertificates;
	private final SAMLSignatureProfileValidator profileValidator;
	private final KeyStore keyStore;
	private final int timeSkew;
	private final Clock clock;

	public SAMLValidator(String trustStore, String trustStorePassword, Clock clock) {
		this(trustStore, trustStorePassword, 0, clock);
	}

	public SAMLValidator(String trustStore, String trustStorePassword, int timeSkewInMinutes, Clock clock) {
		this.timeSkew = timeSkewInMinutes;
		this.clock = clock;
		init();
		keyStore = new KeyStore(trustStore, trustStorePassword, null);
		profileValidator = new SAMLSignatureProfileValidator();
	}

	private void init() {
		try {
			InitializationService.initialize();
		} catch (InitializationException e) {
			throw new IllegalStateException("Feilet under initialisering av SAML", e);
		}
	}

	private AuthenticationResult validateSignature(Assertion assertion) {
		BasicX509Credential credential;
		Signature signature;
		try {
			credential = getCredentialFromAssertion(assertion);
			signature = assertion.getSignature();
			X509Certificate certificateFromSignature = getCertificateFromSignature(signature);

			assertValidity(certificateFromSignature);
			if (!existsInTrustStore(certificateFromSignature) && !isSignedByTrustedCA(certificateFromSignature)) {
				return AuthenticationResult.invalid("Certificate not trusted, either it is missing from the truststore or not signed by a root CA in the truststore");
			}
		} catch (Exception e) {
			return invalidResult(e.getMessage(), e);
		}

		try {
			getProfileValidator().validate(signature);
		} catch (SignatureException e) {
			return invalidResult("Signature does not conform to spec", e);
		}

		try {
			SignatureValidator.validate(signature, credential);
		} catch (SignatureException e) {
			return invalidResult("Signature not valid", e);
		}

		return AuthenticationResult.success(getUsernameFromNameID(assertion), getAttribute(assertion, SAMLValidator.ATTR_CONSUMER_ID));
	}

	private boolean isSignedByTrustedCA(X509Certificate certificateInAssertion) throws SAMLException {
		Optional<X509Certificate> rootCACertificate = findSigningRootCACertificate(certificateInAssertion);

		if (rootCACertificate.isPresent()) {
			assertValidity(rootCACertificate.get());
			return true;
		}

		return false;
	}

	private Optional<X509Certificate> findSigningRootCACertificate(X509Certificate certificateInAssertion) {
		for (X509Certificate certificate : getTrustedCertificates()) {
			boolean isCARoot = (certificate.getBasicConstraints() != -1);
			if (isCARoot) {
				String certCN = certificate.getSubjectDN().getName();
				try {
					certificateInAssertion.verify(certificate.getPublicKey());
					log.debug("Certificate from SAML assertion is signed by root CA certificate with CN {}", certCN);
					return Optional.of(certificate);
				} catch (Exception e) {
					log.trace("Certificate({}) was not used to sign Certificate from SAML assertion", certCN);
				}
			}
		}

		return Optional.empty();
	}

	private AuthenticationResult validateSAMLAssertion(Element assertionElement) {
		Assertion assertion;
		try {
			assertion = (Assertion) new AssertionUnmarshaller().unmarshall(assertionElement);
			if (notExpired(assertion)) {
				return validateSignature(assertion);
			} else {
				return AuthenticationResult.invalid(String.format("Assertion in token has expired. NotBefore: %s NotAfter: %s Current: %s",
						assertion.getConditions().getNotBefore(),
						assertion.getConditions().getNotOnOrAfter(),
						LocalDateTime.now()));
			}
		} catch (UnmarshallingException e) {
			return invalidResult("Failed while unmarshalling assertion", e);
		}
	}

	private AuthenticationResult invalidResult(String msg, Exception e) {
		log.error(msg, e);
		return AuthenticationResult.invalid(msg);
	}

	private boolean notExpired(Assertion assertion) {
		Instant notBefore = assertion.getConditions().getNotBefore().minus(timeSkew, MINUTES);
		Instant notOnOrAfter = assertion.getConditions().getNotOnOrAfter().plus(timeSkew, MINUTES);
		return notBefore.isBefore(clock.instant()) && notOnOrAfter.isAfter(clock.instant());
	}

	private void assertValidity(X509Certificate certificate) throws SAMLException {
		try {
			certificate.checkValidity();
		} catch (CertificateExpiredException | CertificateNotYetValidException e) {
			throw new SAMLException("Certificate failed validity check", e);
		}
	}

	private boolean existsInTrustStore(X509Certificate x509Certificate) throws SAMLException {
		boolean isTrusted = false;
		try {
			String certificateAlias = getSakKeyStore().getKeyStore().getCertificateAlias(x509Certificate);
			if (!StringUtils.isEmpty(certificateAlias)) {
				isTrusted = true;
			}
			return isTrusted;
		} catch (KeyStoreException e) {
			throw new SAMLException("Failed while getting SAML assertion certificate from keystore", e);
		}
	}

	private Collection<X509Certificate> getTrustedCertificates() {
		if (trustedCertificates == null) {
			trustedCertificates = getSakKeyStore().getCertificates();
		}
		return trustedCertificates;
	}

	private BasicX509Credential getCredentialFromAssertion(Assertion assertion) throws Exception {
		X509Certificate x509Certificate = getCertificateFromSignature(assertion.getSignature());
		return new BasicX509Credential(x509Certificate);
	}

	private X509Certificate getCertificateFromSignature(Signature signature) throws Exception {
		org.opensaml.xmlsec.signature.X509Certificate certificate = signature.getKeyInfo().getX509Datas().get(0).getX509Certificates().get(0);
		if (certificate == null) {
			throw new SAMLException("Unable to retrieve certificate from signature");
		}
		byte[] bytes = Base64.decodeBase64(certificate.getValue());
		return createCertificate(new ByteArrayInputStream(bytes));
	}

	private X509Certificate createCertificate(InputStream stream) throws Exception {
		try (BufferedInputStream bis = new BufferedInputStream(stream)) {
			CertificateFactory certificateFactory = CertificateFactory.getInstance(CERT_X509);
			return (X509Certificate) certificateFactory.generateCertificate(bis);
		} catch (IOException | CertificateException e) {
			throw new SAMLException("Could not create certificate from input stream", e);
		}
	}

	private String getAttribute(Assertion assertion, String attributeName) {
		List<Attribute> attributes = assertion.getAttributeStatements().get(0).getAttributes();
		Attribute attribute = attributes.stream()
				.filter(attr -> attributeName.equals(attr.getName()))
				.findFirst()
				.orElseThrow(() -> new RuntimeException("Missing attribute in SAML assertion: " + attributeName));

		XMLObject xmlObject = attribute.getAttributeValues().get(0);

		String textContent;
		if (xmlObject instanceof XSString) {
			textContent = ((XSString) xmlObject).getValue();
		} else if (xmlObject instanceof XSAnyImpl) {
			textContent = ((XSAnyImpl) xmlObject).getTextContent();
		} else {
			throw new RuntimeException(String.format("Could not retrieve attribute %s from saml assertion", attributeName));
		}

		return textContent;
	}

	private String getUsernameFromNameID(Assertion assertion) {
		return assertion.getSubject().getNameID().getValue();
	}

	private Element createXMLElementFromToken(String tokenBase64) throws ParserConfigurationException, SAXException, IOException {
		byte[] bytes = Base64.decodeBase64(tokenBase64.getBytes(StandardCharsets.UTF_8));
		DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
		documentBuilderFactory.setNamespaceAware(true);
		DocumentBuilder db = documentBuilderFactory.newDocumentBuilder();
		Document parse = db.parse(new ByteArrayInputStream(bytes));
		return parse.getDocumentElement();
	}

	private SAMLSignatureProfileValidator getProfileValidator() {
		return profileValidator;
	}

	private KeyStore getSakKeyStore() {
		return keyStore;
	}

	public AuthenticationResult validate(String tokenBase64) {
		Element assertionElement;
		try {
			assertionElement = createXMLElementFromToken(tokenBase64);
		} catch (Exception e) {
			return invalidResult("Failed while creating XML element from token", e);
		}

		return validateSAMLAssertion(assertionElement);
	}
}
