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

package org.springframework.util.backoff;

import org.springframework.util.Assert;

/**
 * {@link BackOff}接口的实现类，每次重试尝试都会增加退避时间间隔。
 * 当间隔达到{@link #setMaxInterval(long) 最大间隔}后，将不再增加。
 * 一旦达到{@link #setMaxElapsedTime(long) 最大经过时间}，将停止重试。
 *
 * <p>示例：默认间隔为{@value #DEFAULT_INITIAL_INTERVAL}毫秒，
 * 默认乘数为{@value #DEFAULT_MULTIPLIER}，默认最大间隔为{@value #DEFAULT_MAX_INTERVAL}。
 * 对于10次尝试，序列将如下：
 *
 * <pre>
 * 请求# 		 退避时间
 *
 *  1              2000
 *  2              3000
 *  3              4500
 *  4              6750
 *  5             10125
 *  6             15187
 *  7             22780
 *  8             30000
 *  9             30000
 * 10             30000
 * </pre>
 *
 * <p>注意默认最大经过时间是{@link Long#MAX_VALUE}。可以使用
 * {@link #setMaxElapsedTime(long)}来限制实例在返回
 * {@link BackOffExecution#STOP}前应累积的最大时间长度。
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class ExponentialBackOff implements BackOff {

	/**
	 * 默认初始间隔（毫秒）。
	 */
	public static final long DEFAULT_INITIAL_INTERVAL = 2000L;

	/**
	 * 默认乘数（将间隔增加50%）。
	 */
	public static final double DEFAULT_MULTIPLIER = 1.5;

	/**
	 * 默认最大退避时间（毫秒）。
	 */
	public static final long DEFAULT_MAX_INTERVAL = 30000L;

	/**
	 * 默认最大经过时间（毫秒）。
	 */
	public static final long DEFAULT_MAX_ELAPSED_TIME = Long.MAX_VALUE;


	private long initialInterval = DEFAULT_INITIAL_INTERVAL;

	private double multiplier = DEFAULT_MULTIPLIER;

	private long maxInterval = DEFAULT_MAX_INTERVAL;

	private long maxElapsedTime = DEFAULT_MAX_ELAPSED_TIME;


	/**
	 * 使用默认设置创建实例。
	 * @see #DEFAULT_INITIAL_INTERVAL
	 * @see #DEFAULT_MULTIPLIER
	 * @see #DEFAULT_MAX_INTERVAL
	 * @see #DEFAULT_MAX_ELAPSED_TIME
	 */
	public ExponentialBackOff() {
	}

	/**
	 * 使用指定设置创建实例。
	 * @param initialInterval 初始间隔时间（毫秒）
	 * @param multiplier 乘数因子（应大于等于1）
	 * @throws IllegalArgumentException 如果乘数小于1
	 */
	public ExponentialBackOff(long initialInterval, double multiplier) {
		checkMultiplier(multiplier);
		this.initialInterval = initialInterval;
		this.multiplier = multiplier;
	}


	/**
	 * 设置初始间隔时间（毫秒）。
	 * @param initialInterval 初始间隔时间（毫秒）
	 */
	public void setInitialInterval(long initialInterval) {
		this.initialInterval = initialInterval;
	}

	/**
	 * 获取初始间隔时间（毫秒）。
	 * @return 初始间隔时间（毫秒）
	 */
	public long getInitialInterval() {
		return this.initialInterval;
	}

	/**
	 * 设置每次重试时当前间隔时间的乘数因子。
	 * @param multiplier 乘数因子（应大于等于1）
	 * @throws IllegalArgumentException 如果乘数小于1
	 */
	public void setMultiplier(double multiplier) {
		checkMultiplier(multiplier);
		this.multiplier = multiplier;
	}

	/**
	 * 返回每次重试时当前间隔时间的乘数因子。
	 */
	public double getMultiplier() {
		return this.multiplier;
	}

	/**
	 * 设置最大退避时间（毫秒）。
	 * @param maxInterval 最大间隔时间（毫秒）
	 */
	public void setMaxInterval(long maxInterval) {
		this.maxInterval = maxInterval;
	}

	/**
	 * 获取最大退避时间（毫秒）。
	 * @return 最大间隔时间（毫秒）
	 */
	public long getMaxInterval() {
		return this.maxInterval;
	}

	/**
	 * 设置最大经过时间（毫秒），超过此时间后调用
	 * {@link BackOffExecution#nextBackOff()}将返回{@link BackOffExecution#STOP}。
	 * @param maxElapsedTime 最大经过时间（毫秒）
	 */
	public void setMaxElapsedTime(long maxElapsedTime) {
		this.maxElapsedTime = maxElapsedTime;
	}

	/**
	 * 获取最大经过时间（毫秒），超过此时间后调用
	 * {@link BackOffExecution#nextBackOff()}将返回{@link BackOffExecution#STOP}。
	 * @return 最大经过时间（毫秒）
	 */
	public long getMaxElapsedTime() {
		return this.maxElapsedTime;
	}

	@Override
	public BackOffExecution start() {
		return new ExponentialBackOffExecution();
	}

	private void checkMultiplier(double multiplier) {
		Assert.isTrue(multiplier >= 1, () -> "Invalid multiplier '" + multiplier + "'. Should be greater than " +
					"or equal to 1. A multiplier of 1 is equivalent to a fixed interval.");
	}


	private class ExponentialBackOffExecution implements BackOffExecution {

		private long currentInterval = -1;

		private long currentElapsedTime = 0;

		@Override
		public long nextBackOff() {
			if (this.currentElapsedTime >= maxElapsedTime) {
				return STOP;
			}

			long nextInterval = computeNextInterval();
			this.currentElapsedTime += nextInterval;
			return nextInterval;
		}

		private long computeNextInterval() {
			long maxInterval = getMaxInterval();
			if (this.currentInterval >= maxInterval) {
				return maxInterval;
			}
			else if (this.currentInterval < 0) {
				long initialInterval = getInitialInterval();
				this.currentInterval = Math.min(initialInterval, maxInterval);
			}
			else {
				this.currentInterval = multiplyInterval(maxInterval);
			}
			return this.currentInterval;
		}

		private long multiplyInterval(long maxInterval) {
			long i = this.currentInterval;
			i *= getMultiplier();
			return Math.min(i, maxInterval);
		}


		@Override
		public String toString() {
			StringBuilder sb = new StringBuilder("ExponentialBackOff{");
			sb.append("currentInterval=").append(this.currentInterval < 0 ? "n/a" : this.currentInterval + "ms");
			sb.append(", multiplier=").append(getMultiplier());
			sb.append('}');
			return sb.toString();
		}
	}

}
