/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.http.server.reactive;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import javax.net.ssl.SSLSession;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link SslInfo} 的默认实现。
 *
 * @author Rossen Stoyanchev
 * @since 5.0.2
 */
final class DefaultSslInfo implements SslInfo {
	/**
	 * 会话编号
	 */
	@Nullable
	private final String sessionId;

	/**
	 * 与请求关联的SSL证书
	 */
	@Nullable
	private final X509Certificate[] peerCertificates;


	DefaultSslInfo(@Nullable String sessionId, X509Certificate[] peerCertificates) {
		Assert.notNull(peerCertificates, "No SSL certificates");
		this.sessionId = sessionId;
		this.peerCertificates = peerCertificates;
	}

	DefaultSslInfo(SSLSession session) {
		Assert.notNull(session, "SSLSession is required");
		this.sessionId = initSessionId(session);
		this.peerCertificates = initCertificates(session);
	}


	@Override
	@Nullable
	public String getSessionId() {
		return this.sessionId;
	}

	@Override
	@Nullable
	public X509Certificate[] getPeerCertificates() {
		return this.peerCertificates;
	}


	@Nullable
	private static String initSessionId(SSLSession session) {
		// 获取会话 ID 的字节数组
		byte[] bytes = session.getId();

		// 如果字节数组为空，则返回 null
		if (bytes == null) {
			return null;
		}

		// 创建 字符串构建器，用于存储十六进制字符串的
		StringBuilder sb = new StringBuilder();

		// 遍历字节数组中的每个字节
		for (byte b : bytes) {
			// 将字节转换为十六进制字符串表示
			String digit = Integer.toHexString(b);

			// 如果十六进制字符串长度小于 2，则在前面补 '0'
			if (digit.length() < 2) {
				sb.append('0');
			}

			// 如果十六进制字符串长度大于 2，则截取最后两位
			if (digit.length() > 2) {
				digit = digit.substring(digit.length() - 2);
			}

			// 将处理后的十六进制字符串追加到 字符串构建器 中
			sb.append(digit);
		}

		// 返回拼接后的十六进制表示的会话 ID 字符串
		return sb.toString();
	}

	@Nullable
	private static X509Certificate[] initCertificates(SSLSession session) {
		// 声明证书数组
		Certificate[] certificates;

		try {
			// 尝试从会话中获取对等端的证书
			certificates = session.getPeerCertificates();
		} catch (Throwable ex) {
			// 捕获可能的异常，如果失败则返回 null
			return null;
		}

		// 创建一个存储 X509Certificate 的列表
		List<X509Certificate> result = new ArrayList<>(certificates.length);

		// 遍历获取的证书数组
		for (Certificate certificate : certificates) {
			// 检查每个证书是否属于 X509Certificate 类型
			if (certificate instanceof X509Certificate) {
				// 如果是，将其转换为 X509Certificate 并添加到结果列表中
				result.add((X509Certificate) certificate);
			}
		}

		// 如果结果列表不为空，则将其转换为 X509Certificate 数组返回；
		// 否则返回 null
		return (!result.isEmpty() ? result.toArray(new X509Certificate[0]) : null);
	}

}
