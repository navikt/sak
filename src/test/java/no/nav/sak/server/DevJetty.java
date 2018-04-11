package no.nav.sak.server;

import org.eclipse.jetty.servlet.ServletContextHandler;

public class DevJetty extends StartJetty {
    public static void main(String[] args) throws Exception {
        new DevJetty().start();
    }

    public void shutdown() throws Exception {
        server.stop();
    }

    @Override
    void registerJettyMetrics(ServletContextHandler contextHandler) {
    }
}
