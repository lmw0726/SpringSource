package com.lmw.springmvc.domain;

import javax.validation.constraints.NotNull;

public class DemoObj {
	@NotNull(message = "{id.notNull}")
	private Long id;
	private String name;

	public DemoObj() {
		super();
	}

	public DemoObj(Long id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}
}
