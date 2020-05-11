package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.WebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.integration.dsl.MessageChannels;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.stereotype.Component;

@SpringBootApplication
@EnableIntegration
public class DemoApplication {

	@Bean("countChannel")
	public PublishSubscribeChannel countChannel() {
		return MessageChannels.publishSubscribe("countChannel").get();
	}

	@Bean("applicationListener")
	public ApplicationEventListeningMessageProducer applicationListener() {
		ApplicationEventListeningMessageProducer producer = new ApplicationEventListeningMessageProducer();
		producer.setEventTypes(FooEvent.class);
		return producer;
	}

	@Bean("aggregateEventIntegrationFlow")
	public IntegrationFlow aggregateEventIntegrationFlow(ApplicationEventListeningMessageProducer applicationListener, PublishSubscribeChannel countChannel) {
		return IntegrationFlows.from(applicationListener)
				.aggregate(aggregatorSpec ->
						aggregatorSpec.groupTimeout(1000L)
								.sendPartialResultOnExpiry(true)
								.expireGroupsUponCompletion(true)
								.expireGroupsUponTimeout(true)
								.correlationStrategy(message -> true)
								.releaseStrategy(message -> false)
				)
				.log()
				.channel(countChannel)
				.get();
	}

	@Component
	public static class WebServerCustomizer implements WebServerFactoryCustomizer<WebServerFactory> {

		@Override
		public void customize(WebServerFactory factory) {
			System.err.println(factory);
		}
	}

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

}
