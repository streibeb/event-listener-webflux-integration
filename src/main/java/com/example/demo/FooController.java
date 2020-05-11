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

import java.util.function.Consumer;

@Slf4j
@RestController
public class FooController {

	@Autowired
	private ApplicationEventPublisher applicationEventPublisher;
	@Autowired
	private PublishSubscribeChannel countChannel;

	@GetMapping(value = "/listen", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<?> listen() {
		log.info("Creating new flux");
		return Flux.create(getFluxSinkConsumer())
				.log()
				.doOnError(e -> log.info("doOnError"))
				.doOnTerminate(() -> log.info("doOnTerminate"))
				.doOnCancel(() -> log.info("doOnCancel"))
				.doOnComplete(() -> log.info("doOnComplete"))
				.doOnDiscard(Foo.class, x -> log.info("doOnDiscard"))
				.doFinally(s -> log.info("doFinally"))
				.map(count -> ServerSentEvent.builder(count).build());
	}

	@PostMapping(value = "/publish")
	public void publish() {
		log.info("Publishing new event");
		applicationEventPublisher.publishEvent(new FooEvent(Foo.newInstance()));
	}

	private Consumer<FluxSink<Foo>> getFluxSinkConsumer() {
		return sink -> {
			MessageHandler handler = msg -> {
				log.info(msg.toString());
				sink.next(Foo.newInstance());
			};
			sink.onDispose(() -> {
				log.info("Disposed!");
				countChannel.unsubscribe(handler);
			});
			sink.onCancel(() -> {
				log.info("Cancelled!");
				countChannel.unsubscribe(handler);
			});
			log.info("Subscribed!");
			countChannel.subscribe(handler);
		};
	}
}
