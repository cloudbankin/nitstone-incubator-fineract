/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.mifosplatform.infrastructure.sms.vo;

import java.io.Serializable;


import com.google.gson.Gson;

public class EduwizeSMSResponseVO<T> implements Serializable {

	
	
	private static final long serialVersionUID = -1531755169451207344L;
	private final T content;
	private final int response_code;
	private final String response_msg;
	private final long timestamp;

	public EduwizeSMSResponseVO(final T content,final int response_code, final String response_msg, final long timestamp) {
		this.content = content;
		this.response_code = response_code;
		this.response_msg = response_msg;
		this.timestamp = timestamp;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public EduwizeSMSResponseVO(final String jsonString) {
		Gson g = new Gson(); 
		EduwizeSMSResponseVO temp = g.fromJson(jsonString, EduwizeSMSResponseVO.class);
		this.content = (T) temp.getContent();
		this.response_code = temp.getResponse_code();
		this.response_msg = temp.getResponse_msg();
		this.timestamp = temp.getTimestamp();
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}

	public T getContent() {
		return content;
	}

	public int getResponse_code() {
		return response_code;
	}

	public String getResponse_msg() {
		return response_msg;
	}

	public long getTimestamp() {
		return timestamp;
	}
	
	
}
