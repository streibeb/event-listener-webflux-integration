package com.example.demo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.TEXT_EVENT_STREAM;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DemoApplicationTests {

	@LocalServerPort
	private int localServerPort;

	private WebTestClient client;
	private AtomicBoolean running = new AtomicBoolean(false);

	@SpyBean
	private PublishSubscribeChannel channel;
	@SpyBean
	private FooController controller;

	@BeforeEach
	void setup() {
		client = WebTestClient.bindToServer()
				.baseUrl("http://localhost:" + localServerPort)
				.responseTimeout(Duration.ofSeconds(10))
				.build();
	}

	@Test
	void testEvent() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		if (running.compareAndSet(false, true)) {
			executorService.submit(() -> {
				try {
					Thread.sleep(1000L);
					while (running.get()) {
						client.post().uri("/publish").exchange();
						client.post().uri("/publish").exchange();
						Thread.sleep(2000L);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			});
		}

		try {
			FluxExchangeResult<Foo> result = client.get()
					.uri("/listen-event")
					.accept(TEXT_EVENT_STREAM)
					.exchange()
					.expectStatus().isOk()
					.expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
					.returnResult(Foo.class);

			verify(channel).subscribe(any());

			StepVerifier.create(result.getResponseBody())
					.thenCancel()
					.verify();

			verify(channel, timeout(5000L).times(2)).unsubscribe(any());
			verify(controller).doFinally();
		} finally {
			if (running.compareAndSet(true, false)) {
				executorService.shutdown();
			}
		}
	}

	@Test
	void testInterval() {
		FluxExchangeResult<Foo> result = client.get()
				.uri("/listen-interval")
				.accept(TEXT_EVENT_STREAM)
				.exchange()
				.expectStatus().isOk()
				.expectHeader().contentTypeCompatibleWith(TEXT_EVENT_STREAM)
				.returnResult(Foo.class);

		StepVerifier.create(result.getResponseBody())
				.thenCancel()
				.verify();

		verify(controller, timeout(5000L)).doFinally();
	}

}
