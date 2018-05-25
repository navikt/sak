package no.nav.sak.infrastruktur.sts;

import no.nav.sak.SakConfiguration;
import no.nav.sak.infrastruktur.oicd.OidcLogin;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.Validate;
import org.apache.cxf.Bus;
import org.apache.cxf.bus.CXFBusFactory;
import org.apache.cxf.message.Message;
import org.apache.cxf.ws.policy.PolicyBuilder;
import org.apache.cxf.ws.policy.attachment.reference.ReferenceResolver;
import org.apache.cxf.ws.policy.attachment.reference.RemoteReferenceResolver;
import org.apache.cxf.ws.security.SecurityConstants;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.apache.neethi.Policy;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;

import static java.lang.System.getenv;

public class STSSupport {
    private static final String TRUSTSTORE_***passord=gammelt_passord***";
    private static final String TRUSTSTORE = "nav_truststore_nonproduction-t.jts";
    private static final String TOKEN_TYPE = "http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0";
    private static final String KEY_TYPE = "http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer";
    private final OidcLogin oidcLogin = new OidcLogin();

    private final SakConfiguration sakConfiguration = new SakConfiguration();

    public STSSupport() throws IOException {
        setupTrustStore();
    }

    private static String wrapWithBinarySecurityToken(byte[] token, String valueType) {
        String base64encodedToken = Base64.encodeBase64String(token);
        return "<wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\""
            + " EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\""
            + " ValueType=\"" + valueType + "\" >" + base64encodedToken + "</wsse:BinarySecurityToken>";
    }

    private void setupTrustStore() throws IOException {
        InputStream trustStore = STSSupport.class.getClassLoader().getResourceAsStream(TRUSTSTORE);
        File tempFile = File.createTempFile("nav_truststore" + System.currentTimeMillis(), ".tmp");
        tempFile.deleteOnExit();
        FileUtils.copyInputStreamToFile(trustStore, tempFile);
        System.setProperty("javax.net.ssl.trustStore", tempFile.getAbsolutePath());
        System.setProperty("javax.net.ssl.trustStorePassword", getEnvVariable(TRUSTSTORE_PASSWORD));
    }

    private String getEnvVariable(String name) {
        return Validate.notNull(getenv(name), String.format("Env property '%s' er påkrevd", name));
    }

    /**
     * Brukes for å hente et SAML-token basert på et OIDC token, her vil SAML-tokenet inneholde en InternBruker.
     */
    public String getSAMLTokenFromSTS() throws Exception {
        CXFBusFactory c = new CXFBusFactory();
        STSClient stsClient = new STSClient(c.createBus());
        stsClient.setWsdlLocation("wsdl/ws-trust-1.4-service.wsdl");
        stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceProvider"));
        stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceSOAP"));
        stsClient.setEnableAppliesTo(false);
        stsClient.setAllowRenewing(false);

        HashMap<String, Object> properties = new HashMap<>();
        properties.put(SecurityConstants.USERNAME, sakConfiguration.getRequiredString("SYSTEMBRUKER_USERNAME"));
        properties.put(SecurityConstants.PASSWORD, sakConfiguration.getRequiredString("SYSTEMBRUKER_PASSWORD"));
        stsClient.setProperties(properties);
        stsClient.setCustomContent("<wst:SecondaryParameters xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">" +
            "<wst:TokenType>" + TOKEN_TYPE + "</wst:TokenType>" +
            "</wst:SecondaryParameters>");

        stsClient.setTokenType(TOKEN_TYPE);
        stsClient.setKeyType(KEY_TYPE);
        stsClient.setPolicy("classpath:policy/untPolicy.xml");

        String token = oidcLogin.getIdToken();
        String wrapped = wrapWithBinarySecurityToken(token.getBytes(), "urn:ietf:params:oauth:token-type:jwt");
        stsClient.setOnBehalfOf(wrapped);
        stsClient.setAllowRenewing(false);
        stsClient.setAddressingNamespace("http://w3.org/2005/08/addressing");
        stsClient.getClient().getRequestContext().put(Message.ENDPOINT_ADDRESS, "https://sts-t8.test.local/SecurityTokenServiceProvider/");

        SecurityToken securityToken = stsClient.requestSecurityToken();
        Element assertion = securityToken.getToken();

        StringWriter writer = new StringWriter();
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        try {
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(assertion), new StreamResult(writer));
        } catch (TransformerException e) {
            throw new IllegalStateException(e);
        }

        return Base64.encodeBase64String(writer.toString().getBytes(Charset.forName("UTF-8")));
    }

    /**
     * Brukes for å hente et SAML-token basert på en systembruker, dette tokenet vil kun inneholde systembrukeren
     * som vil identifiseres som en systemressurs.
     */
    public String getSystemSAMLTokenFromSTS() {
        CXFBusFactory c = new CXFBusFactory();
        Bus bus = c.createBus();
        STSClient stsClient = new STSClient(bus);
        try {
            stsClient.setWsdlLocation("wsdl/ws-trust-1.4-service.wsdl");
            stsClient.setServiceQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceProvider"));
            stsClient.setEndpointQName(new QName("http://docs.oasis-open.org/ws-sx/ws-trust/200512/wsdl", "SecurityTokenServiceSOAP"));
            stsClient.getClient().getRequestContext().put(Message.ENDPOINT_ADDRESS, "https://sts-t8.test.local/SecurityTokenServiceProvider/");
            HashMap<String, Object> properties = new HashMap<>();
            properties.put(SecurityConstants.USERNAME, sakConfiguration.getRequiredString("SYSTEMBRUKER_USERNAME"));
            properties.put(SecurityConstants.PASSWORD, sakConfiguration.getRequiredString("SYSTEMBRUKER_PASSWORD"));
            stsClient.setProperties(properties);
            stsClient.setTokenType(TOKEN_TYPE);
            stsClient.setKeyType(KEY_TYPE);
            stsClient.setAllowRenewing(false);
            stsClient.setAddressingNamespace("http://w3.org/2005/08/addressing");

            PolicyBuilder builder = bus.getExtension(PolicyBuilder.class);
            ReferenceResolver resolver = new RemoteReferenceResolver(null, builder);
            Policy policy = resolver.resolveReference("classpath:policy/untPolicy.xml");
            stsClient.setPolicy(policy);

            SecurityToken securityToken = stsClient.requestSecurityToken();
            Element assertion = securityToken.getToken();

            StringWriter writer = new StringWriter();
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.transform(new DOMSource(assertion), new StreamResult(writer));

            return Base64.encodeBase64String(writer.toString().getBytes(Charset.forName("UTF-8")));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
