/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.servlet.mvc.annotation;

import org.springframework.lang.Nullable;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.servlet.ModelAndView;

import java.lang.reflect.Method;

/**
 * SPI（Service Provider Interface）用于从特定的处理器方法中解析自定义返回值。
 * 通常实现此接口以检测特殊的返回类型，并为它们解析已知的结果值。
 *
 * <p>一个典型的实现可能如下所示：
 *
 * <pre class="code">
 * public class MyModelAndViewResolver implements ModelAndViewResolver {
 *
 *     public ModelAndView resolveModelAndView(Method handlerMethod, Class handlerType,
 *             Object returnValue, ExtendedModelMap implicitModel, NativeWebRequest webRequest) {
 *         if (returnValue instanceof MySpecialRetVal.class)) {
 *             return new MySpecialRetVal(returnValue);
 *         }
 *         return UNRESOLVED;
 *     }
 * }</pre>
 *
 * @author Arjen Poutsma
 * @since 3.0
 */
public interface ModelAndViewResolver {

	/**
	 * 当解析器不知道如何处理给定的方法参数时返回的标记。
	 */
	ModelAndView UNRESOLVED = new ModelAndView();

	/**
	 * 解析模型和视图的方法。
	 *
	 * @param handlerMethod 处理器方法
	 * @param handlerType   处理器类型
	 * @param returnValue   返回值
	 * @param implicitModel 隐式模型
	 * @param webRequest    网络请求
	 * @return 解析后的模型和视图，如果无法解析则返回UNRESOLVED
	 */
	ModelAndView resolveModelAndView(Method handlerMethod, Class<?> handlerType,
									 @Nullable Object returnValue, ExtendedModelMap implicitModel, NativeWebRequest webRequest);

}
