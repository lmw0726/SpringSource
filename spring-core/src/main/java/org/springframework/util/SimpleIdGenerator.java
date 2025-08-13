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

package org.springframework.util;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 一个简单的 {@link IdGenerator}，起始值为1，递增直到
 * {@link Long#MAX_VALUE}，然后重新从头开始。
 *
 * @author Rossen Stoyanchev
 * @since 4.1.5
 */
public class SimpleIdGenerator implements IdGenerator {

	private final AtomicLong leastSigBits = new AtomicLong();


	@Override
	public UUID generateId() {
		return new UUID(0, this.leastSigBits.incrementAndGet());
	}

}
