/*
 * Copyright 2002-2007 the original author or authors.
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

import org.aopalliance.aop.Advice;

/**
 * 前置通知的通用标记接口，例如 {@link MethodBeforeAdvice}。
 *
 * <p>Spring 仅支持方法前置通知。尽管这不太可能改变，
 * 但此 API 被设计为在需要时允许将来支持字段通知。
 *
 * @author Rod Johnson
 * @see AfterAdvice
 */
public interface BeforeAdvice extends Advice {

}
