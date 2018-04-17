package no.nav.sak.server;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.servlet.ServletContainer;

public class DevJetty extends StartJetty {
    public static void main(String[] args) throws Exception {
        new DevJetty().start();
    }

    public void shutdown() throws Exception {
        server.stop();
    }

    void registerJerseyApplication(ServletContextHandler context) {
        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("javax.ws.rs.Application", "no.nav.sak.SakH2Application");
        context.addServlet(jerseyServlet, "/api/*");
    }

    @Override
    void registerJettyMetrics(ServletContextHandler contextHandler) {
    }
}
