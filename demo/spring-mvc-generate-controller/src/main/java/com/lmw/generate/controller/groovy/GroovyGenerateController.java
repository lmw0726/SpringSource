package com.lmw.generate.controller.groovy;

import com.lmw.generate.controller.groovy.service.GenerateControllerService;
import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.IOException;

/**
 * Groovy生成Controller 控制器
 *
 * @author LMW
 * @version 1.0
 * @date 2024-06-28 21:54
 */
@Controller
@RequestMapping("/groovy")
public class GroovyGenerateController {
	private final GenerateControllerService generateControllerService;

	public GroovyGenerateController(@Qualifier("groovyGenerateControllerService") GenerateControllerService generateControllerService) {
		this.generateControllerService = generateControllerService;
	}

	/**
	 * 根据Groovy脚本生成URL
	 *
	 * @return 生成URL的结果
	 * @throws IOException
	 * @throws ScriptException
	 * @throws ResourceException
	 */
	@ResponseBody
	@PostMapping("/generate")
	public ResponseEntity<String> generate() throws IOException, ScriptException, ResourceException {
		return generateControllerService.generate();
	}

	@ResponseBody
	@DeleteMapping("/unload")
	public ResponseEntity<String> unload() throws IOException, ScriptException, ResourceException {
		return generateControllerService.unload();
	}
}
