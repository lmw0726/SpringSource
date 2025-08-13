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

package org.springframework.util.backoff;

/**
 * 简单的{@link BackOff}实现，提供固定的重试间隔和最大重试次数。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class FixedBackOff implements BackOff {

	/**
	 * 默认的重试间隔：5000毫秒（5秒）。
	 */
	public static final long DEFAULT_INTERVAL = 5000;

	/**
	 * 表示无限次重试尝试的常量值。
	 */
	public static final long UNLIMITED_ATTEMPTS = Long.MAX_VALUE;

	private long interval = DEFAULT_INTERVAL;

	private long maxAttempts = UNLIMITED_ATTEMPTS;


	/**
	 * 创建实例，使用{@value #DEFAULT_INTERVAL}毫秒的间隔和无限次重试。
	 */
	public FixedBackOff() {
	}

	/**
	 * 创建实例。
	 * @param interval 两次重试之间的间隔（毫秒）
	 * @param maxAttempts 最大重试次数
	 */
	public FixedBackOff(long interval, long maxAttempts) {
		this.interval = interval;
		this.maxAttempts = maxAttempts;
	}


	/**
	 * 设置两次重试之间的间隔（毫秒）。
	 * @param interval 重试间隔（毫秒）
	 */
	public void setInterval(long interval) {
		this.interval = interval;
	}

	/**
	 * 获取两次重试之间的间隔（毫秒）。
	 * @return 重试间隔（毫秒）
	 */
	public long getInterval() {
		return this.interval;
	}

	/**
	 * 设置最大重试次数。
	 * @param maxAttempts 最大重试次数
	 */
	public void setMaxAttempts(long maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	/**
	 * 获取最大重试次数。
	 * @return 最大重试次数
	 */
	public long getMaxAttempts() {
		return this.maxAttempts;
	}

	@Override
	public BackOffExecution start() {
		return new FixedBackOffExecution();
	}


	private class FixedBackOffExecution implements BackOffExecution {

		private long currentAttempts = 0;

		@Override
		public long nextBackOff() {
			this.currentAttempts++;
			if (this.currentAttempts <= getMaxAttempts()) {
				return getInterval();
			}
			else {
				return STOP;
			}
		}

		@Override
		public String toString() {
			String attemptValue = (FixedBackOff.this.maxAttempts == Long.MAX_VALUE ?
					"unlimited" : String.valueOf(FixedBackOff.this.maxAttempts));
			return "FixedBackOff{interval=" + FixedBackOff.this.interval +
					", currentAttempts=" + this.currentAttempts +
					", maxAttempts=" + attemptValue +
					'}';
		}
	}

}
