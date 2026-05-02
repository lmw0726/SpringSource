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

package org.springframework.aop.framework;

/**
 * 标记接口，指示 bean 是 Spring AOP 基础设施的一部分。
 * 特别是，这意味着任何此类 bean 都不受自动代理，
 * 即使切点会匹配它。
 *
 * @author Juergen Hoeller
 * @since 2.0.3
 * @see org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator
 * @see org.springframework.aop.scope.ScopedProxyFactoryBean
 */
public interface AopInfrastructureBean {

}
