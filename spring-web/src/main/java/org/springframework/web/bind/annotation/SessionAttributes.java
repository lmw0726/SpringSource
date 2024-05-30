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

package org.springframework.web.bind.annotation;

import org.springframework.core.annotation.AliasFor;

import java.lang.annotation.*;

/**
 * 指示特定处理程序使用的会话属性的注解。
 *
 * <p>通常会列出应该透明存储在会话或某些会话存储中的模型属性的名称，作为表单后端 bean。
 * <b>声明在类型级别</b>，应用于被注解的处理程序类操作的模型属性。
 *
 * <p><b>注意：</b>使用此注解指示的会话属性对应于特定处理程序的特定模型属性，会在会话期间被透明存储在对话式会话中。
 * 一旦处理程序指示其会话对话完成，这些属性将被删除。因此，在特定处理程序对话过程中暂时存储在会话中的会话属性，
 * 使用此功能。
 *
 * <p>对于永久性会话属性，例如用户身份验证对象，请改用传统的{@code session.setAttribute}方法。
 * 或者，考虑使用通用{@link org.springframework.web.context.request.WebRequest}接口的属性管理功能。
 *
 * <p><b>注意：</b>使用控制器接口（例如用于AOP代理），请确保一致地将<i>所有</i>映射注解，例如{@code @RequestMapping}
 * 和{@code @SessionAttributes}，放在控制器<i>接口</i>上，而不是在实现类上。
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.5
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SessionAttributes {

	/**
	 * {@link #names}的别名。
	 */
	@AliasFor("names")
	String[] value() default {};

	/**
	 * 应该存储在会话或某些会话存储中的模型中的会话属性的名称。
	 * <p><strong>注意</strong>：这表示<em>模型属性名称</em>。
	 * <em>会话属性名称</em>可能与模型属性名称匹配，也可能不匹配。
	 * 因此，应用程序不应依赖于会话属性名称，而应仅操作模型。
	 *
	 * @since 4.2
	 */
	@AliasFor("value")
	String[] names() default {};

	/**
	 * 应该存储在会话或某些会话存储中的模型中的会话属性的类型。
	 * <p>这些类型的所有模型属性都将存储在会话中，而不管属性名称如何。
	 */
	Class<?>[] types() default {};

}
