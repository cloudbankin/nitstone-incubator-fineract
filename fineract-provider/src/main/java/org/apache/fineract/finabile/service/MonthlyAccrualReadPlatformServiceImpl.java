package org.apache.fineract.finabile.service;

import java.math.BigDecimal;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.fineract.finabile.data.AccuralValueDetailData;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.useradministration.domain.AppUser;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFFont;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.joda.time.LocalDate;
import org.joda.time.Months;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class MonthlyAccrualReadPlatformServiceImpl implements MonthlyAccrualReadPlatformService {

	private final PlatformSecurityContext context;
	private final JdbcTemplate jdbcTemplate;

	@Autowired
	public MonthlyAccrualReadPlatformServiceImpl(final PlatformSecurityContext context,
			final RoutingDataSource dataSource) {
		this.context = context;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}

	@Override
	public Workbook retrieveMonthlyAccrualDetails(String startDate, String endDate) {

		SimpleDateFormat sdf3 = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zzz ", Locale.ENGLISH);
		Date fromDate = null;
		Date toDate = null;
		try {
			Date tempDate = sdf3.parse(startDate);
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			String date1 = format1.format(tempDate);
			fromDate = format1.parse(date1);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		try {
			Date tempDate = sdf3.parse(endDate);
			SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM-dd");
			String date1 = format1.format(tempDate);
			toDate = format1.parse(date1);
		} catch (ParseException e) {
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
		
	
		style1.setDataFormat(creationHelper.createDataFormat().getFormat("ddMMyyyy"));

		Sheet AccrualDetails = workbook.createSheet("Accrual Details");
		
		//style start
		
		 Font my_font=workbook.createFont();	
		 my_font.setBoldweight(Font.BOLDWEIGHT_BOLD);
		 style.setFont(my_font);
		//style end

		Row headerAccuralDetails = AccrualDetails.createRow(1);
		headerAccuralDetails.setRowStyle(style);

		Row headerAccuralDetailsTop = AccrualDetails.createRow(0);
		headerAccuralDetailsTop.setRowStyle(style);

		headerAccuralDetails.createCell(0).setCellValue("Loan Account");
		headerAccuralDetails.createCell(1).setCellValue("Client Name");
		headerAccuralDetailsTop.createCell(2).setCellValue("Interest Accural");
		headerAccuralDetailsTop.getCell(2).setCellStyle(style);

		// finding differences for month details
		List<String> monthCount = differenceFromToDate(startDate, endDate);
		int j = 2;
		for (int i = 0; i < monthCount.size(); i++) {
			headerAccuralDetails.createCell(j).setCellValue(monthCount.get(i));
			j++;
		}
		headerAccuralDetails.createCell(2 + monthCount.size()).setCellValue("Total Interest Accural");
		
		
		// Create a cellRangeAddress to select a range to merge.
		CellRangeAddress cellRangeAddress = new CellRangeAddress(0,0,2,2 + monthCount.size());

		// Merge the selected cells.
		AccrualDetails.addMergedRegion(cellRangeAddress);
		
		headerAccuralDetailsTop.createCell(3 + monthCount.size()).setCellValue("Penality Accural");
		headerAccuralDetailsTop.getCell(3 + monthCount.size()).setCellStyle(style);

		int k = 3 + monthCount.size();
		for (int i = 0; i < monthCount.size(); i++) {
			headerAccuralDetails.createCell(k).setCellValue(monthCount.get(i));
			k++;
		}
		headerAccuralDetails.createCell(k).setCellValue("Total Penality Accural");
		// Create a cellRangeAddress to select a range to merge.
		CellRangeAddress cellRangeAddressPenality = new CellRangeAddress(0,0,3 + monthCount.size(),k);

		// Merge the selected cells.
		AccrualDetails.addMergedRegion(cellRangeAddressPenality);

		headerAccuralDetails.createCell(k+1).setCellValue("Total Accural");
		
		
		for(int styleIterate = 0;styleIterate<=k+1;styleIterate++) {
			headerAccuralDetails.getCell(styleIterate).setCellStyle(style);
			
		}
		// headers are designed

		// Values retrieved from database

		final AccrualValueDetailsMapper rm = new AccrualValueDetailsMapper();
		StringBuilder sql = new StringBuilder(200);
		sql.append(rm.schema());

		LocalDate jdbcStartDate = new LocalDate(fromDate);
		LocalDate jdbcEndDate = new LocalDate(toDate);

		sql.append(" and lt.transaction_date between '" + jdbcStartDate + "' and '" + jdbcEndDate
				+ "' group by l.account_no");

		final List<AccuralValueDetailData> accrualValueDetails = this.jdbcTemplate.query(sql.toString(), rm,
				new Object[] {});

		int rowCount = 1;
		List<String> loanAccountValidation = new ArrayList<String>();
		int totalRowCountDetails = 0;
		for (int i = 0; i < accrualValueDetails.size(); i++) {
			AccuralValueDetailData accrualData = accrualValueDetails.get(i);
			Row accrualRow = AccrualDetails.createRow(rowCount + 1);

			accrualRow.setRowStyle(style);
			accrualRow.createCell(0).setCellValue(accrualData.getLoanAccount());
			accrualRow.createCell(1).setCellValue(accrualData.getClientName());
			
			
			

			// interest accrued details
			AccrualValueForDuplicateDetailsMapper rm1 = new AccrualValueForDuplicateDetailsMapper();

			String sqlForDuplicate = "select c.display_name as client_Name, l.account_no as Loan_Account,lt.transaction_date as Date, "
					+ "ifnull(lt.interest_portion_derived,0) as interest_Accrued  from m_loan_transaction lt "
					+ " join m_loan l on l.id=lt.loan_id " + " join m_client c on c.id=l.client_id "
					+ " where lt.transaction_type_enum=19 and lt.is_reversed=0 and (lt.interest_portion_derived != 0) and l.account_no='"
					+ accrualData.getLoanAccount() + "'" + "  and lt.transaction_date between '" + jdbcStartDate
					+ "' and '" + jdbcEndDate + "'  order by lt.transaction_date ";
			final List<AccuralValueDetailData> accrualValueDuplicate = this.jdbcTemplate.query(sqlForDuplicate, rm1,
					new Object[] {});
			int rowCountForinterestAccrued = 2;
			Double TotalInterestAccrued = 0.00d;
			// for(AccuralValueDetailData sample : accrualValueDuplicate) {

			DateFormat formaterYd = new SimpleDateFormat("MMM-yy");
			
			
			
			//Getting Month details from interest transaction date
			List<String> penMonthInterestValidation = new ArrayList<String>();
			
			for(AccuralValueDetailData data : accrualValueDuplicate) {
				String datevalid = formaterYd.format(data.getTransactionDate());
				penMonthInterestValidation.add(datevalid);			}
			
			for(int a=0;a<monthCount.size();a++) {
				String monthValidForCompare = monthCount.get(a);
				if(penMonthInterestValidation.contains(monthValidForCompare.toLowerCase().substring(0, 1).toUpperCase()+monthValidForCompare.substring(1).toLowerCase())) {
					
				}else {
					penMonthInterestValidation.add(a, "0");
				}
			}
			
			
			String currentDateFormat = "";
			int count = 0;
			
			int p=0;
			for (int c=0;c<penMonthInterestValidation.size();c++) {
			//	currentDateFormat = formaterYd.format(accrualValueDuplicate.get(p).getTransactionDate());
	

				if (monthCount.get(count).equalsIgnoreCase(penMonthInterestValidation.get(c))) {
					accrualRow.createCell(rowCountForinterestAccrued)
							.setCellValue(accrualValueDuplicate.get(p).getInterestAccrued().doubleValue());
					rowCountForinterestAccrued++;
					TotalInterestAccrued += (accrualValueDuplicate.get(p).getInterestAccrued().doubleValue());
					p++;
				}else {
					accrualRow.createCell(rowCountForinterestAccrued).setCellValue(0d);
					rowCountForinterestAccrued++;
				}
				count++;
			}
			for (; count < monthCount.size(); count++) {
				accrualRow.createCell(rowCountForinterestAccrued).setCellValue(0d);
				rowCountForinterestAccrued++;

			}

			accrualRow.createCell(rowCountForinterestAccrued).setCellValue(TotalInterestAccrued.doubleValue());

			// penality accrued details
			AccrualValueForDuplicateDetailsPenalityMapper rm2 = new AccrualValueForDuplicateDetailsPenalityMapper();

			String sqlForDuplicatePenality = "select c.display_name as client_Name, l.account_no as Loan_Account,lt.transaction_date as Date, "
					+ "ifnull(lt.penalty_charges_portion_derived,0) as penality_Accrued  from m_loan_transaction lt "
					+ " join m_loan l on l.id=lt.loan_id " + " join m_client c on c.id=l.client_id "
					+ " where lt.transaction_type_enum=19 and lt.is_reversed=0 and (lt.penalty_charges_portion_derived != 0) and l.account_no='"
					+ accrualData.getLoanAccount() + "'" + "  and lt.transaction_date between '" + jdbcStartDate
					+ "' and '" + jdbcEndDate + "'  order by lt.transaction_date ";
			final List<AccuralValueDetailData> accrualValueDuplicatePenality = this.jdbcTemplate
					.query(sqlForDuplicatePenality, rm2, new Object[] {});
			int rowCountForPenalityAccrued = rowCountForinterestAccrued+1;
			Double TotalPenalityAccrued = 0.00d;
			// for(AccuralValueDetailData sample1 : accrualValueDuplicatePenality) {

			formaterYd = new SimpleDateFormat("MMM-yy");
			int countPenality = 0;

			String currentDateFormatPenality = "";
			
			//Getting Month details from penality transaction date
			List<String> penMonthValidation = new ArrayList<String>();
			
			for(AccuralValueDetailData data : accrualValueDuplicatePenality) {
				String datevalid = formaterYd.format(data.getTransactionDate());
				penMonthValidation.add(datevalid);			}
			
			for(int a=0;a<monthCount.size();a++) {
				String monthValidForCompare = monthCount.get(a);
				if(penMonthValidation.contains(monthValidForCompare.toLowerCase().substring(0, 1).toUpperCase()+monthValidForCompare.substring(1).toLowerCase())) {
					
				}else {
					penMonthValidation.add(a, "0");
				}
			}
			int q=0;
			for (int b=0;b<penMonthValidation.size();b++) {
				
			//	currentDateFormatPenality = formaterYd.format(accrualValueDuplicatePenality.get(q).getTransactionDate());

				if (monthCount.get(countPenality).equalsIgnoreCase(penMonthValidation.get(countPenality))) {
					accrualRow.createCell(rowCountForPenalityAccrued)
							.setCellValue(accrualValueDuplicatePenality.get(q).getPenalityAccrued().doubleValue());
					rowCountForPenalityAccrued++;
					TotalPenalityAccrued += (accrualValueDuplicatePenality.get(q).getPenalityAccrued().doubleValue());
					q++;
				}else {
					accrualRow.createCell(rowCountForPenalityAccrued).setCellValue(0d);
					rowCountForPenalityAccrued++;
				}
				countPenality++;
			}

			for (; countPenality < monthCount.size(); countPenality++) {
				accrualRow.createCell(rowCountForPenalityAccrued).setCellValue(0d);
				rowCountForPenalityAccrued++;

			}
			accrualRow.createCell(rowCountForPenalityAccrued).setCellValue(TotalPenalityAccrued.doubleValue());
			accrualRow.createCell(rowCountForPenalityAccrued+1).setCellValue(TotalInterestAccrued.doubleValue()+TotalPenalityAccrued.doubleValue());
			
			totalRowCountDetails = rowCountForPenalityAccrued+1;
			
			TotalInterestAccrued = 0.00d;
			TotalPenalityAccrued = 0.00d;
			rowCountForinterestAccrued = 2;
			rowCountForPenalityAccrued = 0;
			rowCount++;
		}
		for(int autoFocus=0;autoFocus<=totalRowCountDetails;autoFocus++) {
			AccrualDetails.autoSizeColumn(autoFocus);
		}

		return workbook;
	}

	private List<String> differenceFromToDate(String fromDate, String toDate) {

		/*
		 * LocalDate startDate = new LocalDate(fromDate); LocalDate endDate = new
		 * LocalDate(toDate);
		 * 
		 * int monthsBetWeen = Months.monthsBetween(startDate, endDate).getMonths();
		 * 
		 * return monthsBetWeen;
		 */

		DateFormat formater = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss zzz ", Locale.ENGLISH);

		Calendar beginCalendar = Calendar.getInstance();
		Calendar finishCalendar = Calendar.getInstance();

		try {
			beginCalendar.setTime(formater.parse(fromDate));
			finishCalendar.setTime(formater.parse(toDate));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		List<String> listOfMonths = new ArrayList<String>();

		DateFormat formaterYd = new SimpleDateFormat("MMM-yy");

		while (beginCalendar.before(finishCalendar)) {

			String date = formaterYd.format(beginCalendar.getTime()).toUpperCase();
			//System.out.println(date);
			listOfMonths.add(date);
			// Add One Month to get next Month
			beginCalendar.add(Calendar.MONTH, 1);

		}
		return listOfMonths;
	}

	private static final class AccrualValueDetailsMapper implements RowMapper<AccuralValueDetailData> {

		public String schema() {
			return "select c.display_name as client_Name, l.account_no as Loan_Account,lt.transaction_date as Date,"
					+ "ifnull(lt.interest_portion_derived,0) as interest_Accrued, "
					+ "ifnull(lt.penalty_charges_portion_derived,0) as penality_Accrued"
					+ " from m_loan_transaction lt " + " join m_loan l on l.id=lt.loan_id "
					+ " join m_client c on c.id=l.client_id "
					+ " where lt.transaction_type_enum=19 and lt.is_reversed=0 and (lt.interest_portion_derived != 0 or lt.penalty_charges_portion_derived !=0)";
		}

		@Override
		public AccuralValueDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final String clientName = rs.getString("client_Name");
			final String loanAccount = rs.getString("Loan_Account");
			final Date transactionDate = rs.getDate("Date");
			final BigDecimal interestAccrued = rs.getBigDecimal("interest_Accrued");
			final BigDecimal penalityAccrued = rs.getBigDecimal("penality_Accrued");

			return new AccuralValueDetailData(clientName, loanAccount, transactionDate, interestAccrued,
					penalityAccrued);

		}
	}

	private static final class AccrualValueForDuplicateDetailsMapper implements RowMapper<AccuralValueDetailData> {

		@Override
		public AccuralValueDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final String clientName = rs.getString("client_Name");
			final String loanAccount = rs.getString("Loan_Account");
			final Date transactionDate = rs.getDate("Date");
			final BigDecimal interestAccrued = rs.getBigDecimal("interest_Accrued");

			return new AccuralValueDetailData(clientName, loanAccount, transactionDate, interestAccrued,
					BigDecimal.ZERO);

		}
	}

	private static final class AccrualValueForDuplicateDetailsPenalityMapper
			implements RowMapper<AccuralValueDetailData> {

		@Override
		public AccuralValueDetailData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum)
				throws SQLException {

			final String clientName = rs.getString("client_Name");
			final String loanAccount = rs.getString("Loan_Account");
			final Date transactionDate = rs.getDate("Date");
			// final BigDecimal interestAccrued = rs.getBigDecimal("interest_Accrued");
			final BigDecimal penalityAccrued = rs.getBigDecimal("penality_Accrued");

			return new AccuralValueDetailData(clientName, loanAccount, transactionDate, BigDecimal.ZERO,
					penalityAccrued);

		}
	}

}
