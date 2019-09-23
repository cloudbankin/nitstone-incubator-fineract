package org.apache.fineract.finabile.data;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.poi.ss.usermodel.Row;

public class CreditBureauReportDetailData  {
	private  long   loanId;
	private  String consumerName;
	private  String dateOfBirth;
	private  int gender;
	private  String incomeTaxIdNumber;
	private  String passportNumber;
	private  String passportIssueDate;
	private  String passportExpiryDate;
	private  String voterIdNumber;
	private  String drivingLicenseNumber;
	private  String drivingLicenseIssueDate;
	private  String drivingLicenseExpiryDate;
	private  String rationCardNumber;
	private  String universalIdNumber;
	private  String additionalId1;
    private  String additionalId2;
	private  String telephoneNoMobile;
	private  String telephoneNoResidence;
	private  String telephoneNoOffice;
	private  String extensionOffice;
	private  String telephoneNoOther;
	private  String extensionOther;
	private  String emailId1;
    private  String emailId2;
	private  String address1;
	private  String stateCode1;
	private  String PINCode1;
	private  String addressCategory1;
	private  String residenceCode1;
	private  String address2;
	private  String stateCode2;
	private  String PINCode2;
	private  String addressCategory2;
	private  String residenceCode2;
	private  String address3;
	private  String stateCode3;
	private  String PINCode3;
	private  String addressCategory3;
	private  String residenceCode3;
	private  String currentNewMemberCode;
	private  String currentNewMemberShortName;
	private  long currentNewAccountNo;
	private  String accountType;
	private  String ownershipIndicator;
	private  String dateOpenedorDisbursed;
	private  String dateOfLastPayment;
	private  String dateClosed;
	private  String dateReported;
	private  BigDecimal highCreditorSanctionedAmt;
	private  BigDecimal currentBalance;
	private  BigDecimal amtOverdue;
	private  int noOfDaysPastDue;
	private  String oldMemberCode;
	private  String oldMemberShortName;
	private  long oldAccountNo;
	private  String oldAccountType;
	private  String oldOwnershipIndicator;
	private  String suitFiledorWilfulDefault;
	private  String writtenOffAndSettledStatus;
	private  String assetClassification;
	private  BigDecimal valueOfCollateral;
	private  String typeOfCollateral;
	private  BigDecimal creditLimit;
	private  BigDecimal cashLimit;
	private  float rateOfInterest;
	private  int repaymentTenure;
	private  BigDecimal EMIAmount;
	private  BigDecimal writtenOffAmountTotal;
	private  BigDecimal writtenOffPrincipalAmount;
	private  BigDecimal settlementAmount;
	private  String paymentFrequency;
	private  BigDecimal actualPaymentAmt;
	private  String occupationCode;
	private  BigDecimal income;
	private  String netorGrossIncomeIndicator;
	private  String monthlyAnnualIncomeIndicator;
	private  int hasGaurantor;
	private  String gaurantorName;
	private  String gaurantorDOB;
	private  String gaurantorMobileNo;
	private  String gaurantorstate;
	private  String gaurantorAddress;
	private  String gaurantorPINCode;
	
	public CreditBureauReportDetailData( long loanId,String consumerName,String dateOfBirth,int gender,String incomeTaxIdNumber,String passportNumber,String passportIssueDate,
			String passportExpiryDate,String voterIdNumber,String drivingLicenseNumber,String drivingLicenseIssueDate,String drivingLicenseExpiryDate,String rationCardNumber,
	  String universalIdNumber,String additionalId1,String additionalId2,String telephoneNoMobile,
	  String telephoneNoResidence, String telephoneNoOffice,String extensionOffice,String telephoneNoOther,String extensionOther,
	  String emailId1, String emailId2,String address1,String stateCode1, String PINCode1,String addressCategory1,String residenceCode1,
	  String address2,String stateCode2,String PINCode2,String addressCategory2,String residenceCode2,String address3,
	  String stateCode3,String PINCode3,String addressCategory3,String residenceCode3,String currentNewMemberCode,String currentNewMemberShortName,
	  long currentNewAccountNo,String accountType,String ownershipIndicator,String dateOpenedorDisbursed,
	  String dateOfLastPayment,String dateClosed, String dateReported,BigDecimal highCreditorSanctionedAmt,
	  BigDecimal currentBalance,BigDecimal amtOverdue,int noOfDaysPastDue,String oldMemberCode,String oldMemberShortName,long oldAccountNo,
	  String oldAccountType, String oldOwnershipIndicator,String suitFiledorWilfulDefault, String writtenOffAndSettledStatus,String assetClassification,BigDecimal valueOfCollateral,String typeOfCollateral,BigDecimal creditLimit,
	  BigDecimal cashLimit,float rateOfInterest,int repaymentTenure,BigDecimal EMIAmount, BigDecimal writtenOffAmountTotal,  BigDecimal writtenOffPrincipalAmount,  BigDecimal settlementAmount,  String paymentFrequency, BigDecimal actualPaymentAmt, String occupationCode,
	  BigDecimal income,  String netorGrossIncomeIndicator, String monthlyAnnualIncomeIndicator,int hasGaurantor,String gaurantorName,
	  String gaurantorDOB,String gaurantorMobileNo,String gaurantorstate,String gaurantorAddress,String gaurantorPINCode)
	{
		 this.loanId=loanId;
		 this.consumerName=consumerName;
		 this.dateOfBirth=dateOfBirth;
		 this.gender=gender;
		 this.incomeTaxIdNumber=incomeTaxIdNumber;
		 this.passportNumber=passportNumber;
		 this.passportIssueDate=passportIssueDate;
		 this.passportExpiryDate=passportExpiryDate;
		 this.voterIdNumber=voterIdNumber;
		 this.drivingLicenseNumber=drivingLicenseNumber;
		 this.drivingLicenseIssueDate=drivingLicenseIssueDate;
		 this.drivingLicenseExpiryDate=drivingLicenseExpiryDate;
		 this.rationCardNumber=rationCardNumber;
		 this.universalIdNumber=universalIdNumber;
		 this.additionalId1=additionalId1;
		 this.additionalId2=additionalId2;
		 this.telephoneNoMobile=telephoneNoMobile;
		 this.telephoneNoResidence=telephoneNoResidence;
		 this.telephoneNoOffice=telephoneNoOffice;
		 this.extensionOffice=extensionOffice;
		 this.telephoneNoOther=telephoneNoOther;
		 this.extensionOther=extensionOther;
		 this.emailId1=emailId1;
		 this.emailId2=emailId2;
		 this.address1=address1;
		 this.stateCode1=stateCode1;
		 this.PINCode1=PINCode1;
		 this.addressCategory1=addressCategory1;
		 this.residenceCode1=residenceCode1;
		 this.address2=address2;
		 this.stateCode2=stateCode2;
		 this.PINCode2=PINCode2;
		 this.addressCategory2=addressCategory2;
		 this.residenceCode2=residenceCode2;
		 this.address3=address3;
		 this.stateCode3=stateCode3;
		 this.PINCode3=PINCode3;
		 this.addressCategory3=addressCategory3;
		 this.residenceCode3=residenceCode3;
		 this.currentNewMemberCode=currentNewMemberCode;
		 this.currentNewMemberShortName=currentNewMemberShortName;
		 this.currentNewAccountNo=currentNewAccountNo;
		 this.accountType=accountType;
		 this.ownershipIndicator=ownershipIndicator;
		 this.dateOpenedorDisbursed=dateOpenedorDisbursed;
		 this.dateOfLastPayment=dateOfLastPayment;
		 this.dateClosed=dateClosed;
		 this.dateReported=dateReported;
		 this.highCreditorSanctionedAmt=highCreditorSanctionedAmt;
		 this.currentBalance=currentBalance;
		 this.amtOverdue=amtOverdue;
		 this.noOfDaysPastDue=noOfDaysPastDue;
		 this.oldMemberCode=oldMemberCode;
		 this.oldMemberShortName=oldMemberShortName;
		 this.oldAccountNo=oldAccountNo;
		 this.oldAccountType=oldAccountType;
		 this.oldOwnershipIndicator=oldOwnershipIndicator;
		 this.suitFiledorWilfulDefault=suitFiledorWilfulDefault;
		 this.writtenOffAndSettledStatus=writtenOffAndSettledStatus;
		 this.assetClassification=assetClassification;
		 this.valueOfCollateral=valueOfCollateral;
		 this.typeOfCollateral=typeOfCollateral;
		 this.creditLimit=creditLimit;
		 this.cashLimit=cashLimit;
		 this.rateOfInterest=rateOfInterest;
		 this.repaymentTenure=repaymentTenure;
		 this.EMIAmount=EMIAmount;
		 this.writtenOffAmountTotal=writtenOffAmountTotal;
		 this.writtenOffPrincipalAmount=writtenOffPrincipalAmount;
		 this.settlementAmount=settlementAmount;
		 this.paymentFrequency=paymentFrequency;
		 this.actualPaymentAmt=actualPaymentAmt;
		 this.occupationCode=occupationCode;
		 this.income=income;
		 this.netorGrossIncomeIndicator=netorGrossIncomeIndicator;
		 this.monthlyAnnualIncomeIndicator=monthlyAnnualIncomeIndicator;
		 this.hasGaurantor=hasGaurantor;
		 this.gaurantorName= gaurantorName;
	     this.gaurantorDOB= gaurantorDOB;
		 this.gaurantorMobileNo= gaurantorMobileNo;
		 this.gaurantorstate= gaurantorstate;
		 this.gaurantorAddress=gaurantorAddress;
	     this.gaurantorPINCode=gaurantorPINCode;
	
	}
	

	public String getConsumerName() {
		return consumerName;
	}

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public int getGender() {
		return gender;
	}

	public String getIncomeTaxIdNumber() {
		return incomeTaxIdNumber;
	}

	public String getPassportNumber() {
		return passportNumber;
	}

	public String getPassportIssueDate() {
		return passportIssueDate;
	}

	public String getPassportExpiryDate() {
		return passportExpiryDate;
	}

	public String getVoterIdNumber() {
		return voterIdNumber;
	}

	public String getDrivingLicenseNumber() {
		return drivingLicenseNumber;
	}

	public String getDrivingLicenseIssueDate() {
		return drivingLicenseIssueDate;
	}

	public String getDrivingLicenseExpiryDate() {
		return drivingLicenseExpiryDate;
	}

	public String getRationCardNumber() {
		return rationCardNumber;
	}

	public String getUniversalIdNumber() {
		return universalIdNumber;
	}

	public String getAdditionalId1() {
		return additionalId1;
	}

	public String getAdditionalId2() {
		return additionalId2;
	}

	public String getTelephoneNoMobile() {
		return telephoneNoMobile;
	}

	public String getTelephoneNoResidence() {
		return telephoneNoResidence;
	}

	public String getTelephoneNoOffice() {
		return telephoneNoOffice;
	}

	public String getExtensionOffice() {
		return extensionOffice;
	}

	public String getTelephoneNoOther() {
		return telephoneNoOther;
	}

	public String getExtensionOther() {
		return extensionOther;
	}

	public String getEmailId1() {
		return emailId1;
	}

	public String getEmailId2() {
		return emailId2;
	}

	public String getAddress1() {
		return address1;
	}

	public String getStateCode1() {
		return stateCode1;
	}

	public String getPINCode1() {
		return PINCode1;
	}

	public String getAddressCategory1() {
		return addressCategory1;
	}

	public String getResidenceCode1() {
		return residenceCode1;
	}

	public String getAddress2() {
		return address2;
	}

	public String getStateCode2() {
		return stateCode2;
	}

	public String getPINCode2() {
		return PINCode2;
	}

	public String getAddressCategory2() {
		return addressCategory2;
	}

	public String getResidenceCode2() {
		return residenceCode2;
	}

	public String getAddress3() {
		return address3;
	}

	public String getStateCode3() {
		return stateCode3;
	}

	public String getPINCode3() {
		return PINCode3;
	}

	public String getAddressCategory3() {
		return addressCategory3;
	}

	public String getResidenceCode3() {
		return residenceCode3;
	}

	public String getCurrentNewMemberCode() {
		return currentNewMemberCode;
	}

	public String getCurrentNewMemberShortName() {
		return currentNewMemberShortName;
	}

	public long getCurrentNewAccountNo() {
		return currentNewAccountNo;
	}

	public String getAccountType() {
		return accountType;
	}

	public String getOwnershipIndicator() {
		return ownershipIndicator;
	}

	public String getDateOpenedorDisbursed() {
		return dateOpenedorDisbursed;
	}

	public String getDateOfLastPayment() {
		return dateOfLastPayment;
	}

	public String getDateClosed() {
		return dateClosed;
	}

	public String getDateReported() {
		return dateReported;
	}

	public BigDecimal getHighCreditorSanctionedAmt() {
		return highCreditorSanctionedAmt;
	}

	public BigDecimal getCurrentBalance() {
		return currentBalance;
	}

	public BigDecimal getAmtOverdue() {
		return amtOverdue;
	}

	public int getNoOfDaysPastDue() {
		return noOfDaysPastDue;
	}

	public String getOldMemberCode() {
		return oldMemberCode;
	}

	public String getOldMemberShortName() {
		return oldMemberShortName;
	}

	public long getOldAccountNo() {
		return oldAccountNo;
	}

	public String getOldAccountType() {
		return oldAccountType;
	}

	public String getOldOwnershipIndicator() {
		return oldOwnershipIndicator;
	}

	public String getSuitFiledorWilfulDefault() {
		return suitFiledorWilfulDefault;
	}

	public String getWrittenOffAndSettledStatus() {
		return writtenOffAndSettledStatus;
	}

	public String getAssetClassification() {
		return assetClassification;
	}

	public BigDecimal getValueOfCollateral() {
		return valueOfCollateral;
	}

	public String getTypeOfCollateral() {
		return typeOfCollateral;
	}

	public BigDecimal getCreditLimit() {
		return creditLimit;
	}

	public BigDecimal getCashLimit() {
		return cashLimit;
	}

	public float getRateOfInterest() {
		return rateOfInterest;
	}

	public int getRepaymentTenure() {
		return repaymentTenure;
	}

	public BigDecimal getEMIAmount() {
		return EMIAmount;
	}

	public BigDecimal getWrittenOffAmountTotal() {
		return writtenOffAmountTotal;
	}

	public BigDecimal getWrittenOffPrincipalAmount() {
		return writtenOffPrincipalAmount;
	}

	public BigDecimal getSettlementAmount() {
		return settlementAmount;
	}

	public String getPaymentFrequency() {
		return paymentFrequency;
	}

	public BigDecimal getActualPaymentAmt() {
		return actualPaymentAmt;
	}

	public String getOccupationCode() {
		return occupationCode;
	}

	public BigDecimal getIncome() {
		return income;
	}

	public String getNetorGrossIncomeIndicator() {
		return netorGrossIncomeIndicator;
	}

	public String getMonthlyAnnualIncomeIndicator() {
		return monthlyAnnualIncomeIndicator;
	}

	public void setConsumerName(String consumerName) {
		this.consumerName = consumerName;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public void setIncomeTaxIdNumber(String incomeTaxIdNumber) {
		this.incomeTaxIdNumber = incomeTaxIdNumber;
	}

	public void setPassportNumber(String passportNumber) {
		this.passportNumber = passportNumber;
	}

	public void setPassportIssueDate(String passportIssueDate) {
		this.passportIssueDate = passportIssueDate;
	}

	public void setPassportExpiryDate(String passportExpiryDate) {
		this.passportExpiryDate = passportExpiryDate;
	}

	public void setVoterIdNumber(String voterIdNumber) {
		this.voterIdNumber = voterIdNumber;
	}

	public void setDrivingLicenseNumber(String drivingLicenseNumber) {
		this.drivingLicenseNumber = drivingLicenseNumber;
	}

	public void setDrivingLicenseIssueDate(String drivingLicenseIssueDate) {
		this.drivingLicenseIssueDate = drivingLicenseIssueDate;
	}

	public void setDrivingLicenseExpiryDate(String drivingLicenseExpiryDate) {
		this.drivingLicenseExpiryDate = drivingLicenseExpiryDate;
	}

	public void setRationCardNumber(String rationCardNumber) {
		this.rationCardNumber = rationCardNumber;
	}

	public void setUniversalIdNumber(String universalIdNumber) {
		this.universalIdNumber = universalIdNumber;
	}

	public void setAdditionalId1(String additionalId1) {
		this.additionalId1 = additionalId1;
	}

	public void setAdditionalId2(String additionalId2) {
		this.additionalId2 = additionalId2;
	}

	public void setTelephoneNoMobile(String telephoneNoMobile) {
		this.telephoneNoMobile = telephoneNoMobile;
	}

	public void setTelephoneNoResidence(String telephoneNoResidence) {
		this.telephoneNoResidence = telephoneNoResidence;
	}

	public void setTelephoneNoOffice(String telephoneNoOffice) {
		this.telephoneNoOffice = telephoneNoOffice;
	}

	public void setExtensionOffice(String extensionOffice) {
		this.extensionOffice = extensionOffice;
	}

	public void setTelephoneNoOther(String telephoneNoOther) {
		this.telephoneNoOther = telephoneNoOther;
	}

	public void setExtensionOther(String extensionOther) {
		this.extensionOther = extensionOther;
	}

	public void setEmailId1(String emailId1) {
		this.emailId1 = emailId1;
	}

	public void setEmailId2(String emailId2) {
		this.emailId2 = emailId2;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public void setStateCode1(String stateCode1) {
		this.stateCode1 = stateCode1;
	}

	public void setPINCode1(String pINCode1) {
		PINCode1 = pINCode1;
	}

	public void setAddressCategory1(String addressCategory1) {
		this.addressCategory1 = addressCategory1;
	}

	public void setResidenceCode1(String residenceCode1) {
		this.residenceCode1 = residenceCode1;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public void setStateCode2(String stateCode2) {
		this.stateCode2 = stateCode2;
	}

	public void setPINCode2(String pINCode2) {
		PINCode2 = pINCode2;
	}

	public void setAddressCategory2(String addressCategory2) {
		this.addressCategory2 = addressCategory2;
	}

	public void setResidenceCode2(String residenceCode2) {
		this.residenceCode2 = residenceCode2;
	}

	public void setAddress3(String address3) {
		this.address3 = address3;
	}

	public void setStateCode3(String stateCode3) {
		this.stateCode3 = stateCode3;
	}

	public void setPINCode3(String pINCode3) {
		PINCode3 = pINCode3;
	}

	public void setAddressCategory3(String addressCategory3) {
		this.addressCategory3 = addressCategory3;
	}

	public void setResidenceCode3(String residenceCode3) {
		this.residenceCode3 = residenceCode3;
	}

	public void setCurrentNewMemberCode(String currentNewMemberCode) {
		this.currentNewMemberCode = currentNewMemberCode;
	}

	public void setCurrentNewMemberShortName(String currentNewMemberShortName) {
		this.currentNewMemberShortName = currentNewMemberShortName;
	}

	public void setCurrentNewAccountNo(long currentNewAccountNo) {
		this.currentNewAccountNo = currentNewAccountNo;
	}

	public void setAccountType(String accountType) {
		this.accountType = accountType;
	}

	public void setOwnershipIndicator(String ownershipIndicator) {
		this.ownershipIndicator = ownershipIndicator;
	}

	public void setDateOpenedorDisbursed(String dateOpenedorDisbursed) {
		this.dateOpenedorDisbursed = dateOpenedorDisbursed;
	}

	public void setDateOfLastPayment(String dateOfLastPayment) {
		this.dateOfLastPayment = dateOfLastPayment;
	}

	public void setDateClosed(String dateClosed) {
		this.dateClosed = dateClosed;
	}

	public void setDateReported(String dateReported) {
		this.dateReported = dateReported;
	}

	public void setHighCreditorSanctionedAmt(BigDecimal highCreditorSanctionedAmt) {
		this.highCreditorSanctionedAmt = highCreditorSanctionedAmt;
	}

	public void setCurrentBalance(BigDecimal currentBalance) {
		this.currentBalance = currentBalance;
	}

	public void setAmtOverdue(BigDecimal amtOverdue) {
		this.amtOverdue = amtOverdue;
	}

	public void setNoOfDaysPastDue(int noOfDaysPastDue) {
		this.noOfDaysPastDue = noOfDaysPastDue;
	}

	public void setOldMemberCode(String oldMemberCode) {
		this.oldMemberCode = oldMemberCode;
	}

	public void setOldMemberShortName(String oldMemberShortName) {
		this.oldMemberShortName = oldMemberShortName;
	}

	public void setOldAccountNo(long oldAccountNo) {
		this.oldAccountNo = oldAccountNo;
	}

	public void setOldAccountType(String oldAccountType) {
		this.oldAccountType = oldAccountType;
	}

	public void setOldOwnershipIndicator(String oldOwnershipIndicator) {
		this.oldOwnershipIndicator = oldOwnershipIndicator;
	}

	public void setSuitFiledorWilfulDefault(String suitFiledorWilfulDefault) {
		this.suitFiledorWilfulDefault = suitFiledorWilfulDefault;
	}

	public void setWrittenOffAndSettledStatus(String writtenOffAndSettledStatus) {
		this.writtenOffAndSettledStatus = writtenOffAndSettledStatus;
	}

	public void setAssetClassification(String assetClassification) {
		this.assetClassification = assetClassification;
	}

	public void setValueOfCollateral(BigDecimal valueOfCollateral) {
		this.valueOfCollateral = valueOfCollateral;
	}

	public void setTypeOfCollateral(String typeOfCollateral) {
		this.typeOfCollateral = typeOfCollateral;
	}

	public void setCreditLimit(BigDecimal creditLimit) {
		this.creditLimit = creditLimit;
	}

	public void setCashLimit(BigDecimal cashLimit) {
		this.cashLimit = cashLimit;
	}

	public void setRateOfInterest(float rateOfInterest) {
		this.rateOfInterest = rateOfInterest;
	}

	public void setRepaymentTenure(int repaymentTenure) {
		this.repaymentTenure = repaymentTenure;
	}

	public void setEMIAmount(BigDecimal eMIAmount) {
		EMIAmount = eMIAmount;
	}

	public void setWrittenOffAmountTotal(BigDecimal writtenOffAmountTotal) {
		this.writtenOffAmountTotal = writtenOffAmountTotal;
	}

	public void setWrittenOffPrincipalAmount(BigDecimal writtenOffPrincipalAmount) {
		this.writtenOffPrincipalAmount = writtenOffPrincipalAmount;
	}

	public void setSettlementAmount(BigDecimal settlementAmount) {
		this.settlementAmount = settlementAmount;
	}

	public void setPaymentFrequency(String paymentFrequency) {
		this.paymentFrequency = paymentFrequency;
	}

	public void setActualPaymentAmt(BigDecimal actualPaymentAmt) {
		this.actualPaymentAmt = actualPaymentAmt;
	}

	public void setOccupationCode(String occupationCode) {
		this.occupationCode = occupationCode;
	}

	public void setIncome(BigDecimal income) {
		this.income = income;
	}

	public void setNetorGrossIncomeIndicator(String netorGrossIncomeIndicator) {
		this.netorGrossIncomeIndicator = netorGrossIncomeIndicator;
	}

	public void setMonthlyAnnualIncomeIndicator(String monthlyAnnualIncomeIndicator) {
		this.monthlyAnnualIncomeIndicator = monthlyAnnualIncomeIndicator;
	}

	public long getLoanId() {
		return loanId;
	}

	public int getHasGaurantor() {
		return hasGaurantor;
	}

	public void setLoanId(long loanId) {
		this.loanId = loanId;
	}

	public void setHasGaurantor(int hasGaurantor) {
		this.hasGaurantor = hasGaurantor;
	}
	public String getGaurantorName() {
		return gaurantorName;
	}
	public String getGaurantorDOB() {
		return gaurantorDOB;
	}
	public String getGaurantorMobileNo() {
		return gaurantorMobileNo;
	}
	public String getGaurantorstate() {
		return gaurantorstate;
	}
	public String getGaurantorAddress() {
		return gaurantorAddress;
	}
	public String getGaurantorPINCode() {
		return gaurantorPINCode;
	}
	public void setGaurantorName(String gaurantorName) {
		this.gaurantorName = gaurantorName;
	}
	public void setGaurantorDOB(String gaurantorDOB) {
		this.gaurantorDOB = gaurantorDOB;
	}
	public void setGaurantorMobileNo(String gaurantorMobileNo) {
		this.gaurantorMobileNo = gaurantorMobileNo;
	}
	public void setGaurantorstate(String gaurantorstate) {
		this.gaurantorstate = gaurantorstate;
	}
	public void setGaurantorAddress(String gaurantorAddress) {
		this.gaurantorAddress = gaurantorAddress;
	}
	public void setGaurantorPINCode(String gaurantorPINCode) {
		this.gaurantorPINCode = gaurantorPINCode;
	}
}
