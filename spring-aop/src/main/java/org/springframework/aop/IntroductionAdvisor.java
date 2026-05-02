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
 * 执行一个或多个 AOP <b>引介</b>的 advisor 的上级接口。
 *
 * <p>此接口不能直接实现；子接口必须提供实现引介的通知类型。
 *
 * <p>引介是通过 AOP 通知实现附加接口（目标对象未实现的接口）。
 *
 * @author Rod Johnson
 * @since 04.04.2003
 * @see IntroductionInterceptor
 */
public interface IntroductionAdvisor extends Advisor, IntroductionInfo {

	/**
	 * 返回用于确定此引介应应用于哪些目标类的过滤器。
	 * <p>这表示切点的类部分。注意，方法匹配对引介没有意义。
	 * @return 类过滤器
	 */
	ClassFilter getClassFilter();

	/**
	 * 被通知的接口能否由引介通知实现？
	 * 在添加 IntroductionAdvisor 之前调用。
	 * @throws IllegalArgumentException 如果被通知的接口不能由引介通知实现
	 */
	void validateInterfaces() throws IllegalArgumentException;

}
