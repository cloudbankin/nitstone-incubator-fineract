/**
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this file,
 * You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.mifosplatform.infrastructure.sms.service;


import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.mifosplatform.infrastructure.sms.vo.EduwizeSMSResponseVO;
import org.mifosplatform.infrastructure.sms.vo.SMSDataVO;
import org.mifosplatform.infrastructure.utils.CommonMethodsUtil;


@Service
public class SmsProcessingServiceImpl implements SmsProcessingService {

    private final static Logger logger = LoggerFactory.getLogger(SmsProcessingServiceImpl.class);

    
    @Autowired
	private  Environment propertyEnv;
    

    @SuppressWarnings("rawtypes")
	@Override
	public boolean sendSMS(SMSDataVO data) {

		boolean isSuccess = false;
		String templateName = "";

		try {
			
			if (CommonMethodsUtil.isBlank(data.getSmsTemplateType())) {
				logger.error("Error in sending Sms : Sms template type is blank");
				return isSuccess;
			}
			
			boolean isActive = Boolean.valueOf(propertyEnv.getProperty("sms."+data.getSmsTemplateType()+".isActive"));
			if (!isActive) {
				logger.error("Error in sending Sms : Sms trigger is inactive");
				return isSuccess;
			}

				templateName = propertyEnv.getProperty("sms.template."+data.getSmsTemplateType());
			
				String requestUrl = propertyEnv.getProperty("sms.url.send");
				URL url = new URL(requestUrl);
				
				HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

				//add reuqest header
				con.setRequestMethod("POST");
				con.setRequestProperty("User-Agent", "Mozilla/5.0");
				con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

				String urlParameters = "username="
						+ propertyEnv.getProperty("sms.username") + "&password="
						+ propertyEnv.getProperty("sms.password") + "&destination="
						+ data.getPhoneNo() + "&template_name="
						+ templateName + "&"
						+ data.getTemplateParameterString() + "&response_format="
						+ propertyEnv.getProperty("sms.response_format") + "&sender_id="
						+ propertyEnv.getProperty("sms.sender") + "";
				// Send post request
				con.setDoOutput(true);
				DataOutputStream wr = new DataOutputStream(con.getOutputStream());
				wr.writeBytes(urlParameters);
				wr.flush();
				wr.close();
				
				if (con.getResponseCode()==200) {
					BufferedReader in = new BufferedReader(new InputStreamReader(
							con.getInputStream()));
					String inputLine;
					StringBuffer response = new StringBuffer();

					while ((inputLine = in.readLine()) != null) {
						response.append(inputLine);
					}
					in.close();
					
					EduwizeSMSResponseVO responseMessage = new EduwizeSMSResponseVO(response.toString());
							
					if(responseMessage.getResponse_code()==401)
					{
						isSuccess = true;
						logger.info(propertyEnv.getProperty("sms.sent.response") + " - "
								+ response.toString());
					}
					else {
					logger.error(propertyEnv.getProperty("sms.sent.response") + " - "
							+ response.toString());
					}
			}

			con.disconnect();
		} catch (Exception ex) {
			isSuccess = false;
			logger.error(propertyEnv.getProperty("sms.sent.exception") + " - "
							+ ex.getMessage());
			return isSuccess;
		}
		return isSuccess;
	}


}