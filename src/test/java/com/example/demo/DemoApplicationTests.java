package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

	@LocalServerPort
	private int localServerPort;

	private WebTestClient client;

	@SpyBean
	private PublishSubscribeChannel channel;

	@BeforeEach
	void setup() {
		client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + localServerPort)
				.responseTimeout(Duration.ofSeconds(10))
				.build();
	}

	@Test
	void test() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		executorService.submit(() -> {
			try {
				Thread.sleep(1000L);
				while (!Thread.currentThread().isInterrupted()) {
					client.post().uri("/publish").exchange();
					client.post().uri("/publish").exchange();
					Thread.sleep(2000L);
				}
			} catch (InterruptedException e) {
				System.err.println("Done!");
			}
		});

		try {
			FluxExchangeResult<Foo> result = client.get()
					.uri("/listen")
					.accept(TEXT_EVENT_STREAM)
					.exchange()
					.expectStatus().isOk()
					.expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
					.returnResult(Foo.class);

			verify(channel).subscribe(any());

			StepVerifier.create(result.getResponseBody())
					.expectNextCount(5)
					.thenCancel()
					.verify();

			verify(channel, times(2)).unsubscribe(any());
		} finally {
			executorService.shutdown();
		}
	}

}
