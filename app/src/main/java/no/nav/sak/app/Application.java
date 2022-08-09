package no.nav.sak.app;

import no.nav.sak.SakApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({SakApplication.class})
public class Application {

	public static void main(String[] args) {
		try {
			SpringApplication.run(Application.class, args);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

}
