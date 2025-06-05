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

package org.springframework.core.metrics.jfr;

import org.springframework.core.metrics.ApplicationStartup;
import org.springframework.core.metrics.StartupStep;

import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link ApplicationStartup} 针对 Java Flight Recorder 的实现。
 * <p>此变体将 {@link StartupStep} 记录为 Flight Recorder 事件。由于
 * 这类事件仅支持基础类型，
 * {@link org.springframework.core.metrics.StartupStep.Tags} 会被序列化为
 * 单个字符串属性。
 * <p>一旦在应用上下文中配置了此功能，可以通过启用录制启动应用来记录数据：
 * {@code java -XX:StartFlightRecording:filename=recording.jfr,duration=10s -jar app.jar}。
 *
 * @author Brian Clozel
 * @since 5.3
 */
public class FlightRecorderApplicationStartup implements ApplicationStartup {

	private final AtomicLong currentSequenceId = new AtomicLong(0);

	private final Deque<Long> currentSteps;


	public FlightRecorderApplicationStartup() {
		this.currentSteps = new ConcurrentLinkedDeque<>();
		this.currentSteps.offerFirst(this.currentSequenceId.get());
	}


	@Override
	public StartupStep start(String name) {
		long sequenceId = this.currentSequenceId.incrementAndGet();
		this.currentSteps.offerFirst(sequenceId);
		return new FlightRecorderStartupStep(sequenceId, name,
				this.currentSteps.getFirst(), committedStep -> this.currentSteps.removeFirstOccurrence(sequenceId));
	}

}
