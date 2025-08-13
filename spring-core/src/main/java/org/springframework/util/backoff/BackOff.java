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

package org.springframework.util.backoff;

/**
 * 提供一个{@link BackOffExecution}，指示操作应按照何种速率进行重试。
 *
 * <p>此接口的使用者应按以下方式使用：
 *
 * <pre class="code">
 * BackOffExecution exec = backOff.start();
 *
 * // 在操作恢复/重试循环中：
 * long waitInterval = exec.nextBackOff();
 * if (waitInterval == BackOffExecution.STOP) {
 *     // 不重试操作
 * }
 * else {
 *     // 休眠，例如 Thread.sleep(waitInterval)
 *     // 重试操作
 * }
 * }</pre>
 *
 * 一旦底层操作成功完成，执行实例可以直接丢弃。
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see BackOffExecution
 */
@FunctionalInterface
public interface BackOff {

	/**
	 * 开始一个新的退避执行。
	 * @return 一个新的{@link BackOffExecution}实例，准备使用
	 */
	BackOffExecution start();

}
