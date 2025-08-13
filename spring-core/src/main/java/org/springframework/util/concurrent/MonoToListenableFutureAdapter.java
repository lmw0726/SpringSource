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

package org.springframework.util.concurrent;

import reactor.core.publisher.Mono;

/**
 * 通过从{@link Mono}获取{@code CompletableFuture}（通过{@link Mono#toFuture()}），
 * 并使用{@link CompletableToListenableFutureAdapter}进行适配，
 * 将{@link Mono}适配为{@link ListenableFuture}。
 *
 * @author Rossen Stoyanchev
 * @author Stephane Maldini
 * @since 5.1
 * @param <T> 对象类型
 */
public class MonoToListenableFutureAdapter<T> extends CompletableToListenableFutureAdapter<T> {

	public MonoToListenableFutureAdapter(Mono<T> mono) {
		super(mono.toFuture());
	}

}
