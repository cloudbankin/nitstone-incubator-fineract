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

public class SMSDataVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -387461629641602672L;
	private final String smsTemplateType;
	private final String phoneNo;
	private final String templateParameterString;

	public SMSDataVO(final String smsTemplateType, final String phoneNo, final String templateParameterString) {
		this.smsTemplateType = smsTemplateType;
		this.phoneNo = phoneNo;
		this.templateParameterString = templateParameterString;
	}

	public String getSmsTemplateType() {
		return smsTemplateType;
	}

	public String getPhoneNo() {
		return phoneNo;
	}

	public String getTemplateParameterString() {
		return templateParameterString;
	}


}
