package za.co.ocr;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import za.co.ocr.model.EmailInfo;

import java.util.function.Consumer;

@SpringBootApplication
@Slf4j
public class DigitalPdfFlattenXfaApplication {

	public static void main(String[] args) {
		SpringApplication.run(DigitalPdfFlattenXfaApplication.class, args);
	}

	@Bean
	public Consumer<EmailInfo> flattenPDF() {
		return request -> {
			// Implement the logic to flatten the PDF here
			log.info("Received request to flatten PDF {}: ", request);
			// You can add your PDF flattening logic here, using libraries like Apache PDFBox or iText
		};
	}

}
