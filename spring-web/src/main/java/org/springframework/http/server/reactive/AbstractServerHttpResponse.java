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

package org.springframework.http.server.reactive;

import org.reactivestreams.Publisher;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * {@link ServerHttpResponse} 实现的基类。
 *
 * @author Rossen Stoyanchev
 * @author Juergen Hoeller
 * @author Sebastien Deleuze
 * @author Brian Clozel
 * @since 5.0
 */
public abstract class AbstractServerHttpResponse implements ServerHttpResponse {

	/**
	 * COMMITTING -> COMMITTED 是在调用 doCommit 之后但在响应状态和头被应用到底层响应之前的时期，
	 * 在此期间，预提交操作仍然可以对响应状态和头进行更改。
	 */
	private enum State {NEW, COMMITTING, COMMIT_ACTION_FAILED, COMMITTED}


	/**
	 * 表示数据缓冲区工厂的字段，用于创建数据缓冲区。
	 */
	private final DataBufferFactory dataBufferFactory;

	/**
	 * 表示HTTP状态码的字段。
	 */
	@Nullable
	private Integer statusCode;

	/**
	 * 表示HTTP头的字段，用于存储HTTP头信息。
	 */
	private final HttpHeaders headers;

	/**
	 * 表示要发送到客户端的cookies的字段。
	 */
	private final MultiValueMap<String, ResponseCookie> cookies;

	/**
	 * 表示HTTP响应状态的字段，使用AtomicReference以确保线程安全，初始状态为NEW。
	 */
	private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

	/**
	 * 表示在提交HTTP响应之前要执行的操作列表，初始容量为4。
	 */
	private final List<Supplier<? extends Mono<Void>>> commitActions = new ArrayList<>(4);

	/**
	 * 表示只读的HTTP头的字段。
	 */
	@Nullable
	private HttpHeaders readOnlyHeaders;


	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory) {
		this(dataBufferFactory, new HttpHeaders());
	}

	public AbstractServerHttpResponse(DataBufferFactory dataBufferFactory, HttpHeaders headers) {
		Assert.notNull(dataBufferFactory, "DataBufferFactory must not be null");
		Assert.notNull(headers, "HttpHeaders must not be null");
		this.dataBufferFactory = dataBufferFactory;
		this.headers = headers;
		this.cookies = new LinkedMultiValueMap<>();
	}


	@Override
	public final DataBufferFactory bufferFactory() {
		return this.dataBufferFactory;
	}

	@Override
	public boolean setStatusCode(@Nullable HttpStatus status) {
		// 如果当前状态为 COMMITTED（已提交）
		if (this.state.get() == State.COMMITTED) {
			// 返回 false，表示操作未成功
			return false;
		} else {
			// 否则，将状态码设置为传入状态的整数值，如果传入状态为 null，则设置为 null
			this.statusCode = (status != null ? status.value() : null);
			// 返回 true，表示操作成功
			return true;
		}
	}

	@Override
	@Nullable
	public HttpStatus getStatusCode() {
		return (this.statusCode != null ? HttpStatus.resolve(this.statusCode) : null);
	}

	@Override
	public boolean setRawStatusCode(@Nullable Integer statusCode) {
		// 如果当前状态为 COMMITTED（已提交）
		if (this.state.get() == State.COMMITTED) {
			// 返回 false，表示操作未成功
			return false;
		} else {
			// 否则，将状态码设置为传入的状态码
			this.statusCode = statusCode;
			// 返回 true，表示操作成功
			return true;
		}
	}

	@Override
	@Nullable
	public Integer getRawStatusCode() {
		return this.statusCode;
	}

	/**
	 * 设置响应的HTTP状态码。
	 *
	 * @param statusCode HTTP状态码的整数值
	 * @since 5.0.1
	 * @deprecated 自5.2.4起，使用{@link ServerHttpResponse#setRawStatusCode(Integer)}更优。
	 */
	@Deprecated
	public void setStatusCodeValue(@Nullable Integer statusCode) {
		if (this.state.get() != State.COMMITTED) {
			this.statusCode = statusCode;
		}
	}

	/**
	 * 返回响应的HTTP状态码。
	 *
	 * @return HTTP状态码的整数值
	 * @since 5.0.1
	 * @deprecated 自5.2.4起，使用{@link ServerHttpResponse#getRawStatusCode()}更优。
	 */
	@Nullable
	@Deprecated
	public Integer getStatusCodeValue() {
		return this.statusCode;
	}

	@Override
	public HttpHeaders getHeaders() {
		// 如果只读头信息不为空，则直接返回
		if (this.readOnlyHeaders != null) {
			return this.readOnlyHeaders;
		} else if (this.state.get() == State.COMMITTED) {
			// 如果当前状态为 COMMITTED（已提交），且只读头信息为空
			// 创建只读的 HttpHeaders，并设置为只读头信息
			this.readOnlyHeaders = HttpHeaders.readOnlyHttpHeaders(this.headers);
			// 返回只读头信息
			return this.readOnlyHeaders;
		} else {
			// 如果当前状态不为 COMMITTED（未提交）
			// 返回原始头信息
			return this.headers;
		}
	}

	@Override
	public MultiValueMap<String, ResponseCookie> getCookies() {
		return (this.state.get() == State.COMMITTED ?
				CollectionUtils.unmodifiableMultiValueMap(this.cookies) : this.cookies);
	}

	@Override
	public void addCookie(ResponseCookie cookie) {
		Assert.notNull(cookie, "ResponseCookie must not be null");

		// 如果当前状态为 COMMITTED（已提交）
		if (this.state.get() == State.COMMITTED) {
			// 抛出 IllegalStateException 异常
			throw new IllegalStateException("Can't add the cookie " + cookie +
					"because the HTTP response has already been committed");
		} else {
			// 否则，向 Cookie 集合中添加 Cookie
			getCookies().add(cookie.getName(), cookie);
		}
	}

	/**
	 * 返回底层服务器响应。
	 * <p><strong>注意：</strong> 这主要是为了内部框架使用，例如在 spring-webflux 模块中进行WebSocket升级。
	 *
	 * @param <T> 底层响应类型的泛型
	 * @return 底层服务器响应
	 */
	public abstract <T> T getNativeResponse();


	@Override
	public void beforeCommit(Supplier<? extends Mono<Void>> action) {
		this.commitActions.add(action);
	}

	@Override
	public boolean isCommitted() {
		// 获取当前的状态
		State state = this.state.get();

		// 如果状态不是 NEW（新建）且不是 COMMIT_ACTION_FAILED（提交操作失败）
		return (state != State.NEW && state != State.COMMIT_ACTION_FAILED);
	}

	@Override
	@SuppressWarnings("unchecked")
	public final Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
		// 如果 body 是 Mono 类型，可以避免使用 ChannelSendOperator，而 Reactor Netty 更适用于 Mono。
		// 但是，为了有机会处理潜在的错误，我们必须先解析值。
		if (body instanceof Mono) {
			return ((Mono<? extends DataBuffer>) body)
					.flatMap(buffer -> {
						// 对数据缓冲区进行处理
						touchDataBuffer(buffer);
						// 原子布尔值，用于记录是否已订阅
						AtomicBoolean subscribed = new AtomicBoolean();
						// 执行提交操作
						return doCommit(
								() -> {
									try {
										// 内部写入数据
										return writeWithInternal(Mono.fromCallable(() -> buffer)
												.doOnSubscribe(s -> subscribed.set(true))
												.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release));
									} catch (Throwable ex) {
										// 如果出现异常，则返回一个错误的 Mono
										return Mono.error(ex);
									}
								})
								// 处理错误，释放数据缓冲区
								.doOnError(ex -> DataBufferUtils.release(buffer))
								// 处理取消操作
								.doOnCancel(() -> {
									if (!subscribed.get()) {
										DataBufferUtils.release(buffer);
									}
								});
					})
					// 处理错误，清除内容头信息
					.doOnError(t -> getHeaders().clearContentHeaders())
					// 处理丢弃操作，释放数据缓冲区
					.doOnDiscard(PooledDataBuffer.class, DataBufferUtils::release);
		} else {
			// 否则，使用 ChannelSendOperator 包装 body，并执行提交操作
			return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeWithInternal(inner)))
					// 处理错误，清除内容头信息
					.doOnError(t -> getHeaders().clearContentHeaders());
		}
	}

	@Override
	public final Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
		return new ChannelSendOperator<>(body, inner -> doCommit(() -> writeAndFlushWithInternal(inner)))
				.doOnError(t -> getHeaders().clearContentHeaders());
	}

	@Override
	public Mono<Void> setComplete() {
		return !isCommitted() ? doCommit(null) : Mono.empty();
	}

	/**
	 * {@link #doCommit(Supplier)} 的一种变体，用于没有消息体的响应。
	 *
	 * @return 一个完成的发布者
	 */
	protected Mono<Void> doCommit() {
		return doCommit(null);
	}

	/**
	 * 应用{@link #beforeCommit(Supplier) beforeCommit}操作，应用响应状态和头/cookies，并写入响应体。
	 *
	 * @param writeAction 写入响应体的操作（可以为{@code null}）
	 * @return 一个完成的发布者
	 */
	protected Mono<Void> doCommit(@Nullable Supplier<? extends Mono<Void>> writeAction) {
		// 创建一个空的 Flux<Void>，用于保存所有操作
		Flux<Void> allActions = Flux.empty();

		// 如果状态从 NEW（新建）变为 COMMITTING（提交中）
		if (this.state.compareAndSet(State.NEW, State.COMMITTING)) {
			// 如果提交操作不为空
			if (!this.commitActions.isEmpty()) {
				// 将所有提交操作连接成一个 Flux，并在发生错误时执行清除内容头信息操作
				allActions = Flux.concat(Flux.fromIterable(this.commitActions).map(Supplier::get))
						.doOnError(ex -> {
							// 如果状态从 COMMITTING（提交中）变为 COMMIT_ACTION_FAILED（提交操作失败）
							if (this.state.compareAndSet(State.COMMITTING, State.COMMIT_ACTION_FAILED)) {
								// 清除内容头信息
								getHeaders().clearContentHeaders();
							}
						});
			}
		} else if (this.state.compareAndSet(State.COMMIT_ACTION_FAILED, State.COMMITTING)) {
			// 如果状态从 COMMIT_ACTION_FAILED（提交操作失败）变为 COMMITTING（提交中）
			// 跳过提交操作
		} else {
			// 如果状态既不是 NEW（新建）也不是 COMMIT_ACTION_FAILED（提交操作失败）
			// 返回一个空的 Mono
			return Mono.empty();
		}

		// 将应用状态码、头信息和 Cookie 的操作以及设置状态为 COMMITTED（已提交）的操作连接到 allActions 中
		allActions = allActions.concatWith(Mono.fromRunnable(() -> {
			applyStatusCode();
			applyHeaders();
			applyCookies();
			this.state.set(State.COMMITTED);
		}));

		// 如果 writeAction 不为空，则将其操作连接到 allActions 中
		if (writeAction != null) {
			allActions = allActions.concatWith(writeAction.get());
		}

		// 返回 allActions 的结果
		return allActions.then();
	}


	/**
	 * 向底层响应写入数据。
	 *
	 * @param body 要写入的发布者
	 */
	protected abstract Mono<Void> writeWithInternal(Publisher<? extends DataBuffer> body);

	/**
	 * 向底层响应写入数据，并在每个{@code Publisher<DataBuffer>}之后刷新。
	 *
	 * @param body 要写入和刷新的发布者
	 */
	protected abstract Mono<Void> writeAndFlushWithInternal(Publisher<? extends Publisher<? extends DataBuffer>> body);

	/**
	 * 将状态码应用到底层响应。
	 * 此方法仅调用一次。
	 */
	protected abstract void applyStatusCode();

	/**
	 * 在响应提交时调用，允许子类将头值应用到底层响应。
	 * <p>注意，大多数子类使用一个{@link HttpHeaders}实例，它包装了对本地响应头的适配器，使得更改会即时传播到底层响应。
	 * 这意味着此回调通常不被使用，除非用于特定的更新，例如在Servlet响应中设置contentType或characterEncoding字段。
	 */
	protected abstract void applyHeaders();

	/**
	 * 从{@link #getHeaders()}中添加cookies到底层响应。
	 * 此方法仅调用一次。
	 */
	protected abstract void applyCookies();

	/**
	 * 允许子类将提示与数据缓冲区关联，如果它是一个池化的缓冲区并支持泄漏跟踪。
	 *
	 * @param buffer 要附加提示的缓冲区
	 * @since 5.3.2
	 */
	protected void touchDataBuffer(DataBuffer buffer) {
	}

}
