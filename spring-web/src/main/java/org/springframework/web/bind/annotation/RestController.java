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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Controller;

import java.lang.annotation.*;

/**
 * 一个方便的注解，本身带有 {@link Controller @Controller} 和 {@link ResponseBody @ResponseBody} 注解。
 * <p>
 * 携带此注解的类型被视为控制器，其中 {@link RequestMapping @RequestMapping} 方法默认假定具有 {@link ResponseBody @ResponseBody} 语义。
 * <p>
 * 注意：如果配置了适当的 {@code HandlerMapping} - {@code HandlerAdapter} 对，例如 {@code RequestMappingHandlerMapping} - {@code RequestMappingHandlerAdapter} 对，
 * 则会处理 {@code @RestController}，它们是 MVC Java 配置和 MVC 命名空间中的默认选项。
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@ResponseBody
public @interface RestController {

	/**
	 * 该值可能表示逻辑组件名称的建议，如果是自动检测到的组件，则将其转换为 Spring bean。
	 *
	 * @return 建议的组件名称（如果有）；否则为空字符串
	 * @since 4.0.1
	 */
	@AliasFor(annotation = Controller.class)
	String value() default "";

}
