/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.server.session;

import org.springframework.util.Assert;
import org.springframework.util.IdGenerator;
import org.springframework.util.JdkIdGenerator;
import org.springframework.web.server.WebSession;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 基于Map的简单的{@link WebSession} 实例的存储。
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public class InMemoryWebSessionStore implements WebSessionStore {
	/**
	 * ID生成器
	 */
	private static final IdGenerator idGenerator = new JdkIdGenerator();

	/**
	 * 会话的最大数量
	 */
	private int maxSessions = 10000;

	/**
	 * 要使用的时钟
	 */
	private Clock clock = Clock.system(ZoneId.of("GMT"));

	/**
	 * 会话编号 —— 内存Web会话 映射
	 */
	private final Map<String, InMemoryWebSession> sessions = new ConcurrentHashMap<>();

	/**
	 * 过期会话检查器
	 */
	private final ExpiredSessionChecker expiredSessionChecker = new ExpiredSessionChecker();


	/**
	 * 设置可以存储的会话的最大数量。一旦达到限制，任何尝试存储额外会话的操作都将导致{@link IllegalStateException}。
	 * <p>默认设置为10000。
	 *
	 * @param maxSessions 最大会话数
	 * @since 5.0.8
	 */
	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}

	/**
	 * 返回可以存储的会话的最大数量。
	 *
	 * @since 5.0.8
	 */
	public int getMaxSessions() {
		return this.maxSessions;
	}

	/**
	 * 配置用于设置每个创建的会话的lastAccessTime和计算其是否已过期的{@link Clock}。
	 * <p>这可能对齐不同的时区或在测试中将时钟设置回去有用，例如{@code Clock.offset(clock, Duration.ofMinutes(-31))}，以模拟会话过期。
	 * <p>默认为{@code Clock.system(ZoneId.of("GMT"))}。
	 *
	 * @param clock 要使用的时钟
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "Clock is required");
		this.clock = clock;
		removeExpiredSessions();
	}

	/**
	 * 返回用于会话lastAccessTime计算的配置时钟。
	 */
	public Clock getClock() {
		return this.clock;
	}

	/**
	 * 返回具有{@link Collections#unmodifiableMap unmodifiable}包装器的会话映射。
	 * 这可以用于管理目的，列出活动会话，使过期会话无效等。
	 *
	 * @since 5.0.8
	 */
	public Map<String, WebSession> getSessions() {
		return Collections.unmodifiableMap(this.sessions);
	}


	@Override
	public Mono<WebSession> createWebSession() {

		// 清理过期会话的机会
		Instant now = this.clock.instant();
		this.expiredSessionChecker.checkIfNecessary(now);

		return Mono.<WebSession>fromSupplier(() -> new InMemoryWebSession(now))
				// 在有界弹性调度器上执行
				.subscribeOn(Schedulers.boundedElastic())
				// 在并行调度器上发布
				.publishOn(Schedulers.parallel());
	}

	@Override
	public Mono<WebSession> retrieveSession(String id) {
		// 获取当前时间
		Instant now = this.clock.instant();
		// 检查是否需要检查过期会话
		this.expiredSessionChecker.checkIfNecessary(now);
		// 从sessions中获取id对应的会话
		InMemoryWebSession session = this.sessions.get(id);
		if (session == null) {
			// 如果会话为空，则返回一个空的Mono
			return Mono.empty();
		} else if (session.isExpired(now)) {
			// 如果会话已经过期，则从sessions中移除该会话，并返回一个空的Mono
			this.sessions.remove(id);
			return Mono.empty();
		} else {
			// 更新会话的最后访问时间为当前时间，并返回一个包含该会话的Mono
			session.updateLastAccessTime(now);
			return Mono.just(session);
		}
	}

	@Override
	public Mono<Void> removeSession(String id) {
		this.sessions.remove(id);
		return Mono.empty();
	}

	@Override
	public Mono<WebSession> updateLastAccessTime(WebSession session) {
		return Mono.fromSupplier(() -> {
			Assert.isInstanceOf(InMemoryWebSession.class, session);
			// 更新会话的最后访问时间为当前时间
			((InMemoryWebSession) session).updateLastAccessTime(this.clock.instant());
			// 返回更新后的会话
			return session;
		});
	}

	/**
	 * 检查过期会话并移除它们。通常这样的检查在调用{@link #createWebSession() create}或{@link #retrieveSession retrieve}时被懒惰地启动，间隔不少于60秒。
	 * 可以调用此方法在特定时间强制进行检查。
	 *
	 * @since 5.0.8
	 */
	public void removeExpiredSessions() {
		this.expiredSessionChecker.removeExpiredSessions(this.clock.instant());
	}


	private class InMemoryWebSession implements WebSession {
		/**
		 * 生成的会话ID
		 */
		private final AtomicReference<String> id = new AtomicReference<>(String.valueOf(idGenerator.generateId()));

		/**
		 * 属性
		 */
		private final Map<String, Object> attributes = new ConcurrentHashMap<>();

		/**
		 * 创建时间
		 */
		private final Instant creationTime;

		/**
		 * 最后进入时间
		 */
		private volatile Instant lastAccessTime;

		/**
		 * 最大空闲时间，默认为30分钟
		 */
		private volatile Duration maxIdleTime = Duration.ofMinutes(30);

		/**
		 * 状态
		 */
		private final AtomicReference<State> state = new AtomicReference<>(State.NEW);


		/**
		 * 创建一个内存中的 Web 会话。
		 *
		 * @param creationTime 会话创建时间
		 */
		public InMemoryWebSession(Instant creationTime) {
			this.creationTime = creationTime;
			this.lastAccessTime = this.creationTime;
		}

		@Override
		public String getId() {
			return this.id.get();
		}

		@Override
		public Map<String, Object> getAttributes() {
			return this.attributes;
		}

		@Override
		public Instant getCreationTime() {
			return this.creationTime;
		}

		@Override
		public Instant getLastAccessTime() {
			return this.lastAccessTime;
		}

		@Override
		public void setMaxIdleTime(Duration maxIdleTime) {
			this.maxIdleTime = maxIdleTime;
		}

		@Override
		public Duration getMaxIdleTime() {
			return this.maxIdleTime;
		}

		@Override
		public void start() {
			this.state.compareAndSet(State.NEW, State.STARTED);
		}

		@Override
		public boolean isStarted() {
			return this.state.get().equals(State.STARTED) || !getAttributes().isEmpty();
		}

		@Override
		public Mono<Void> changeSessionId() {
			// 获取当前会话的ID
			String currentId = this.id.get();
			// 从内存中移除当前会话
			InMemoryWebSessionStore.this.sessions.remove(currentId);
			// 生成新的会话ID
			String newId = String.valueOf(idGenerator.generateId());
			// 设置新的会话ID
			this.id.set(newId);
			// 将当前会话存储到内存中
			InMemoryWebSessionStore.this.sessions.put(this.getId(), this);
			// 返回一个空的Mono
			return Mono.empty();
		}

		@Override
		public Mono<Void> invalidate() {
			// 设置会话状态为已过期
			this.state.set(State.EXPIRED);
			// 清空会话属性
			getAttributes().clear();
			// 从内存中移除该会话
			InMemoryWebSessionStore.this.sessions.remove(this.id.get());
			// 返回一个空的Mono
			return Mono.empty();
		}

		@Override
		public Mono<Void> save() {

			// 检查最大会话限制
			checkMaxSessionsLimit();

			// 隐式启动的会话..
			if (!getAttributes().isEmpty()) {
				this.state.compareAndSet(State.NEW, State.STARTED);
			}

			if (isStarted()) {
				// 保存
				InMemoryWebSessionStore.this.sessions.put(this.getId(), this);

				// 除非它已被使无效
				if (this.state.get().equals(State.EXPIRED)) {
					// 移除会话编号
					InMemoryWebSessionStore.this.sessions.remove(this.getId());
					// 返回异常
					return Mono.error(new IllegalStateException("Session was invalidated"));
				}
			}

			return Mono.empty();
		}

		/**
		 * 检查最大会话限制。
		 */
		private void checkMaxSessionsLimit() {
			// 如果当前会话数达到最大会话数限制
			if (sessions.size() >= maxSessions) {
				// 移除过期的会话
				expiredSessionChecker.removeExpiredSessions(clock.instant());
				// 再次检查当前会话数是否达到最大会话数限制
				if (sessions.size() >= maxSessions) {
					// 如果是，则抛出异常
					throw new IllegalStateException("Max sessions limit reached: " + sessions.size());
				}
			}
		}

		@Override
		public boolean isExpired() {
			return isExpired(clock.instant());
		}

		/**
		 * 检查会话是否已过期。
		 *
		 * @param now 当前时间
		 * @return 如果会话已过期，则返回 {@code true}，否则返回 {@code false}
		 */
		private boolean isExpired(Instant now) {
			// 如果会话状态为已过期，则返回true
			if (this.state.get().equals(State.EXPIRED)) {
				return true;
			}
			if (checkExpired(now)) {
				// 如果检查会话是否过期并且已过期，则设置会话状态为已过期，并返回true
				this.state.set(State.EXPIRED);
				return true;
			}
			// 否则，返回false
			return false;
		}

		/**
		 * 检查会话是否已过期。
		 *
		 * @param currentTime 当前时间
		 * @return 如果会话已过期，则返回 {@code true}，否则返回 {@code false}
		 */
		private boolean checkExpired(Instant currentTime) {
			return isStarted() && !this.maxIdleTime.isNegative() &&
					currentTime.minus(this.maxIdleTime).isAfter(this.lastAccessTime);
		}

		/**
		 * 更新最后访问时间。
		 *
		 * @param currentTime 当前时间
		 */
		private void updateLastAccessTime(Instant currentTime) {
			this.lastAccessTime = currentTime;
		}
	}


	private class ExpiredSessionChecker {

		/**
		 * 到期检查之间的最长时间。
		 */
		private static final int CHECK_PERIOD = 60 * 1000;

		/**
		 * 会话锁
		 */
		private final ReentrantLock lock = new ReentrantLock();

		/**
		 * 检查时间
		 */
		private Instant checkTime = clock.instant().plus(CHECK_PERIOD, ChronoUnit.MILLIS);


		public void checkIfNecessary(Instant now) {
			if (this.checkTime.isBefore(now)) {
				// 如果上次检查时间早于当前时间，则删除过期会话
				removeExpiredSessions(now);
			}
		}

		public void removeExpiredSessions(Instant now) {
			// 如果会话列表为空，则直接返回
			if (sessions.isEmpty()) {
				return;
			}

			// 尝试获取锁，如果成功获取到锁，则执行过期会话的清理工作
			if (this.lock.tryLock()) {
				try {
					// 使用迭代器遍历会话列表
					Iterator<InMemoryWebSession> iterator = sessions.values().iterator();
					while (iterator.hasNext()) {
						InMemoryWebSession session = iterator.next();
						if (session.isExpired(now)) {
							// 如果会话过期，将其移除
							iterator.remove();
							// 使会话无效
							session.invalidate();
						}
					}
				} finally {
					// 更新检查时间，并释放锁
					this.checkTime = now.plus(CHECK_PERIOD, ChronoUnit.MILLIS);
					this.lock.unlock();
				}
			}
		}
	}


	private enum State {NEW, STARTED, EXPIRED}

}
