/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.handler;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.handler.ResponseStatusExceptionHandler;

/**
 * 这是一个常见的 WebFlux 异常处理器，用于检测继承自基类的 {@link org.springframework.web.server.ResponseStatusException}
 * 实例以及通过确定它们的 HTTP 状态码并相应地更新响应状态来处理带有 {@link ResponseStatus @ResponseStatus} 注解的异常。
 *
 * <p>如果响应已经提交，则错误将保持未解决状态并被传播。
 *
 * @author Juergen Hoeller
 * @author Rossen Stoyanchev
 * @since 5.0.5
 */
public class WebFluxResponseStatusExceptionHandler extends ResponseStatusExceptionHandler {

	@Override
	protected int determineRawStatusCode(Throwable ex) {
		int status = super.determineRawStatusCode(ex);
		if (status == -1) {
			// 查找异常类上的合并注解 @ResponseStatus注解
			ResponseStatus ann = AnnotatedElementUtils.findMergedAnnotation(ex.getClass(), ResponseStatus.class);
			if (ann != null) {
				// 如果找到注解，使用注解中的状态码
				status = ann.code().value();
			}
		}
		// 返回最终确定的状态码
		return status;
	}

}

