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
 * Simple Map-based storage for {@link WebSession} instances.
 *
 * @author Rossen Stoyanchev
 * @author Rob Winch
 * @since 5.0
 */
public class InMemoryWebSessionStore implements WebSessionStore {

	private static final IdGenerator idGenerator = new JdkIdGenerator();


	private int maxSessions = 10000;

	private Clock clock = Clock.system(ZoneId.of("GMT"));

	private final Map<String, InMemoryWebSession> sessions = new ConcurrentHashMap<>();

	private final ExpiredSessionChecker expiredSessionChecker = new ExpiredSessionChecker();


	/**
	 * Set the maximum number of sessions that can be stored. Once the limit is
	 * reached, any attempt to store an additional session will result in an
	 * {@link IllegalStateException}.
	 * <p>By default set to 10000.
	 *
	 * @param maxSessions the maximum number of sessions
	 * @since 5.0.8
	 */
	public void setMaxSessions(int maxSessions) {
		this.maxSessions = maxSessions;
	}

	/**
	 * Return the maximum number of sessions that can be stored.
	 *
	 * @since 5.0.8
	 */
	public int getMaxSessions() {
		return this.maxSessions;
	}

	/**
	 * Configure the {@link Clock} to use to set lastAccessTime on every created
	 * session and to calculate if it is expired.
	 * <p>This may be useful to align to different timezone or to set the clock
	 * back in a test, e.g. {@code Clock.offset(clock, Duration.ofMinutes(-31))}
	 * in order to simulate session expiration.
	 * <p>By default this is {@code Clock.system(ZoneId.of("GMT"))}.
	 *
	 * @param clock the clock to use
	 */
	public void setClock(Clock clock) {
		Assert.notNull(clock, "Clock is required");
		this.clock = clock;
		removeExpiredSessions();
	}

	/**
	 * Return the configured clock for session lastAccessTime calculations.
	 */
	public Clock getClock() {
		return this.clock;
	}

	/**
	 * Return the map of sessions with an {@link Collections#unmodifiableMap
	 * unmodifiable} wrapper. This could be used for management purposes, to
	 * list active sessions, invalidate expired ones, etc.
	 *
	 * @since 5.0.8
	 */
	public Map<String, WebSession> getSessions() {
		return Collections.unmodifiableMap(this.sessions);
	}


	@Override
	public Mono<WebSession> createWebSession() {

		// Opportunity to clean expired sessions
		Instant now = this.clock.instant();
		this.expiredSessionChecker.checkIfNecessary(now);

		return Mono.<WebSession>fromSupplier(() -> new InMemoryWebSession(now))
				.subscribeOn(Schedulers.boundedElastic())
				.publishOn(Schedulers.parallel());
	}

	@Override
	public Mono<WebSession> retrieveSession(String id) {
		Instant now = this.clock.instant();
		this.expiredSessionChecker.checkIfNecessary(now);
		InMemoryWebSession session = this.sessions.get(id);
		if (session == null) {
			return Mono.empty();
		} else if (session.isExpired(now)) {
			this.sessions.remove(id);
			return Mono.empty();
		} else {
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
			((InMemoryWebSession) session).updateLastAccessTime(this.clock.instant());
			return session;
		});
	}

	/**
	 * Check for expired sessions and remove them. Typically such checks are
	 * kicked off lazily during calls to {@link #createWebSession() create} or
	 * {@link #retrieveSession retrieve}, no less than 60 seconds apart.
	 * This method can be called to force a check at a specific time.
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
		 * Max time between expiration checks.
		 */
		private static final int CHECK_PERIOD = 60 * 1000;


		private final ReentrantLock lock = new ReentrantLock();

		private Instant checkTime = clock.instant().plus(CHECK_PERIOD, ChronoUnit.MILLIS);


		public void checkIfNecessary(Instant now) {
			if (this.checkTime.isBefore(now)) {
				removeExpiredSessions(now);
			}
		}

		public void removeExpiredSessions(Instant now) {
			if (sessions.isEmpty()) {
				return;
			}
			if (this.lock.tryLock()) {
				try {
					Iterator<InMemoryWebSession> iterator = sessions.values().iterator();
					while (iterator.hasNext()) {
						InMemoryWebSession session = iterator.next();
						if (session.isExpired(now)) {
							iterator.remove();
							session.invalidate();
						}
					}
				} finally {
					this.checkTime = now.plus(CHECK_PERIOD, ChronoUnit.MILLIS);
					this.lock.unlock();
				}
			}
		}
	}


	private enum State {NEW, STARTED, EXPIRED}

}
