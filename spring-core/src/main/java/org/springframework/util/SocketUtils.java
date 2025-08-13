/*
 * Copyright 2002-2022 the original author or authors.
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

import javax.net.ServerSocketFactory;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * 用于网络套接字的简单工具方法，例如用于查找 {@code localhost} 上可用的端口。
 *
 * <p>在此类中，TCP 端口指的是 {@link ServerSocket} 使用的端口；
 * 而 UDP 端口指的是 {@link DatagramSocket} 使用的端口。
 *
 * <p>{@code SocketUtils} 于 Spring Framework 4.0 中引入，主要用于
 * 帮助编写启动外部服务器的集成测试，服务器会选择一个可用的随机端口。
 * 但是，这些工具无法保证之后该端口的可用性，因此不可靠。
 * 相比使用 {@code SocketUtils} 来查找服务器的本地可用端口，更推荐依赖服务器
 * 自身选择或由操作系统分配的随机端口，并通过查询服务器获取当前使用的端口。
 *
 * @author Sam Brannen
 * @author Ben Hale
 * @author Arjen Poutsma
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 4.0
 * @deprecated 自 Spring Framework 5.3.16 起废弃，将于 6.0 版本移除；
 * 请参阅 {@link SocketUtils 类级 Javadoc} 了解详情。
 */
@Deprecated
public class SocketUtils {

	/**
	 * 用于查找可用套接字端口时的默认最小端口范围值。
	 */
	public static final int PORT_RANGE_MIN = 1024;

	/**
	 * 用于查找可用套接字端口时的默认最大端口范围值。
	 */
	public static final int PORT_RANGE_MAX = 65535;


	private static final Random random = new Random(System.nanoTime());


	/**
	 * 尽管 {@code SocketUtils} 仅包含静态工具方法，
	 * 此构造函数仍故意声明为 {@code public}。
	 * <h4>理由</h4>
	 * <p>此类的静态方法可以通过 Spring 表达式语言（SpEL）在 XML 配置文件中调用，示例如下。
	 * <pre><code>&lt;bean id="bean1" ... p:port="#{T(org.springframework.util.SocketUtils).findAvailableTcpPort(12000)}" /&gt;</code></pre>
	 * 如果构造函数是 {@code private}，则每次使用 SpEL 的 {@code T()} 函数时，都必须提供全限定类名。
	 * 因此，将构造函数声明为 {@code public} 可以减少 SpEL 配置的冗余，示例如下。
	 * <pre><code>&lt;bean id="socketUtils" class="org.springframework.util.SocketUtils" /&gt;
	 * &lt;bean id="bean1" ... p:port="#{socketUtils.findAvailableTcpPort(12000)}" /&gt;
	 * &lt;bean id="bean2" ... p:port="#{socketUtils.findAvailableTcpPort(30000)}" /&gt;</code></pre>
	 */
	public SocketUtils() {
	}


	/**
	 * 从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 TCP 端口。
	 * @return 一个可用的 TCP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableTcpPort() {
		return findAvailableTcpPort(PORT_RANGE_MIN);
	}

	/**
	 * 从范围 [{@code minPort}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 TCP 端口。
	 * @param minPort 最小端口号
	 * @return 一个可用的 TCP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableTcpPort(int minPort) {
		return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
	}

	/**
	 * 从范围 [{@code minPort}, {@code maxPort}] 中随机选择一个可用的 TCP 端口。
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 一个可用的 TCP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableTcpPort(int minPort, int maxPort) {
		return SocketType.TCP.findAvailablePort(minPort, maxPort);
	}

	/**
	 * 查找指定数量的可用 TCP 端口，每个端口均从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}] 中随机选择。
	 * @param numRequested 需要查找的可用端口数量
	 * @return 一个已排序的可用 TCP 端口号集合
	 * @throws IllegalStateException 如果找不到足够数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableTcpPorts(int numRequested) {
		return findAvailableTcpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * 查找指定数量的可用 TCP 端口，每个端口均从范围 [{@code minPort}, {@code maxPort}] 中随机选择。
	 * @param numRequested 需要查找的可用端口数量
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 一个已排序的可用 TCP 端口号集合
	 * @throws IllegalStateException 如果找不到足够数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableTcpPorts(int numRequested, int minPort, int maxPort) {
		return SocketType.TCP.findAvailablePorts(numRequested, minPort, maxPort);
	}

	/**
	 * 从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 UDP 端口。
	 * @return 一个可用的 UDP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableUdpPort() {
		return findAvailableUdpPort(PORT_RANGE_MIN);
	}

	/**
	 * 从范围 [{@code minPort}, {@value #PORT_RANGE_MAX}] 中随机选择一个可用的 UDP 端口。
	 * @param minPort 最小端口号
	 * @return 一个可用的 UDP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableUdpPort(int minPort) {
		return findAvailableUdpPort(minPort, PORT_RANGE_MAX);
	}

	/**
	 * 从范围 [{@code minPort}, {@code maxPort}] 中随机选择一个可用的 UDP 端口。
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 一个可用的 UDP 端口号
	 * @throws IllegalStateException 如果找不到可用端口
	 */
	public static int findAvailableUdpPort(int minPort, int maxPort) {
		return SocketType.UDP.findAvailablePort(minPort, maxPort);
	}

	/**
	 * 查找指定数量的可用 UDP 端口，每个端口均从范围 [{@value #PORT_RANGE_MIN}, {@value #PORT_RANGE_MAX}] 中随机选择。
	 * @param numRequested 需要查找的可用端口数量
	 * @return 一个已排序的可用 UDP 端口号集合
	 * @throws IllegalStateException 如果找不到足够数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableUdpPorts(int numRequested) {
		return findAvailableUdpPorts(numRequested, PORT_RANGE_MIN, PORT_RANGE_MAX);
	}

	/**
	 * 查找指定数量的可用 UDP 端口，每个端口均从范围 [{@code minPort}, {@code maxPort}] 中随机选择。
	 * @param numRequested 需要查找的可用端口数量
	 * @param minPort 最小端口号
	 * @param maxPort 最大端口号
	 * @return 一个已排序的可用 UDP 端口号集合
	 * @throws IllegalStateException 如果找不到足够数量的可用端口
	 */
	public static SortedSet<Integer> findAvailableUdpPorts(int numRequested, int minPort, int maxPort) {
		return SocketType.UDP.findAvailablePorts(numRequested, minPort, maxPort);
	}


	private enum SocketType {

		TCP {
			@Override
			protected boolean isPortAvailable(int port) {
				try {
					ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
							port, 1, InetAddress.getByName("localhost"));
					serverSocket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		},

		UDP {
			@Override
			protected boolean isPortAvailable(int port) {
				try {
					DatagramSocket socket = new DatagramSocket(port, InetAddress.getByName("localhost"));
					socket.close();
					return true;
				}
				catch (Exception ex) {
					return false;
				}
			}
		};

		/**
		 * 判断此 {@code SocketType} 指定的端口在 {@code localhost} 上当前是否可用。
		 */
		protected abstract boolean isPortAvailable(int port);

		/**
		 * 在范围 [{@code minPort}, {@code maxPort}] 内查找一个伪随机端口号。
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * @return 指定范围内的随机端口号
		 */
		private int findRandomPort(int minPort, int maxPort) {
			int portRange = maxPort - minPort;
			return minPort + random.nextInt(portRange + 1);
		}

		/**
		 * 为此 {@code SocketType} 查找一个可用端口，随机选择自范围 [{@code minPort}, {@code maxPort}]。
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * @return 此套接字类型的一个可用端口号
		 * @throws IllegalStateException 如果找不到可用端口则抛出异常
		 */
		int findAvailablePort(int minPort, int maxPort) {
			Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
			Assert.isTrue(maxPort >= minPort, "'maxPort' must be greater than or equal to 'minPort'");
			Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);

			int portRange = maxPort - minPort;
			int candidatePort;
			int searchCounter = 0;
			do {
				if (searchCounter > portRange) {
					throw new IllegalStateException(String.format(
							"Could not find an available %s port in the range [%d, %d] after %d attempts",
							name(), minPort, maxPort, searchCounter));
				}
				candidatePort = findRandomPort(minPort, maxPort);
				searchCounter++;
			}
			while (!isPortAvailable(candidatePort));

			return candidatePort;
		}

		/**
		 * 为此 {@code SocketType} 查找指定数量的可用端口，
		 * 每个端口随机选择自范围 [{@code minPort}, {@code maxPort}]。
		 * @param numRequested 请求的可用端口数量
		 * @param minPort 最小端口号
		 * @param maxPort 最大端口号
		 * @return 此套接字类型的已排序可用端口号集合
		 * @throws IllegalStateException 如果无法找到请求数量的可用端口则抛出异常
		 */
		SortedSet<Integer> findAvailablePorts(int numRequested, int minPort, int maxPort) {
			Assert.isTrue(minPort > 0, "'minPort' must be greater than 0");
			Assert.isTrue(maxPort > minPort, "'maxPort' must be greater than 'minPort'");
			Assert.isTrue(maxPort <= PORT_RANGE_MAX, "'maxPort' must be less than or equal to " + PORT_RANGE_MAX);
			Assert.isTrue(numRequested > 0, "'numRequested' must be greater than 0");
			Assert.isTrue((maxPort - minPort) >= numRequested,
					"'numRequested' must not be greater than 'maxPort' - 'minPort'");

			SortedSet<Integer> availablePorts = new TreeSet<>();
			int attemptCount = 0;
			while ((++attemptCount <= numRequested + 100) && availablePorts.size() < numRequested) {
				availablePorts.add(findAvailablePort(minPort, maxPort));
			}

			if (availablePorts.size() != numRequested) {
				throw new IllegalStateException(String.format(
						"Could not find %d available %s ports in the range [%d, %d]",
						numRequested, name(), minPort, maxPort));
			}

			return availablePorts;
		}
	}

}
