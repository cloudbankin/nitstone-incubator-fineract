package org.mifosplatform.infrastructure.sms.service;

import org.mifosplatform.infrastructure.sms.vo.SMSDataVO;

public interface SmsProcessingService {

    boolean sendSMS(SMSDataVO data);

}