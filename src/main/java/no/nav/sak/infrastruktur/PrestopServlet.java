package no.nav.sak.infrastruktur;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class PrestopServlet extends HttpServlet {
    private static Logger log = LoggerFactory.getLogger(PrestopServlet.class);
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            log.info("Received pre-stop signal from Kubernetes - awaiting 5 seconds before allowing sigterm");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            log.warn("Prestop interrupted", e);
            Thread.currentThread().interrupt();
        }
        resp.setStatus(200);
    }

}
