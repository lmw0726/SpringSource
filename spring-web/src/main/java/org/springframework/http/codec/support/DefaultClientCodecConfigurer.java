/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.http.codec.support;

import org.springframework.http.codec.ClientCodecConfigurer;
import org.springframework.http.codec.HttpMessageWriter;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link ClientCodecConfigurer} 的默认实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public class DefaultClientCodecConfigurer extends BaseCodecConfigurer implements ClientCodecConfigurer {


	public DefaultClientCodecConfigurer() {
		super(new ClientDefaultCodecsImpl());
		((ClientDefaultCodecsImpl) defaultCodecs()).setPartWritersSupplier(this::getPartWriters);
	}

	private DefaultClientCodecConfigurer(DefaultClientCodecConfigurer other) {
		super(other);
		((ClientDefaultCodecsImpl) defaultCodecs()).setPartWritersSupplier(this::getPartWriters);
	}


	@Override
	public ClientDefaultCodecs defaultCodecs() {
		return (ClientDefaultCodecs) super.defaultCodecs();
	}

	@Override
	public DefaultClientCodecConfigurer clone() {
		return new DefaultClientCodecConfigurer(this);
	}

	@Override
	protected BaseDefaultCodecs cloneDefaultCodecs() {
		return new ClientDefaultCodecsImpl((ClientDefaultCodecsImpl) defaultCodecs());
	}

	private List<HttpMessageWriter<?>> getPartWriters() {
		// 创建一个空的结果列表
		List<HttpMessageWriter<?>> result = new ArrayList<>();

		// 将自定义编解码器中的类型化编码器添加到结果列表中
		result.addAll(this.customCodecs.getTypedWriters().keySet());

		// 将默认编解码器中的基本类型化编码器添加到结果列表中
		result.addAll(this.defaultCodecs.getBaseTypedWriters());

		// 将自定义编解码器中的对象编码器添加到结果列表中
		result.addAll(this.customCodecs.getObjectWriters().keySet());

		// 将默认编解码器中的基本对象编码器添加到结果列表中
		result.addAll(this.defaultCodecs.getBaseObjectWriters());

		// 将默认编解码器中的通用编码器添加到结果列表中
		result.addAll(this.defaultCodecs.getCatchAllWriters());

		// 返回填充了各种编码器的结果列表
		return result;
	}

}
