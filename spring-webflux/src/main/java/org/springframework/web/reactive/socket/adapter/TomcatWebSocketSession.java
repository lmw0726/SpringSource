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

package org.springframework.web.reactive.socket.adapter;

import org.apache.tomcat.websocket.WsSession;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.web.reactive.socket.HandshakeInfo;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Sinks;

import javax.websocket.Session;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Spring {@link WebSocketSession} adapter for Tomcat's
 * {@link javax.websocket.Session}.
 *
 * @author Violeta Georgieva
 * @since 5.0
 */
public class TomcatWebSocketSession extends StandardWebSocketSession {

	/**
	 * TomcatWebSocketSession是一个WebSocket会话的实现类。
	 */
	private static final AtomicIntegerFieldUpdater<TomcatWebSocketSession> SUSPENDED =
			AtomicIntegerFieldUpdater.newUpdater(TomcatWebSocketSession.class, "suspended");

	/**
	 * 表示会话是否处于挂起状态的标志。
	 */
	@SuppressWarnings("unused")
	private volatile int suspended;

	/**
	 * 使用给定的Session、HandshakeInfo和DataBufferFactory创建一个TomcatWebSocketSession对象。
	 *
	 * @param session WebSocket会话对象。
	 * @param info    握手信息对象。
	 * @param factory DataBufferFactory对象。
	 */
	public TomcatWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory) {
		super(session, info, factory);
	}

	/**
	 * 使用给定的Session、HandshakeInfo、DataBufferFactory和completionSink创建一个TomcatWebSocketSession对象。
	 * 创建对象后，会将接收挂起。
	 *
	 * @param session        WebSocket会话对象。
	 * @param info           握手信息对象。
	 * @param factory        DataBufferFactory对象。
	 * @param completionSink Sinks.Empty对象。
	 */
	public TomcatWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
								  Sinks.Empty<Void> completionSink) {
		super(session, info, factory, completionSink);
		suspendReceiving();
	}

	/**
	 * 使用给定的Session、HandshakeInfo、DataBufferFactory和completionMono创建一个TomcatWebSocketSession对象。
	 * 创建对象后，会将接收挂起。
	 *
	 * @param session        WebSocket会话对象。
	 * @param info           握手信息对象。
	 * @param factory        DataBufferFactory对象。
	 * @param completionMono MonoProcessor对象。
	 */
	@Deprecated
	public TomcatWebSocketSession(Session session, HandshakeInfo info, DataBufferFactory factory,
								  reactor.core.publisher.MonoProcessor<Void> completionMono) {
		super(session, info, factory, completionMono);
		suspendReceiving();
	}

	/**
	 * 判断是否可以挂起接收。
	 *
	 * @return 如果可以挂起接收，则返回true。
	 */
	@Override
	protected boolean canSuspendReceiving() {
		return true;
	}

	/**
	 * 挂起接收。
	 * 如果挂起成功，会调用WebSocket会话代理对象的suspend()方法。
	 */
	@Override
	protected void suspendReceiving() {
		if (SUSPENDED.compareAndSet(this, 0, 1)) {
			((WsSession) getDelegate()).suspend();
		}
	}

	/**
	 * 恢复接收WebSocket会话。
	 */
	@Override
	protected void resumeReceiving() {
		if (SUSPENDED.compareAndSet(this, 1, 0)) {
			// 调用getDelegate()方法获取WebSocket会话代理对象，并调用resume()方法恢复接收
			((WsSession) getDelegate()).resume();
		}
	}

}
