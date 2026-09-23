package za.co.ocr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Verifies that the Spring application context starts successfully.
 *
 * <p>This kind of test is a simple safety check. If the context can load, it
 * usually means the application wiring, configuration, and bean creation are at
 * least good enough to start the app.</p>
 */
@SpringBootTest
class DigitalPdfFlattenXfaApplicationTests {

	@Test
	void contextLoads() {
	}

}
