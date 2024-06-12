/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.client.reactive;

import org.reactivestreams.Publisher;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * {@link ClientHttpRequest}实现的基类。
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractClientHttpRequest implements ClientHttpRequest {

	/**
	 * COMMITTING -> COMMITTED 是在调用 doCommit 方法后，但在响应状态和头部应用到底层响应之前的阶段，
	 * 在此期间，预提交操作仍然可以更改响应状态和头部。
	 */
	private enum State {NEW, COMMITTING, COMMITTED}


	/**
	 * 包含用于构建客户端 HTTP 请求的基本信息。
	 */
	private final HttpHeaders headers;

	/**
	 * 用于存储发送到服务器的请求 Cookie 的多值映射。
	 */
	private final MultiValueMap<String, HttpCookie> cookies;

	/**
	 * 表示请求的状态，是一个原子引用，初始状态为 {@code NEW}。
	 */
	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	/**
	 * 包含提交操作的列表，每个操作都是一个供应者，可以提供一个发布者以进行提交。
	 */
	private final List<Supplier<? extends Publisher<Void>>> commitActions = new ArrayList<>(4);

	/**
	 * 只读的请求头信息。
	 */
	@Nullable
	private HttpHeaders readOnlyHeaders;


	public AbstractClientHttpRequest() {
		this(new HttpHeaders());
	}

	public AbstractClientHttpRequest(HttpHeaders headers) {
		Assert.notNull(headers, "HttpHeaders must not be null");
		this.headers = headers;
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public HttpHeaders getHeaders() {
		// 如果只读头部不为 null，则直接返回
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		} else if (State.COMMITTED.equals(this.state.get())) {
			// 如果状态为 COMMITTED，则初始化只读头部并返回
			this.readOnlyHeaders = initReadOnlyHeaders();
			return this.readOnlyHeaders;
		} else {
			// 否则，返回原始的头部
			return this.headers;
		}
	}

	/**
	 * 在请求提交后初始化只读头。
	 * <p>默认情况下，此方法只是应用一个只读包装器。
	 * 子类可以对来自原生请求的头执行相同的操作。
	 *
	 * @since 5.3.15
	 */
	protected HttpHeaders initReadOnlyHeaders() {
		return HttpHeaders.readOnlyHttpHeaders(this.headers);
	}

	@Override
	public MultiValueMap<String, HttpCookie> getCookies() {
		// 如果状态为 COMMITTED，则返回一个不可修改的 MultiValueMap（只读），其中包含响应中的所有 Cookie
		if (State.COMMITTED.equals(this.state.get())) {
			return CollectionUtils.unmodifiableMultiValueMap(this.cookies);
		}

		// 否则，返回原始的 MultiValueMap，其中包含响应中的所有 Cookie
		return this.cookies;
	}

	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		Assert.notNull(action, "Action must not be null");
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		return (this.state.get() != State.NEW);
	}

	/**
	 * 用于没有请求体的请求的 {@link #doCommit(Supplier)} 的变体。
	 *
	 * @return 完成发布者
	 */
	protected Mono<Void> doCommit() {
		return doCommit(null);
	}

	/**
	 * 应用 {@link #beforeCommit(Supplier) beforeCommit} 操作，应用请求头/cookie，并写入请求体。
	 *
	 * @param writeAction 写入请求体的操作（可以为 {@code null}）
	 * @return 完成发布者
	 */
	protected Mono<Void> doCommit(@Nullable Supplier<? extends Publisher<Void>> writeAction) {
		// 如果状态不是 NEW，则返回一个空的 Mono
		if (!this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			return Mono.empty();
		}

		// 添加提交动作，其中包括应用头信息、应用 Cookie 以及设置状态为 COMMITTED
		this.commitActions.add(() ->
				Mono.fromRunnable(() -> {
					applyHeaders();
					applyCookies();
					this.state.set(State.COMMITTED);
				}));

		// 如果写入动作不为 null，则将其添加到提交动作列表中
		if (writeAction != null) {
			this.commitActions.add(writeAction);
		}

		// 将提交动作转换为 Publisher 列表
		List<? extends Publisher<Void>> actions = this.commitActions.stream()
				.map(Supplier::get).collect(Collectors.toList());

		// 将所有提交动作按顺序执行，并返回一个空的 Flux
		return Flux.concat(actions).then();
	}


	/**
	 * 将来自 {@link #getHeaders()} 的标头更改应用到底层请求。
	 * 此方法仅调用一次。
	 */
	protected abstract void applyHeaders();

	/**
	 * 将来自 {@link #getHeaders()} 的 cookie 添加到底层请求。
	 * 此方法仅调用一次。
	 */
	protected abstract void applyCookies();

}
