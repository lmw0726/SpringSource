/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.server;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * 用于在 HTTP 请求之间提供访问会话属性的服务器端会话的主要契约。
 *
 * <p>{@code WebSession} 实例的创建并不会自动启动会话，因此不会将会话 ID 发送到客户端（通常通过 cookie）。
 * 当添加会话属性时，会话会隐式启动。也可以通过 {@link #start()} 显式创建会话。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface WebSession {

	/**
	 * 返回一个唯一的会话标识符。
	 */
	String getId();

	/**
	 * 返回一个持有会话属性的映射。
	 */
	Map<String, Object> getAttributes();

	/**
	 * 如果存在，则返回会话属性值。
	 *
	 * @param name 属性名称
	 * @param <T>  属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T getAttribute(String name) {
		return (T) getAttributes().get(name);
	}

	/**
	 * 返回会话属性值，如果不存在则抛出 {@link IllegalArgumentException}。
	 *
	 * @param name 属性名称
	 * @param <T>  属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	default <T> T getRequiredAttribute(String name) {
		T value = getAttribute(name);
		Assert.notNull(value, () -> "Required attribute '" + name + "' is missing.");
		return value;
	}

	/**
	 * 返回会话属性值，或者默认的回退值。
	 *
	 * @param name         属性名称
	 * @param defaultValue 默认值
	 * @param <T>          属性类型
	 * @return 属性值
	 */
	@SuppressWarnings("unchecked")
	default <T> T getAttributeOrDefault(String name, T defaultValue) {
		return (T) getAttributes().getOrDefault(name, defaultValue);
	}

	/**
	 * 强制创建会话，导致在调用 {@link #save()} 时发送会话 ID。
	 */
	void start();

	/**
	 * 返回一个布尔值，指示是否通过 {@link #start()} 显式或通过添加会话属性隐式启动了与客户端的会话。
	 * 如果为 "false"，则不会将会话 ID 发送到客户端，并且 {@link #save()} 方法实际上是一个空操作。
	 */
	boolean isStarted();

	/**
	 * 生成一个新的会话 ID，并更新底层会话存储以反映新的 ID。成功调用后，{@link #getId()} 将反映新的会话 ID。
	 *
	 * @return 完成通知（成功或错误）
	 */
	Mono<Void> changeSessionId();

	/**
	 * 使当前会话无效，并清除会话存储。
	 *
	 * @return 完成通知（成功或错误）
	 */
	Mono<Void> invalidate();

	/**
	 * 通过 {@code WebSessionStore} 保存会话，具体步骤如下：
	 * <ul>
	 * <li>如果会话是新的（即已创建但从未持久化），则必须通过 {@link #start()} 显式启动，或通过添加属性隐式启动，
	 * 否则此方法不应起任何作用。
	 * <li>如果会话是通过 {@code WebSessionStore} 获取的，则此方法的实现必须检查会话是否已被{@link #invalidate() 使无效}，
	 * 如果是，则返回错误。
	 * </ul>
	 * <p>请注意，此方法不适用于应用程序直接使用。相反，它会在响应提交之前自动调用。
	 *
	 * @return {@code Mono}，指示完成状态（成功或错误）
	 */
	Mono<Void> save();

	/**
	 * 如果会话在 {@link #getMaxIdleTime() maxIdleTime} 经过后已过期，则返回 {@code true}。
	 * <p>通常情况下，应该在访问会话、创建新的 {@code WebSession} 实例（如果需要）、以及请求处理开始时自动进行过期检查，
	 * 这样应用程序就不必默认关注过期的会话。
	 */
	boolean isExpired();

	/**
	 * 返回会话创建的时间。
	 */
	Instant getCreationTime();

	/**
	 * 返回会话上一次由于用户活动（例如 HTTP 请求）而访问的时间。与 {@link #getMaxIdleTime() maxIdleTimeInSeconds} 一起，
	 * 这有助于确定会话是否 {@link #isExpired() 过期}。
	 */
	Instant getLastAccessTime();

	/**
	 * 配置在 {@link #getLastAccessTime() lastAccessTime} 经过之后可能经过的最长时间，然后会话被视为过期。
	 * 负值表示会话不会过期。
	 */
	void setMaxIdleTime(Duration maxIdleTime);

	/**
	 * 返回在 {@link #getLastAccessTime() lastAccessTime} 经过后会话过期的最长时间。负时间表示会话不会过期。
	 */
	Duration getMaxIdleTime();

}
