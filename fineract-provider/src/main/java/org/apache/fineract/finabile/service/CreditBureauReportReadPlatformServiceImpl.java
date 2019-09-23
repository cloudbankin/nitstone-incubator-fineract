package org.apache.fineract.finabile.service;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.poi.ss.usermodel.Cell;

import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;


import org.apache.fineract.finabile.data.CreditBureauReportDetailData;


@Service
public class CreditBureauReportReadPlatformServiceImpl implements CreditBureauReportReadPlatformService{
	
	    private final PlatformSecurityContext context;
		private final JdbcTemplate jdbcTemplate;

		@Autowired
		public CreditBureauReportReadPlatformServiceImpl(final PlatformSecurityContext context,
				final RoutingDataSource dataSource) {
			this.context = context;
			this.jdbcTemplate = new JdbcTemplate(dataSource);
		}

		@Override
		public Workbook retrieveCreditBureauReportData(String asOnDate, String closedFrom) {
			SimpleDateFormat sdf3 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zzz ", Locale.ENGLISH);
			Date asDate = null;
			Date fromDate = null;
			try {
				Date tempDate = sdf3.parse(asOnDate);
				SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
				String date1 = format1.format(tempDate);
				asDate = format1.parse(date1);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				Date tempDate = sdf3.parse(closedFrom);
				SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
				String date1 = format1.format(tempDate);
				fromDate = format1.parse(date1);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			final AppUser currentUser = this.context.authenticatedUser();
			final String hierarchy = currentUser.getOffice().getHierarchy();
			// Excel Sheet creation
			Workbook workbook = new HSSFWorkbook();
			CreationHelper creationHelper = workbook.getCreationHelper();
			CellStyle style = workbook.createCellStyle(); // Create new style
			style.setWrapText(true); // Set wordwrap
			CellStyle style1 = workbook.createCellStyle();
			
			
			final CreditBureauReportDetailsMapper rm = new CreditBureauReportDetailsMapper();
			
			final String query=rm.schema();
			final List<CreditBureauReportDetailData> creditBureauDetails = this.jdbcTemplate.query(rm.schema(),rm,new Object[] {asDate,asDate,fromDate,asDate,asDate,asDate,fromDate,asDate});
			

			
		    style1.setDataFormat(creationHelper.createDataFormat().getFormat("ddMMyyyy"));
		    
			Sheet CreditBureauDetailsheet = workbook.createSheet("Credit Bureau Details");
			//style start
			
			 Font my_font=workbook.createFont();	
			 my_font.setBoldweight(Font.BOLDWEIGHT_BOLD);
			 my_font.setFontHeight((short) 25);
			 style.setFont(my_font);
			//style end
			 
			Row headerLoan = CreditBureauDetailsheet.createRow(0);
			headerLoan.setRowStyle(style);
			headerLoan.setHeightInPoints(25);
			headerLoan.createCell(0).setCellValue("Consumer Name");
			headerLoan.createCell(1).setCellValue("Date of Birth");
			headerLoan.createCell(2).setCellValue("Gender");
			headerLoan.createCell(3).setCellValue("Income Tax ID Number");
			headerLoan.createCell(4).setCellValue("Passport Number");
			headerLoan.createCell(5).setCellValue("Passport Issue Date");
			headerLoan.createCell(6).setCellValue("Passport Expiry Date");
			headerLoan.createCell(7).setCellValue("Voter ID Number");
			headerLoan.createCell(8).setCellValue("Driving License Number");
			headerLoan.createCell(9).setCellValue("Driving License Issue Date");
			headerLoan.createCell(10).setCellValue("Driving License Expiry Date");
			headerLoan.createCell(11).setCellValue("Ration Card Number");
			headerLoan.createCell(12).setCellValue("Universal ID Number");
			headerLoan.createCell(13).setCellValue("Additional ID #1");
			headerLoan.createCell(14).setCellValue("Additional ID #2");
			headerLoan.createCell(15).setCellValue("Telephone No.Mobile");
			headerLoan.createCell(16).setCellValue("Telephone No.Residence");
			headerLoan.createCell(17).setCellValue("Telephone No.Office");
			headerLoan.createCell(18).setCellValue("Extension Office");
			headerLoan.createCell(19).setCellValue("Telephone No.Other ");
			headerLoan.createCell(20).setCellValue("Extension Other");
			headerLoan.createCell(21).setCellValue("Email ID 1");
			headerLoan.createCell(22).setCellValue("Email ID 2");
			headerLoan.createCell(23).setCellValue("Address 1");
			headerLoan.createCell(24).setCellValue("State Code 1");
			headerLoan.createCell(25).setCellValue("PIN Code 1");
			headerLoan.createCell(26).setCellValue("Address Category 1");
			headerLoan.createCell(27).setCellValue("Residence Code 1");
			headerLoan.createCell(28).setCellValue("Address 2");
			headerLoan.createCell(29).setCellValue("State Code 2");
			headerLoan.createCell(30).setCellValue("PIN Code 2");
			headerLoan.createCell(31).setCellValue("Address Category 2");
			headerLoan.createCell(32).setCellValue("Residence Code 2");
			headerLoan.createCell(33).setCellValue("Current/New Member Code");
			headerLoan.createCell(34).setCellValue("Current/New Member Short Name");
			headerLoan.createCell(35).setCellValue("Curr/New Account No");
			headerLoan.createCell(36).setCellValue("Account Type");
			headerLoan.createCell(37).setCellValue("Ownership Indicator");
			headerLoan.createCell(38).setCellValue("Date Opened/Disbursed");
			headerLoan.createCell(39).setCellValue("Date of Last Payment");
			headerLoan.createCell(40).setCellValue("Date Closed");
			headerLoan.createCell(41).setCellValue("Date Reported");
			headerLoan.createCell(42).setCellValue("High Credit/Sanctioned Amt");
			headerLoan.createCell(43).setCellValue("Current  Balance");
			headerLoan.createCell(44).setCellValue("Amt Overdue");
			headerLoan.createCell(45).setCellValue("No of Days Past Due");
			headerLoan.createCell(46).setCellValue("Old Mbr Code");
			headerLoan.createCell(47).setCellValue("Old Mbr Short Name");
			headerLoan.createCell(48).setCellValue("Old Acc No");
			headerLoan.createCell(49).setCellValue("Old Acc Type");
			headerLoan.createCell(50).setCellValue("Old Ownership Indicator");
			headerLoan.createCell(51).setCellValue("Suit Filed / Wilful Default");
			headerLoan.createCell(52).setCellValue("Written-off and Settled Status");
			headerLoan.createCell(53).setCellValue("Asset Classification");
			headerLoan.createCell(54).setCellValue("Value of Collateral");
			headerLoan.createCell(55).setCellValue("Type of Collateral");
			headerLoan.createCell(56).setCellValue("Credit Limit");
			headerLoan.createCell(57).setCellValue("Cash Limit");
			headerLoan.createCell(58).setCellValue("Rate of Interest");
			headerLoan.createCell(59).setCellValue("RepaymentTenure");
			headerLoan.createCell(60).setCellValue("EMI Amount");
			headerLoan.createCell(61).setCellValue("Written- off Amount (Total) ");
			headerLoan.createCell(62).setCellValue("Written- off Principal Amount");
			headerLoan.createCell(63).setCellValue("Settlement Amt");
			headerLoan.createCell(64).setCellValue("Payment Frequency");
			headerLoan.createCell(65).setCellValue("Actual Payment Amt");
			headerLoan.createCell(66).setCellValue("Occupation Code");
			headerLoan.createCell(67).setCellValue("Income");
			headerLoan.createCell(68).setCellValue("Net/Gross Income Indicator");
			headerLoan.createCell(69).setCellValue("Monthly/Annual Income Indicator");
			int NoOfRows=creditBureauDetails.size();
			for (int k = 0; k < creditBureauDetails.size(); k++) {
				CreditBureauReportDetailData tempLoan = creditBureauDetails.get(k);
				Row loanRow =CreditBureauDetailsheet.createRow(k + 1);
				try {
					loanRow.setHeightInPoints(20);
					loanRow.setRowStyle(style1);
					loanRow.createCell(0).setCellValue(tempLoan.getConsumerName());
					loanRow.createCell(1).setCellValue(tempLoan.getDateOfBirth());
					loanRow.createCell(2).setCellValue(tempLoan.getGender());
					loanRow.createCell(3).setCellValue(tempLoan.getIncomeTaxIdNumber());
					loanRow.createCell(4).setCellValue(tempLoan.getPassportNumber());
					loanRow.createCell(5).setCellValue(tempLoan.getPassportIssueDate());
					loanRow.createCell(6).setCellValue(tempLoan.getPassportExpiryDate());
					loanRow.createCell(7).setCellValue(tempLoan.getVoterIdNumber());
					loanRow.createCell(8).setCellValue(tempLoan.getDrivingLicenseNumber());
					loanRow.createCell(9).setCellValue(tempLoan.getDrivingLicenseIssueDate());
					loanRow.createCell(10).setCellValue(tempLoan.getDrivingLicenseExpiryDate());
					loanRow.createCell(11).setCellValue(tempLoan.getRationCardNumber());
					loanRow.createCell(12).setCellValue(tempLoan.getUniversalIdNumber());
					loanRow.createCell(13).setCellValue(tempLoan.getAdditionalId1());
					loanRow.createCell(14).setCellValue(tempLoan.getAdditionalId2());
					loanRow.createCell(15).setCellValue(tempLoan.getTelephoneNoMobile());
					loanRow.createCell(16).setCellValue(tempLoan.getTelephoneNoResidence());
					loanRow.createCell(17).setCellValue(tempLoan.getTelephoneNoOffice());
					loanRow.createCell(18).setCellValue(tempLoan.getExtensionOffice());
					loanRow.createCell(19).setCellValue(tempLoan.getTelephoneNoOther());
					loanRow.createCell(20).setCellValue(tempLoan.getExtensionOther());
					loanRow.createCell(21).setCellValue(tempLoan.getEmailId1());
					loanRow.createCell(22).setCellValue(tempLoan.getEmailId2());
					if(tempLoan.getAddress1().equals(""))
					{
						if(tempLoan.getAddress2().equals(""))
						{
							if(tempLoan.getAddress3().equals(""))
							{
								loanRow.createCell(23).setCellValue("");
							}
							else
							{
								loanRow.createCell(23).setCellValue(tempLoan.getAddress3());
								if(tempLoan.getStateCode3().equals("73"))
							    {
							    loanRow.createCell(24).setCellValue("27");
							    }
							    else if(tempLoan.getStateCode3().equals("70"))
							    {
							    	loanRow.createCell(24).setCellValue("29");
							    }
							    else if(tempLoan.getStateCode3().equals("71"))
							    {
							    	loanRow.createCell(24).setCellValue(32);
							    }
							    else if(tempLoan.getStateCode3().equals("58"))
							    {
							    	loanRow.createCell(24).setCellValue(33);
							    }
							    else if(tempLoan.getStateCode3().equals("59"))
							    {
							    	loanRow.createCell(24).setCellValue(28);
							    }
							    else
							    {
							    	loanRow.createCell(24).setCellValue("");
							    }								
								loanRow.createCell(25).setCellValue(tempLoan.getPINCode3());
								loanRow.createCell(26).setCellValue("03");
								loanRow.createCell(27).setCellValue(tempLoan.getResidenceCode3());
							}
						}
						else
						{
							loanRow.createCell(23).setCellValue(tempLoan.getAddress2());
							if(tempLoan.getStateCode2().equals("73"))
						    {
						    loanRow.createCell(24).setCellValue("27");
						    }
						    else if(tempLoan.getStateCode2().equals("70"))
						    {
						    	loanRow.createCell(24).setCellValue("29");
						    }
						    else if(tempLoan.getStateCode2().equals("71"))
						    {
						    	loanRow.createCell(24).setCellValue(32);
						    }
						    else
						    {
						    	loanRow.createCell(24).setCellValue("");
						    }							
							loanRow.createCell(25).setCellValue(tempLoan.getPINCode2());
							loanRow.createCell(26).setCellValue("02");
							loanRow.createCell(27).setCellValue(tempLoan.getResidenceCode2());
						}
					}	
					else
					{
					    loanRow.createCell(23).setCellValue(tempLoan.getAddress1());
					    if(tempLoan.getStateCode1().equals("73"))
					    {
					    loanRow.createCell(24).setCellValue("27");
					    }
					    else if(tempLoan.getStateCode1().equals("70"))
					    {
					    	loanRow.createCell(24).setCellValue("29");
					    }
					    else if(tempLoan.getStateCode1().equals("71"))
					    {
					    	loanRow.createCell(24).setCellValue(32);
					    }
					    else
					    {
					    	loanRow.createCell(24).setCellValue("");
					    }
						loanRow.createCell(25).setCellValue(tempLoan.getPINCode1());
						loanRow.createCell(26).setCellValue("01");
						loanRow.createCell(27).setCellValue(tempLoan.getResidenceCode1());
					}
					
					
					/*loanRow.createCell(28).setCellValue(tempLoan.getAddress2());
					loanRow.createCell(29).setCellValue(tempLoan.getStateCode2());
					loanRow.createCell(30).setCellValue(tempLoan.getPINCode2());
					loanRow.createCell(31).setCellValue(tempLoan.getAddressCategory2());
					loanRow.createCell(32).setCellValue(tempLoan.getResidenceCode2());
					*/
					loanRow.createCell(28).setCellValue("");
					loanRow.createCell(29).setCellValue("");
					loanRow.createCell(30).setCellValue("");
					loanRow.createCell(31).setCellValue("");
					loanRow.createCell(32).setCellValue("");
					loanRow.createCell(33).setCellValue(tempLoan.getCurrentNewMemberCode());
					loanRow.createCell(34).setCellValue(tempLoan.getCurrentNewMemberShortName());
					if(tempLoan.getCurrentNewAccountNo()>0 && tempLoan.getCurrentNewAccountNo()<=9)
					{	
						loanRow.createCell(35).setCellValue("00000000"+tempLoan.getCurrentNewAccountNo());
					}
					else
					{
						loanRow.createCell(35).setCellValue("0000000"+tempLoan.getCurrentNewAccountNo());

					}
					
					loanRow.createCell(36).setCellValue(tempLoan.getAccountType());
					loanRow.createCell(37).setCellValue("1");
					loanRow.createCell(38).setCellValue(tempLoan.getDateOpenedorDisbursed());
					loanRow.createCell(39).setCellValue(tempLoan.getDateOfLastPayment());
					loanRow.createCell(40).setCellValue(tempLoan.getDateClosed());
					loanRow.createCell(41).setCellValue(tempLoan.getDateReported());
					loanRow.createCell(42).setCellValue(tempLoan.getHighCreditorSanctionedAmt().floatValue());
					loanRow.createCell(43).setCellValue(tempLoan.getCurrentBalance().floatValue());
					if(tempLoan.getAmtOverdue().floatValue()==0)
					{
						loanRow.createCell(44).setCellValue("");

					}
					else 
					{
					loanRow.createCell(44).setCellValue(tempLoan.getAmtOverdue().floatValue());
					}
					if(tempLoan.getNoOfDaysPastDue()==0)
					{
						loanRow.createCell(45).setCellValue("");
					}
					else
					{
					loanRow.createCell(45).setCellValue(tempLoan.getNoOfDaysPastDue());
					}
					loanRow.createCell(46).setCellValue(tempLoan.getOldMemberCode());
					loanRow.createCell(47).setCellValue(tempLoan.getOldMemberShortName());
					if(tempLoan.getOldAccountNo()==0)
					{
						loanRow.createCell(48).setCellValue("");
					}
					else
					{
					loanRow.createCell(48).setCellValue(tempLoan.getOldAccountNo());
					}
					loanRow.createCell(49).setCellValue(tempLoan.getOldAccountType());
					loanRow.createCell(50).setCellValue(tempLoan.getOldOwnershipIndicator());
					loanRow.createCell(51).setCellValue(tempLoan.getSuitFiledorWilfulDefault());
					loanRow.createCell(52).setCellValue(tempLoan.getWrittenOffAndSettledStatus());
					loanRow.createCell(53).setCellValue(tempLoan.getAssetClassification());
					if(tempLoan.getValueOfCollateral().floatValue()==0)
					{
						loanRow.createCell(54).setCellValue("");

					}
					else
					{
					loanRow.createCell(54).setCellValue(tempLoan.getValueOfCollateral().floatValue());
					}
					loanRow.createCell(55).setCellValue(tempLoan.getTypeOfCollateral());
					if(tempLoan.getCreditLimit().floatValue()==0)
					{
						loanRow.createCell(56).setCellValue("");

					}
					else
					{
					loanRow.createCell(56).setCellValue(tempLoan.getCreditLimit().floatValue());
					}
					if(tempLoan.getCashLimit().floatValue()==0)
					{
						loanRow.createCell(57).setCellValue("");

					}
					else
					{
					loanRow.createCell(57).setCellValue(tempLoan.getCashLimit().floatValue());
					}
					loanRow.createCell(58).setCellValue(tempLoan.getRateOfInterest());
					loanRow.createCell(59).setCellValue(tempLoan.getRepaymentTenure());
					loanRow.createCell(60).setCellValue(tempLoan.getEMIAmount().floatValue());
					if(tempLoan.getWrittenOffAmountTotal().floatValue()==0)
					{
						loanRow.createCell(61).setCellValue("");

					}
					else {
					loanRow.createCell(61).setCellValue(tempLoan.getWrittenOffAmountTotal().floatValue());
					}
					if(tempLoan.getWrittenOffPrincipalAmount().floatValue()==0)
					{
						loanRow.createCell(62).setCellValue("");

					}
					else
					{
						
					loanRow.createCell(62).setCellValue(tempLoan.getWrittenOffPrincipalAmount().floatValue());
				    }
					if(tempLoan.getSettlementAmount().floatValue()==0)
					{
						loanRow.createCell(63).setCellValue("");

					}
					else
					{
						loanRow.createCell(63).setCellValue(tempLoan.getSettlementAmount().floatValue());

					}
					loanRow.createCell(64).setCellValue(tempLoan.getPaymentFrequency());
					if(tempLoan.getActualPaymentAmt().floatValue()==0)
					{
						loanRow.createCell(65).setCellValue("");

					}
					else
					{
					loanRow.createCell(65).setCellValue(tempLoan.getActualPaymentAmt().floatValue());
					}
					if(tempLoan.getOccupationCode().equals("143"))
					{
					loanRow.createCell(66).setCellValue("01");
					}
					else if(tempLoan.getOccupationCode().equals("145"))
					{
					loanRow.createCell(66).setCellValue("02");
					}
					else
					{
						loanRow.createCell(66).setCellValue(tempLoan.getOccupationCode());
					}
					if(tempLoan.getIncome().floatValue()==0)
					{
						loanRow.createCell(67).setCellValue("");

					}
					else
					{
					loanRow.createCell(67).setCellValue(tempLoan.getIncome().floatValue());
					}
					loanRow.createCell(68).setCellValue(tempLoan.getNetorGrossIncomeIndicator());
					loanRow.createCell(69).setCellValue(tempLoan.getMonthlyAnnualIncomeIndicator());
			 
					
					if(tempLoan.getHasGaurantor()==3)
					{
						
						 loanRow =CreditBureauDetailsheet.createRow(NoOfRows + 1);
						 try {
								loanRow.setRowStyle(style1);
								loanRow.setHeightInPoints(20);
								loanRow.createCell(0).setCellValue(tempLoan.getGaurantorName());
								loanRow.createCell(1).setCellValue(tempLoan.getGaurantorDOB());
								loanRow.createCell(2).setCellValue("");
								loanRow.createCell(3).setCellValue("");
								loanRow.createCell(4).setCellValue("");
								loanRow.createCell(5).setCellValue("");
								loanRow.createCell(6).setCellValue("");
								loanRow.createCell(7).setCellValue("");
								loanRow.createCell(8).setCellValue("");
								loanRow.createCell(9).setCellValue("");
								loanRow.createCell(10).setCellValue("");
								loanRow.createCell(11).setCellValue("");
								loanRow.createCell(12).setCellValue("");
								loanRow.createCell(13).setCellValue("");
								loanRow.createCell(14).setCellValue("");
								loanRow.createCell(15).setCellValue(tempLoan.getGaurantorMobileNo());
								loanRow.createCell(16).setCellValue("");
								loanRow.createCell(17).setCellValue("");
								loanRow.createCell(18).setCellValue("");
								loanRow.createCell(19).setCellValue("");
								loanRow.createCell(20).setCellValue("");
								loanRow.createCell(21).setCellValue("");
								loanRow.createCell(22).setCellValue("");
								loanRow.createCell(23).setCellValue(tempLoan.getGaurantorAddress());
								if(tempLoan.getGaurantorstate().equals("73"))
							    {
							    loanRow.createCell(24).setCellValue("27");
							    }
							    else if(tempLoan.getGaurantorstate().equals("70"))
							    {
							    	loanRow.createCell(24).setCellValue("29");
							    }
							    else if(tempLoan.getGaurantorstate().equals("71"))
							    {
							    	loanRow.createCell(24).setCellValue(32);
							    }
							    else
							    {
							    	loanRow.createCell(24).setCellValue("04");
							    }								
								loanRow.createCell(25).setCellValue(tempLoan.getGaurantorPINCode());
								loanRow.createCell(26).setCellValue("");
								loanRow.createCell(27).setCellValue("");
								loanRow.createCell(28).setCellValue("");
								loanRow.createCell(29).setCellValue("");
								loanRow.createCell(30).setCellValue("");
								loanRow.createCell(31).setCellValue("");
								loanRow.createCell(32).setCellValue("");
								loanRow.createCell(33).setCellValue(tempLoan.getCurrentNewMemberCode());
								loanRow.createCell(34).setCellValue(tempLoan.getCurrentNewMemberShortName());
								if(tempLoan.getCurrentNewAccountNo()>0 && tempLoan.getCurrentNewAccountNo()<=9)
								{	
									loanRow.createCell(35).setCellValue("00000000"+tempLoan.getCurrentNewAccountNo());
								}
								else
								{
									loanRow.createCell(35).setCellValue("0000000"+tempLoan.getCurrentNewAccountNo());

								}								loanRow.createCell(36).setCellValue(tempLoan.getAccountType());
								loanRow.createCell(37).setCellValue("3");
								loanRow.createCell(38).setCellValue(tempLoan.getDateOpenedorDisbursed());
								loanRow.createCell(39).setCellValue(tempLoan.getDateOfLastPayment());
								loanRow.createCell(40).setCellValue(tempLoan.getDateClosed());
								loanRow.createCell(41).setCellValue(tempLoan.getDateReported());
								loanRow.createCell(42).setCellValue(tempLoan.getHighCreditorSanctionedAmt().floatValue());
								loanRow.createCell(43).setCellValue(tempLoan.getCurrentBalance().floatValue());
								if(tempLoan.getAmtOverdue().floatValue()==0)
								{
									loanRow.createCell(44).setCellValue("");

								}
								else 
								{
								loanRow.createCell(44).setCellValue(tempLoan.getAmtOverdue().floatValue());
								}
								if(tempLoan.getNoOfDaysPastDue()==0)
								{
									loanRow.createCell(45).setCellValue("");
								}
								else
								{
								loanRow.createCell(45).setCellValue(tempLoan.getNoOfDaysPastDue());
								}
								loanRow.createCell(46).setCellValue(tempLoan.getOldMemberCode());
								loanRow.createCell(47).setCellValue(tempLoan.getOldMemberShortName());
								if(tempLoan.getOldAccountNo()==0)
								{
									loanRow.createCell(48).setCellValue("");
								}
								else
								{
								loanRow.createCell(48).setCellValue(tempLoan.getOldAccountNo());
								}								
								loanRow.createCell(49).setCellValue(tempLoan.getOldAccountType());
								loanRow.createCell(50).setCellValue(tempLoan.getOldOwnershipIndicator());
								loanRow.createCell(51).setCellValue(tempLoan.getSuitFiledorWilfulDefault());
								loanRow.createCell(52).setCellValue(tempLoan.getWrittenOffAndSettledStatus());
								loanRow.createCell(53).setCellValue(tempLoan.getAssetClassification());
								if(tempLoan.getValueOfCollateral().floatValue()==0)
								{
									loanRow.createCell(54).setCellValue("");

								}
								else
								{
								loanRow.createCell(54).setCellValue(tempLoan.getValueOfCollateral().floatValue());
								}								
								loanRow.createCell(55).setCellValue(tempLoan.getTypeOfCollateral());
								if(tempLoan.getCreditLimit().floatValue()==0)
								{
									loanRow.createCell(56).setCellValue("");

								}
								else
								{
								loanRow.createCell(56).setCellValue(tempLoan.getCreditLimit().floatValue());
								}
								if(tempLoan.getCashLimit().floatValue()==0)
								{
									loanRow.createCell(57).setCellValue("");

								}
								else
								{
								loanRow.createCell(57).setCellValue(tempLoan.getCashLimit().floatValue());
								}
								loanRow.createCell(58).setCellValue(tempLoan.getRateOfInterest());
								loanRow.createCell(59).setCellValue(tempLoan.getRepaymentTenure());
								loanRow.createCell(60).setCellValue(tempLoan.getEMIAmount().floatValue());
								if(tempLoan.getWrittenOffAmountTotal().floatValue()==0)
								{
									loanRow.createCell(61).setCellValue("");

								}
								else {
								loanRow.createCell(61).setCellValue(tempLoan.getWrittenOffAmountTotal().floatValue());
								}
								if(tempLoan.getWrittenOffPrincipalAmount().floatValue()==0)
								{
									loanRow.createCell(62).setCellValue("");

								}
								else
								{
									
								loanRow.createCell(62).setCellValue(tempLoan.getWrittenOffPrincipalAmount().floatValue());
							    }
								if(tempLoan.getSettlementAmount().floatValue()==0)
								{
									loanRow.createCell(63).setCellValue("");

								}
								else
								{
									loanRow.createCell(63).setCellValue(tempLoan.getSettlementAmount().floatValue());

								}
								loanRow.createCell(64).setCellValue(tempLoan.getPaymentFrequency());
								if(tempLoan.getActualPaymentAmt().floatValue()==0)
								{
									loanRow.createCell(65).setCellValue("");

								}
								else
								{
								loanRow.createCell(65).setCellValue(tempLoan.getActualPaymentAmt().floatValue());
								}
								if(tempLoan.getOccupationCode().equals("143"))
								{
								loanRow.createCell(66).setCellValue("01");
								}
								else if(tempLoan.getOccupationCode().equals("145"))
								{
								loanRow.createCell(66).setCellValue("02");
								}
								else
								{
									loanRow.createCell(66).setCellValue(tempLoan.getOccupationCode());
								}
								if(tempLoan.getIncome().floatValue()==0)
								{
									loanRow.createCell(67).setCellValue("");

								}
								else
								{
								loanRow.createCell(67).setCellValue(tempLoan.getIncome().floatValue());
								}
								loanRow.createCell(68).setCellValue(tempLoan.getNetorGrossIncomeIndicator());
								loanRow.createCell(69).setCellValue(tempLoan.getMonthlyAnnualIncomeIndicator());
								
								NoOfRows=NoOfRows+1;
						 }
						 catch (Exception e) {
								loanRow.createCell(6).setCellValue("Error Occured" + e.getMessage());
							}
					}
				}
				catch (Exception e) {
					loanRow.createCell(6).setCellValue("Error Occured" + e.getMessage());
				}
			
			}
			//set cell size
			 CreditBureauDetailsheet.autoSizeColumn(0);
			 CreditBureauDetailsheet.autoSizeColumn(1);
			 CreditBureauDetailsheet.autoSizeColumn(2);
			 CreditBureauDetailsheet.autoSizeColumn(3);
			 CreditBureauDetailsheet.autoSizeColumn(4);
			 CreditBureauDetailsheet.autoSizeColumn(5);
			 CreditBureauDetailsheet.autoSizeColumn(6);
			 CreditBureauDetailsheet.autoSizeColumn(7);
			 CreditBureauDetailsheet.autoSizeColumn(8);
			 CreditBureauDetailsheet.autoSizeColumn(9);
			 CreditBureauDetailsheet.autoSizeColumn(10);
			 CreditBureauDetailsheet.autoSizeColumn(11);
			 CreditBureauDetailsheet.autoSizeColumn(12);
			 CreditBureauDetailsheet.autoSizeColumn(13);
			 CreditBureauDetailsheet.autoSizeColumn(14);
			 CreditBureauDetailsheet.autoSizeColumn(15);
			 CreditBureauDetailsheet.autoSizeColumn(16);
			 CreditBureauDetailsheet.autoSizeColumn(17);
			 CreditBureauDetailsheet.autoSizeColumn(18);
			 CreditBureauDetailsheet.autoSizeColumn(19);
			 CreditBureauDetailsheet.autoSizeColumn(20);
			 CreditBureauDetailsheet.autoSizeColumn(21);
			 CreditBureauDetailsheet.autoSizeColumn(22);
			 CreditBureauDetailsheet.autoSizeColumn(23);
			 CreditBureauDetailsheet.autoSizeColumn(24);
			 CreditBureauDetailsheet.autoSizeColumn(25);
			 CreditBureauDetailsheet.autoSizeColumn(26);
			 CreditBureauDetailsheet.autoSizeColumn(27);
			 CreditBureauDetailsheet.autoSizeColumn(28);
			 CreditBureauDetailsheet.autoSizeColumn(29);
			 CreditBureauDetailsheet.autoSizeColumn(30);
			 CreditBureauDetailsheet.autoSizeColumn(31);
			 CreditBureauDetailsheet.autoSizeColumn(32);
			 CreditBureauDetailsheet.autoSizeColumn(33);
			 CreditBureauDetailsheet.autoSizeColumn(34);
			 CreditBureauDetailsheet.autoSizeColumn(35);
			 CreditBureauDetailsheet.autoSizeColumn(36);
			 CreditBureauDetailsheet.autoSizeColumn(37);
			 CreditBureauDetailsheet.autoSizeColumn(38);
			 CreditBureauDetailsheet.autoSizeColumn(39);
			 CreditBureauDetailsheet.autoSizeColumn(40);
			 CreditBureauDetailsheet.autoSizeColumn(41);
			 CreditBureauDetailsheet.autoSizeColumn(42);
			 CreditBureauDetailsheet.autoSizeColumn(43);
			 CreditBureauDetailsheet.autoSizeColumn(44);
			 CreditBureauDetailsheet.autoSizeColumn(45);
			 CreditBureauDetailsheet.autoSizeColumn(46);
			 CreditBureauDetailsheet.autoSizeColumn(47);
			 CreditBureauDetailsheet.autoSizeColumn(48);
			 CreditBureauDetailsheet.autoSizeColumn(49);
			 CreditBureauDetailsheet.autoSizeColumn(50);
			 CreditBureauDetailsheet.autoSizeColumn(51);
			 CreditBureauDetailsheet.autoSizeColumn(52);
			 CreditBureauDetailsheet.autoSizeColumn(53);
			 CreditBureauDetailsheet.autoSizeColumn(54);
			 CreditBureauDetailsheet.autoSizeColumn(55);
			 CreditBureauDetailsheet.autoSizeColumn(56);
			 CreditBureauDetailsheet.autoSizeColumn(57);
			 CreditBureauDetailsheet.autoSizeColumn(58);
			 CreditBureauDetailsheet.autoSizeColumn(59);
			 CreditBureauDetailsheet.autoSizeColumn(60);
			 CreditBureauDetailsheet.autoSizeColumn(61);
			 CreditBureauDetailsheet.autoSizeColumn(62);
			 CreditBureauDetailsheet.autoSizeColumn(63);
			 CreditBureauDetailsheet.autoSizeColumn(64);
			 CreditBureauDetailsheet.autoSizeColumn(65);
			 CreditBureauDetailsheet.autoSizeColumn(66);
			 CreditBureauDetailsheet.autoSizeColumn(67);
			 CreditBureauDetailsheet.autoSizeColumn(68);
			 CreditBureauDetailsheet.autoSizeColumn(69);
			

			return workbook;
		}
		
		

	
		private static final class CreditBureauReportDetailsMapper implements RowMapper<CreditBureauReportDetailData> {

			public String schema() {
				return "select l.id as loanid,c.display_name as Consumer_Name,\n" + 
						"date_format(c.date_of_birth,'%d%m%Y') as Date_Of_Birth,\n" + 
						"IF(c.gender_cv_id=42, 2, 1) as Gender,\n" + 
						"if(ci1.document_key is null,\"\",ci1.document_key) as IncomeTaxIDNumber_PAN,\n" + 
						"if(ci4.document_key is null,\"\",ci4.document_key) as PassportNumber,\n" + 
						"'' as PassportIssueDate,\n" + 
						"'' as PassportExpiryDate,\n" + 
						"'' as VoterIdNumber,\n" + 
						"if(ci3.document_key is null,\"\",ci3.document_key) as DrivingLicense,\n" + 
						"'' as DrivingLicenseIssueDate,\n" + 
						"'' as DrivingLicenseExpiryDate,\n" + 
						"'' as RationcardID,\n" + 
						"if(ci2.document_key is null,\"\",ci2.document_key) as AadharIDorUniversalIDNumber,\n" + 
						"if(ci5.document_key is null,\"\",ci5.document_key) as AdditionalID1,\n" + 
						"if(ci6.document_key  is null ,\"\",ci6.document_key) as Additionalid2,\n" + 
						"if(c.mobile_no is null,\"\",c.mobile_no) as TelephoneNoMobile,\n" + 
						"'' as TelephoneNoResidence,\n" + 
						"'' as TelephoneNoOffice,\n" + 
						"'' as ExtensionOffice,\n" + 
						"'' as TelephoneNoOther,\n" + 
						"'' as ExtensionOther,\n" + 
						"if(c.Email is not null,c.email,\" \")as Email_ID1,\n" + 
						"'' as Email_ID2,\n" + 
						"\n" + 
						"if(CONCAT(a1.street,\" \",a1.address_line_1,\" \",a1.address_line_2,\"\n" + 
						"\",a1.address_line_3,\" \",a1.town_village,\" \",a1.city,\"\n" + 
						"\",a1.county_district)is null,\"\",CONCAT(a1.street,\"\n" + 
						"\",a1.address_line_1,\" \",a1.address_line_2,\"\n" + 
						"\",a1.address_line_3,\" \",a1.town_village,\" \",a1.city,\"\n" + 
						"\",a1.county_district)) as Address1 ,\n" + 
						"if(a1.state_province_id=cv.id,cv.id,\"\")as StateCode1,\n" + 
						"if(a1.postal_code is null,\"\",a1.postal_code) as PINCode1,\n" + 
						"if(ca1.address_type_id  is not null,\"01\",\"\") as AddressCategory1permanent,\n" + 
						"'' as ResidenceCode1,\n" + 
						"\n" + 
						"if(CONCAT(a2.street,\" \",a2.address_line_1,\" \",a2.address_line_2,\"\n" + 
						"\",a2.address_line_3,\" \",a2.town_village,\" \",a2.city,\"\n" + 
						"\",a2.county_district) is null,\"\",CONCAT(a2.street,\"\n" + 
						"\",a2.address_line_1,\" \",a2.address_line_2,\"\n" + 
						"\",a2.address_line_3,\" \",a2.town_village,\" \",a2.city,\"\n" + 
						"\",a2.county_district)) as Address2,\n" + 
						"if(a2.state_province_id=cv1.id,cv1.id,\"\") as StateCode2,\n" + 
						"if(a2.postal_code is null,\"\",a2.postal_code) as PINCode2,\n" + 
						"if(ca2.address_type_id is not null,\"02\",\"\") as Addresscategory2residence,\n" + 
						"'' as ResidenceCode2,\n" + 
						"\n" + 
						"if(CONCAT(a3.street,\" \",a3.address_line_1,\" \",a3.address_line_2,\"\n" + 
						"\",a3.address_line_3,\" \",a3.town_village,\" \",a3.city,\"\n" + 
						"\",a3.county_district) is null ,\"\",CONCAT(a3.street,\"\n" + 
						"\",a3.address_line_1,\" \",a3.address_line_2,\"\n" + 
						"\",a3.address_line_3,\" \",a3.town_village,\" \",a3.city,\"\n" + 
						"\",a3.county_district)) as\n" + 
						"Address3,\n" + 
						"if(a3.state_province_id=cv2.id,cv2.id,\"\") as StateCode3,\n" + 
						"if(a3.postal_code is null,\"\",a3.postal_code) as PINCode3,\n" + 
						"if(ca3.address_type_id is not null,\"03\",\"\") as Addresscategory3officeorbusiness,\n" + 
						"'' as ResidenceCode3,\n" + 
						"\n" + 
						"\n" + 
						"'' as Current_NewMemberCode,\n" + 
						"'' as Current_NewMemberShortName,\n" + 
						"c.account_no as Curr_AccountNo,\n" + 
						"if(l.product_id=1 or l.product_id=6,\"05\",\"\") as AccounType ,\n" + 
						"'01' as OwnershipIndicator,\n" + 
						"if(date_format(l.disbursedon_date,'%d%m%Y') is\n" + 
						"null,\"\",date_format(l.disbursedon_date,'%d%m%Y')) as\n" + 
						"DateOpenedorDisbursed,\n" + 
						"if(t.transaction_date is null,\"\",t.transaction_date) as DateOfLastPayment,\n" + 
						"if(date_format(l.closedon_date,'%d%m%Y') is null,\"\",date_format(l.closedon_date,'%d%m%Y'))\n" + 
						"as DateClosed ,\n" + 
						"if(date_format(CURDATE(),'%d%m%Y') is null,\"\",date_format(CURDATE(),'%d%m%Y')) as\n" + 
						"DateReported,\n" + 
						"\n" + 
						"if(l.approved_principal is null,\"\",l.approved_principal) as HighSanctionedAmount\n" + 
						",if(tx.outstanding_loan_balance_derived=0,0,tx.outstanding_loan_balance_derived)\n" + 
						"as CurrentBalance,\n" + 
						"if((laa.principal_overdue_derived+laa.interest_overdue_derived+\n" + 
						"laa.fee_charges_overdue_derived+laa.penalty_charges_overdue_derived) is null,\"\",(laa.principal_overdue_derived+laa.interest_overdue_derived+\n" + 
						"laa.fee_charges_overdue_derived+laa.penalty_charges_overdue_derived))\n" + 
						"as AmtOverdue,\n" + 
						"if(datediff(curdate(),laa.overdue_since_date_derived) is\n" + 
						"null,\"\",datediff(curdate(),laa.overdue_since_date_derived)) as\n" + 
						"NoOFDaysPastDue ,\n" + 
						"'' as oldMbrCode,\n" + 
						"'' as oldMbrShortName,\n" + 
						"'' as oldAccNo,\n" + 
						"'' as oldAccType,\n" + 
						"'' as oldOwnershipIndicator,\n" + 
						"'' as SuitFiled_wilfulDefault,\n" + 
						"'' as Written_OffandSettledStatus,\n" + 
						"if((laa.overdue_since_date_derived=null),\"\",\"01\") as Asset_Classification,\n" + 
						"if(lc.value is null,\"\",lc.value) as\n" + 
						"Value_Of_Collateral,if(lc.value=null,\"00\",\"\") as\n" + 
						"Type_Of_Collateral,\n" + 
						"'' as CreditLimit,\n" + 
						"'' as CashLimit,\n" + 
						"if(l.nominal_interest_rate_per_period is\n" + 
						"null,\"\",l.nominal_interest_rate_per_period)as RateOfInterest ,\n" + 
						"if(l.number_of_repayments is null,\"\",l.number_of_repayments) as RepaymentTenure,\n" + 
						"if(emi.EMIamount is null,\"\",emi.EMIamount) as EMIAmount,\n" + 
						"if(l.writtenoffon_date  is null ,\"\",l.principal_writtenoff_derived)as\n" + 
						"WrittenOffPrincipleAmount,\n" + 
						"if((l.principal_writtenoff_derived+l.interest_writtenoff_derived+\n" + 
						"l.fee_charges_writtenoff_derived+l.penalty_charges_writtenoff_derived)\n" + 
						"=0,\"\",(l.principal_writtenoff_derived+l.interest_writtenoff_derived+\n" + 
						"l.fee_charges_writtenoff_derived+l.penalty_charges_writtenoff_derived))\n" + 
						"as WrittenOffTotalAmount,\n" + 
						"'' as SettlementAmount,\n" + 
						"if(l.repayment_period_frequency_enum is null,\"\",\"03\") as\n" + 
						"PaymentFrequency ,\n" + 
						"'' as ActualPaymentAmt,\n" + 
						" if(od.occupation_cd_professional is\n" + 
						"null,\"\",od.occupation_cd_professional) as OccupationCode,\n" + 
						" '' as Income,\n" + 
						" '' as NetorGrossIncomeIndicator,\n" + 
						" '' as MonthlyAnnualIncomeIndicator,\n" + 
						" if(l.id=g.loan_id,3,1) as has_gaurantor,\n" + 
						" if(concat(g.firstname,\" \",g.lastname) is null,\"\",concat(g.firstname,\" \",g.lastname)) as guarantorName,\n" + 
						" if(g.dob is null,\"\",g.dob) as gaurantorDateOfBirth,\n" + 
						" if(g.mobile_number is null,\"\",g.mobile_number) as gaurantorMobileNumber,\n" + 
						" if(g.state is null,\"\",g.state) as gaurantorState,\n" + 
						" if( concat(g.address_line_1,\"\",g.address_line_2,g.city) is null,\"\",concat(g.address_line_1,\"\",g.address_line_2,g.city)) as gaurantorAddress,\n" + 
						" if(g.zip is null,\"\",g.zip)  as gaurantorPINCode\n" + 
						"\n" + 
						"from m_client c  join  m_loan l on c.id=l.client_id and l.loan_type_enum=1 and l.loan_status_id in (300,600,601,700)\n" + 
						"\n" + 
						"inner join (select max(id) trxid,tx.loan_id trxlid from m_loan_transaction tx where  tx.transaction_type_enum in (1,2,6,8,19) and tx.is_reversed=0 group by tx.loan_id) trx on trx.trxlid=l.id\n"+
						"inner join m_loan_transaction tx on tx.id=trx.trxid\n"+
						"left join m_client_address ca1 on c.id=ca1.client_id and ca1.address_type_id=17\n" + 
						"left join m_client_address ca2 on c.id=ca2.client_id and ca2.address_type_id=14\n" + 
						"left join m_client_address ca3 on c.id=ca3.client_id and ca3.address_type_id=15\n" + 
						"left join m_address a1 on a1.id=ca1.address_id\n" + 
						"left join m_address a2 on a2.id=ca2.address_id\n" + 
						"left join m_address a3 on a3.id=ca3.address_id\n" + 
						"left join m_code_value cv on  cv.id = a1.state_province_id\n" + 
						"left join m_code_value cv1 on  cv1.id = a2.state_province_id\n" + 
						"left join m_code_value cv2 on  cv2.id = a3.state_province_id\n" + 
						"left join m_loan_disbursement_detail ldd on ldd.loan_id = l.id\n" + 
						"left join `occupation details` od on c.id=od.client_id\n" + 
						"left join m_code_value cv3 on cv3.id=od.occupation_cd_professional\n" + 
						"left join m_client_identifier ci1 on ci1.client_id=c.id and\n" + 
						"ci1.document_type_id=41\n" + 
						"left join m_client_identifier ci2 on ci2.client_id=c.id and\n" + 
						"ci2.document_type_id=40\n" + 
						"left join m_client_identifier ci4 on ci4.client_id=c.id and\n" + 
						"ci4.document_type_id=1\n" + 
						"left join m_client_identifier ci5 on ci5.client_id=c.id and\n" + 
						"ci5.document_type_id=2\n" + 
						"left join m_client_identifier ci3 on ci3.client_id=c.id and\n" + 
						"ci3.document_type_id=3\n" + 
						"left join m_client_identifier ci6 on ci6.client_id=c.id and\n" + 
						"ci6.document_type_id=4\n" + 
						"\n" + 
						"\n" + 
						"left  join m_loan_arrears_aging laa on l.id=laa.loan_id\n" + 
						"left join\n" + 
						"(select transactdate.loan_id as\n" + 
						"loanid,date_format(transactdate.transaction_date,'%d%m%Y')\n" + 
						"transaction_date  from (SELECT *\n" + 
						" FROM (SELECT * FROM m_loan_transaction lt where\n" + 
						"lt.transaction_type_enum=2 and lt.is_reversed =0\n" + 
						" ORDER BY id DESC) as x GROUP BY loan_id) as transactdate) t on l.id=t.loanid\n" + 
						" left join\n" + 
						" (select lrs.loan_id,(lrs.principal_amount+lrs.interest_amount) as EMIamount\n" + 
						"  from m_loan_repayment_schedule lrs where lrs.installment=1 order by\n" + 
						"loan_id asc) emi on emi.loan_id=l.id\n" + 
						" left join m_loan_collateral lc on lc.loan_id=l.id\n" + 
						" left join m_guarantor g on g.loan_id=l.id\n"+ 
						"where \n" + 
						
						"					 ifnull(date(ldd.disbursedon_date),date(l.disbursedon_date)) <= ? AND ( ( ifnull(l.closedon_date, ?) \n" + 
						"					 between ? and ? ) or ( if( l.closedon_date > ?, ?, l.closedon_date ) between ? and ? ) )\n" + 
						"  \n" + 
						" group by l.id";
			}
            @Override
			public CreditBureauReportDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
					throws SQLException {
                
            	final long loanId=rs.getLong("loanid");
				final String consumerName = rs.getString("Consumer_Name");
				final String dateOfBirth = rs.getString("Date_Of_Birth");
				final int gender = rs.getInt("Gender");
				final String incomeTaxIdNumber = rs.getString("IncomeTaxIDNumber_PAN");
				final String passportNumber = rs.getString("PassportNumber");
				final String passportIssueDate = rs.getString("PassportIssueDate");
				final String passportExpiryDate = rs.getString("PassportExpiryDate");
				final String voterIdNumber = rs.getString("VoterIdNumber");
				final String drivingLicenseNumber = rs.getString("DrivingLicense");
				final String drivingLicenseIssueDate = rs.getString("DrivingLicenseIssueDate");
				final String drivingLicenseExpiryDate = rs.getString("DrivingLicenseExpiryDate");
				final String rationCardNumber = rs.getString("RationcardID");
				final String universalIdNumber = rs.getString("AadharIDorUniversalIDNumber");
				final String additionalId1 = rs.getString("AdditionalID1");
				final String additionalId2 = rs.getString("AdditionalID2");
				final String telephoneNoMobile = rs.getString("TelephoneNoMobile");
				final String telephoneNoResidence = rs.getString("TelephoneNoResidence");
				final String telephoneNoOffice = rs.getString("TelephoneNoOffice");
				final String extensionOffice = rs.getString("ExtensionOffice");
				final String telephoneNoOther = rs.getString("TelephoneNoOther");
				final String extensionOther = rs.getString("ExtensionOther");
				final String emailId1 = rs.getString("Email_ID1");
				final String emailId2 = rs.getString("Email_ID2");
				final String address1 = rs.getString("Address1");
                final String stateCode1 = rs.getString("StateCode1");
                final String PINCode1 = rs.getString("PINCode1");
                final String addressCategory1 = rs.getString("AddressCategory1permanent");
                final String residenceCode1 = rs.getString("ResidenceCode1");
                final String address2 = rs.getString("Address2");
                final String stateCode2 = rs.getString("StateCode2");
                final String PINCode2 = rs.getString("PINCode2");
                final String addressCategory2 = rs.getString("Addresscategory2residence");
                final String residenceCode2 = rs.getString("ResidenceCode2");
                final String address3 = rs.getString("Address3");
                final String stateCode3 = rs.getString("StateCode3");
                final String PINCode3 = rs.getString("PINCode3");
                final String addressCategory3 = rs.getString("Addresscategory3officeorbusiness");
                final String residenceCode3 = rs.getString("ResidenceCode3");
                final String currentNewMemberCode = rs.getString("Current_NewMemberCode");
                final String currentNewMemberShortName = rs.getString("Current_NewMemberShortName");
                final long currentNewAccountNo = rs.getLong("Curr_AccountNo");
                final String accountType = rs.getString("AccounType");
                final String ownershipIndicator = rs.getString("OwnershipIndicator");
                final String dateOpenedorDisbursed = rs.getString("DateOpenedorDisbursed");
                final String dateOfLastPayment = rs.getString("DateOfLastPayment");
                final String dateClosed = rs.getString("DateClosed");
                final String dateReported = rs.getString("DateReported");
    			final BigDecimal highCreditorSanctionedAmt = rs.getBigDecimal("HighSanctionedAmount");
    			final BigDecimal currentBalance = rs.getBigDecimal("CurrentBalance");
    			final BigDecimal amtOverdue = rs.getBigDecimal("AmtOverdue");
    			final int noOfDaysPastDue = rs.getInt("NoOFDaysPastDue");
    			final String oldMemberCode = rs.getString("oldMbrCode");
                final String oldMemberShortName = rs.getString("oldMbrShortName");
                final long oldAccountNo = rs.getLong("oldAccNo");
                final String oldAccountType = rs.getString("oldAccType");
                final String oldOwnershipIndicator = rs.getString("oldOwnershipIndicator");
                final String suitFiledorWilfulDefault = rs.getString("SuitFiled_wilfulDefault");
                final String writtenOffAndSettledStatus = rs.getString("Written_OffandSettledStatus");
                final String assetClassification = rs.getString("Asset_Classification");
                final BigDecimal valueOfCollateral = rs.getBigDecimal("Value_Of_Collateral");
                final String typeOfCollateral = rs.getString("Type_Of_Collateral");
                final BigDecimal creditLimit = rs.getBigDecimal("CreditLimit");
                final BigDecimal cashLimit = rs.getBigDecimal("CashLimit");
                final float rateOfInterest=rs.getFloat("RateOfInterest");
                final int repaymentTenure = rs.getInt("RepaymentTenure");
                final BigDecimal EMIAmount = rs.getBigDecimal("EMIAmount");
                final BigDecimal writtenOffPrincipalAmount = rs.getBigDecimal("WrittenOffPrincipleAmount");
                final BigDecimal writtenOffAmountTotal = rs.getBigDecimal("WrittenOffTotalAmount");
                final BigDecimal settlementAmount = rs.getBigDecimal("SettlementAmount");
                final String paymentFrequency = rs.getString("PaymentFrequency");
                final BigDecimal actualPaymentAmt = rs.getBigDecimal("ActualPaymentAmt");
                final String occupationCode = rs.getString("OccupationCode");
                final BigDecimal income = rs.getBigDecimal("Income");
                final String netorGrossIncomeIndicator = rs.getString("NetorGrossIncomeIndicator");
                final String monthlyAnnualIncomeIndicator = rs.getString("MonthlyAnnualIncomeIndicator");
                final int hasGaurantor =rs.getInt("has_gaurantor");
                final String gaurantorName=rs.getString("guarantorName");
                final  String gaurantorDOB=rs.getString("gaurantorDateOfBirth");
            	final  String gaurantorMobileNo=rs.getString("gaurantorMobileNumber");
            	final  String gaurantorstate=rs.getString("gaurantorState");
            	final  String gaurantorAddress=rs.getString("gaurantorAddress");
            	final  String gaurantorPINCode=rs.getString("gaurantorPINCode");
                
                return new CreditBureauReportDetailData( loanId,consumerName, dateOfBirth, gender, incomeTaxIdNumber, passportNumber, passportIssueDate,
                		   passportExpiryDate, voterIdNumber, drivingLicenseNumber, drivingLicenseIssueDate, drivingLicenseExpiryDate, rationCardNumber,
                		   universalIdNumber, additionalId1, additionalId2, telephoneNoMobile,telephoneNoResidence,  telephoneNoOffice, extensionOffice, telephoneNoOther, extensionOther,
                		   emailId1,  emailId2, address1, stateCode1,  PINCode1, addressCategory1, residenceCode1,
                		   address2, stateCode2, PINCode2, addressCategory2, residenceCode2, address3,
                		   stateCode3, PINCode3, addressCategory3, residenceCode3, currentNewMemberCode, currentNewMemberShortName,
                		   currentNewAccountNo, accountType, ownershipIndicator, dateOpenedorDisbursed,
                		   dateOfLastPayment, dateClosed,  dateReported, highCreditorSanctionedAmt,
                		   currentBalance, amtOverdue, noOfDaysPastDue, oldMemberCode, oldMemberShortName, oldAccountNo,
                		   oldAccountType,  oldOwnershipIndicator, suitFiledorWilfulDefault,  writtenOffAndSettledStatus, assetClassification, valueOfCollateral, typeOfCollateral, creditLimit,
                		   cashLimit, rateOfInterest, repaymentTenure, EMIAmount,  writtenOffAmountTotal,   writtenOffPrincipalAmount,   settlementAmount,   paymentFrequency,  actualPaymentAmt,  occupationCode,
                		   income,   netorGrossIncomeIndicator,  monthlyAnnualIncomeIndicator,hasGaurantor,gaurantorName,gaurantorDOB,gaurantorMobileNo,gaurantorstate,gaurantorAddress,gaurantorPINCode);

			}
			

			

			
		}
}

