/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.aop.target;

/**
 * ThreadLocal TargetSource 的统计信息。
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public interface ThreadLocalTargetSourceStats {

	/**
	 * 返回客户端调用次数。
	 */
	int getInvocationCount();

	/**
	 * 返回由线程绑定对象满足的命中次数。
	 */
	int getHitCount();

	/**
	 * 返回创建的线程绑定对象数量。
	 */
	int getObjectCount();

}
