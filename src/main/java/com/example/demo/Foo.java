package com.example.demo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Random;

@Data
@Accessors(chain = true)
public class Foo {
	private Integer blah;

	public static Foo newInstance() {
		Foo instance = new Foo();
		instance.blah = new Random().nextInt();
		return instance;
	}
}
