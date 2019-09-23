package org.apache.fineract.finabile.service;

import org.apache.poi.ss.usermodel.Workbook;

public interface MonthlyAccrualReadPlatformService {

	Workbook retrieveMonthlyAccrualDetails(String startDate, String endDate);

}
	