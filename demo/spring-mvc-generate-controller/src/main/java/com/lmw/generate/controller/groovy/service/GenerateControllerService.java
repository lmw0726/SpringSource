package com.lmw.generate.controller.groovy.service;

import groovy.util.ResourceException;
import groovy.util.ScriptException;
import org.springframework.http.ResponseEntity;

import java.io.IOException;

/**
 * 生成Controller服务接口
 *
 * @author LMW
 * @version 1.0
 * @date 2024-06-28 22:03
 */
public interface GenerateControllerService {
	/**
	 * 动态生成URL
	 *
	 * @return 响应消息
	 * @see <a>https://blog.csdn.net/zhao_god/article/details/132083904</a>
	 */
	ResponseEntity<String> generate();

	/**
	 * 卸载URL
	 *
	 * @return 响应消息
	 * @see <a>https://blog.csdn.net/zhao_god/article/details/132083904</a>
	 */
	ResponseEntity<String> unload() throws IOException, ScriptException, ResourceException;
}
