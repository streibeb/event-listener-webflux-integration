package com.example.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import java.time.Duration;
import java.util.function.Consumer;

@Slf4j
@RestController
public class FooController {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private PublishSubscribeChannel countChannel;

	@GetMapping(value = "/listen-event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<?> listenOnEvent() {
		log.info("Creating new flux");
		return Flux.create(getFluxSinkConsumer())
				.log()
				.doFinally(s -> doFinally())
				.map(this::createServerSentEvent);
	}

	@GetMapping(value = "/listen-interval", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<?> listenOnInterval() {
		log.info("Creating new flux");
		return Flux.interval(Duration.ofSeconds(1))
				.log()
				.doFinally(s -> doFinally())
				.map(x -> Foo.newInstance())
				.map(this::createServerSentEvent);
	}

	@PostMapping(value = "/publish")
	public void publish() {
		log.info("Publishing new event");
		applicationEventPublisher.publishEvent(new FooEvent(Foo.newInstance()));
	}

	void doFinally() {
		log.info("doFinally");
	}

	ServerSentEvent<Foo> createServerSentEvent(Foo foo) {
		log.info("createServerSentEvent [{}]", foo);
		return ServerSentEvent.builder(foo).build();
	}

	private Consumer<FluxSink<Foo>> getFluxSinkConsumer() {
		return sink -> {
			MessageHandler handler = msg -> {
				log.info(msg.toString());
				sink.next(Foo.newInstance());
			};
			sink.onDispose(() -> {
				log.info("FluxSink.onDispose()");
				countChannel.unsubscribe(handler);
			});
			sink.onCancel(() -> {
				log.info("FluxSink.onCancel()");
				countChannel.unsubscribe(handler);
			});
			countChannel.subscribe(handler);
		};
	}
}
