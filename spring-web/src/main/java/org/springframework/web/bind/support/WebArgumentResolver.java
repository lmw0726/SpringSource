/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.bind.support;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.web.context.request.NativeWebRequest;

/**
 * 用于为特定处理器方法参数解析自定义参数的SPI接口。
 * 通常实现此接口以检测特殊的参数类型，为它们解析已知的参数值。
 *
 * <p>一个典型的实现示例如下：
 *
 * <pre class="code">
 * public class MySpecialArgumentResolver implements WebArgumentResolver {
 *
 *   public Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) {
 *     if (methodParameter.getParameterType().equals(MySpecialArg.class)) {
 *       return new MySpecialArg("myValue");
 *     }
 *     return UNRESOLVED;
 *   }
 * }</pre>
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#setCustomArgumentResolvers
 */
@FunctionalInterface
public interface WebArgumentResolver {

	/**
	 * 当解析器不知道如何处理给定的方法参数时返回的标记对象。
	 */
	Object UNRESOLVED = new Object();


	/**
	 * 在给定的web请求中为给定的处理器方法参数解析参数。
	 *
	 * @param methodParameter 要解析的处理器方法参数
	 * @param webRequest      当前web请求，允许访问原生请求
	 * @return 参数值，或者{@code UNRESOLVED}如果不可解析
	 * @throws Exception 在解析失败的情况下抛出异常
	 */
	@Nullable
	Object resolveArgument(MethodParameter methodParameter, NativeWebRequest webRequest) throws Exception;

}
