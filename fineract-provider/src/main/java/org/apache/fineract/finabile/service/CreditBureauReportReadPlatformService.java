package org.apache.fineract.finabile.service;

import org.apache.poi.ss.usermodel.Workbook;

public interface CreditBureauReportReadPlatformService {
	
	Workbook retrieveCreditBureauReportData(String startDate, String endDate);

}
