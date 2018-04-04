package no.nav.sak.server;

import io.prometheus.client.exporter.MetricsServlet;
import io.prometheus.client.jetty.JettyStatisticsCollector;
import no.nav.sak.infrastruktur.AliveCheckServlet;
import no.nav.sak.infrastruktur.ReadyCheckServlet;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.StatisticsHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.resource.Resource;
import org.glassfish.jersey.servlet.ServletContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartJetty {
    private static final Logger log = LoggerFactory.getLogger(StartJetty.class);
    Server server = new Server(getPort());

    public static void main(String[] args) throws Exception {
        new StartJetty().start();
    }

    public void start() throws Exception {
        ServletContextHandler context = new ServletContextHandler(server, "/");

        ServletHolder defaultServlet = new ServletHolder(new DefaultServlet());
        context.addServlet(defaultServlet, "/*");

        ServletHolder jerseyServlet = new ServletHolder(new ServletContainer());
        jerseyServlet.setInitParameter("javax.ws.rs.Application", "no.nav.sak.SakApplication");
        context.addServlet(jerseyServlet, "/api/*");

        ServletHolder readyServlet = new ServletHolder(new ReadyCheckServlet());
        context.addServlet(readyServlet, "/internal/ready/*");

        ServletHolder aliveServlet = new ServletHolder(new AliveCheckServlet());
        context.addServlet(aliveServlet, "/internal/alive/*");

        ServletHolder metricsServlet = new ServletHolder(new MetricsServlet());
        context.addServlet(metricsServlet, "/internal/metrics/*");

        context.setBaseResource(Resource.newClassPathResource("META-INF/resources/webjars/swagger-ui/3.9.2"));

        server.setHandler(context);
        registerJettyMetrics(context);

        server.start();
        log.info("Startet jetty");
    }

    void registerJettyMetrics(ServletContextHandler contextHandler) {
        StatisticsHandler stats = new StatisticsHandler();
        stats.setHandler(contextHandler);
        server.setHandler(stats);
        new JettyStatisticsCollector(stats).register();
    }

    private int getPort() {
        return Integer.valueOf(System.getProperty("sak.port", "8080"));
    }
}
