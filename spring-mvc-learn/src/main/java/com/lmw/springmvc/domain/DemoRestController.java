package com.lmw.springmvc.domain;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/rest")
public class DemoRestController {
	@RequestMapping(value = "/getjson", produces = "application/json;charset=UTF-8")
	@ResponseBody
	public DemoObj getjson(@Valid DemoObj obj) {
		return new DemoObj(obj.getId() + 1, obj.getName() + "yy");
	}

	@RequestMapping(value = "/getxml", produces = "application/xml;charset=UTF-8")
	@ResponseBody
	public DemoObj getXml(DemoObj obj) {
		return new DemoObj(obj.getId() + 1, obj.getName() + "yy");
	}

}
