/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.web.context.annotation;

import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.core.annotation.AliasFor;
import org.springframework.web.context.WebApplicationContext;

import java.lang.annotation.*;

/**
 * {@code @SessionScope} 是 {@link Scope @Scope} 的一种专门化，用于将生命周期绑定到当前 Web 会话的组件。
 *
 * <p>具体来说，{@code @SessionScope} 是一个 <em>组合注解</em>，充当了 {@code @Scope("session")} 的快捷方式，
 * 其默认 {@link #proxyMode} 设置为 {@link ScopedProxyMode#TARGET_CLASS TARGET_CLASS}。
 *
 * <p>{@code @SessionScope} 可以用作元注解，用于创建自定义的组合注解。
 *
 * @author Sam Brannen
 * @see RequestScope
 * @see ApplicationScope
 * @see org.springframework.context.annotation.Scope
 * @see org.springframework.web.context.WebApplicationContext#SCOPE_SESSION
 * @see org.springframework.web.context.request.SessionScope
 * @see org.springframework.stereotype.Component
 * @see org.springframework.context.annotation.Bean
 * @since 4.3
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Scope(WebApplicationContext.SCOPE_SESSION)
public @interface SessionScope {

	/**
	 * {@link Scope#proxyMode} 的别名。
	 * <p>默认为 {@link ScopedProxyMode#TARGET_CLASS}。
	 */
	@AliasFor(annotation = Scope.class)
	ScopedProxyMode proxyMode() default ScopedProxyMode.TARGET_CLASS;

}
