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

package org.springframework.aop;

/**
 * 提供描述引介所需信息的接口。
 *
 * <p>{@link IntroductionAdvisor IntroductionAdvisors} 必须实现此接口。
 * 如果某个 {@link org.aopalliance.aop.Advice} 实现了此接口，
 * 则可以在没有 {@link IntroductionAdvisor} 的情况下将其用作引介。
 * 在这种情况下，该通知是自描述的：它不仅提供必要行为，
 * 还描述其引介的接口。
 *
 * @author Rod Johnson
 * @since 1.1.1
 */
public interface IntroductionInfo {

	/**
	 * 返回此 Advisor 或 Advice 引介的附加接口。
	 * @return 被引介的接口
	 */
	Class<?>[] getInterfaces();

}
