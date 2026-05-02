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
 * 所有由切点驱动的 Advisor 的上级接口。
 * 这涵盖了几乎所有 advisor，但不包括引介 advisor，
 * 因为方法级匹配不适用于引介 advisor。
 *
 * @author Rod Johnson
 */
public interface PointcutAdvisor extends Advisor {

	/**
	 * 获取驱动此 advisor 的 Pointcut。
	 */
	Pointcut getPointcut();

}
