package no.nav.sak.app.nais;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@RestController
public final class NaisContract {

	private static final String APPLICATION_ALIVE = "Application is alive!";
	private static final String APPLICATION_READY = "Application is ready for traffic!";
	private static AtomicInteger isReady = new AtomicInteger(1);

	@Autowired
	public NaisContract() {
	}

	@GetMapping("/isAlive")
	public String isAlive() {
		return APPLICATION_ALIVE;
	}

	@ResponseBody
	@RequestMapping(value = "/isReady", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity<String> isReady() {
		return new ResponseEntity<>(APPLICATION_READY, HttpStatus.OK);
	}
}
