package org.apache.fineract.finabile.data;

import java.math.BigDecimal;
import java.util.Date;

public class AccuralValueDetailData {
	
	private  String clientName;
	private  String loanAccount;
	private  Date transactionDate;
	private  BigDecimal interestAccrued;
	private  BigDecimal penalityAccrued;

	public AccuralValueDetailData(String clientName, String loanAccount, Date transactionDate, BigDecimal interestAccrued,
			BigDecimal penalityAccrued) {
		
		this.clientName = clientName;
		this.loanAccount = loanAccount;
		this.transactionDate = transactionDate;
		this.interestAccrued = interestAccrued;
		this.penalityAccrued = penalityAccrued;
	}

	public String getClientName() {
		return clientName;
	}

	public void setClientName(String clientName) {
		this.clientName = clientName;
	}

	public String getLoanAccount() {
		return loanAccount;
	}

	public void setLoanAccount(String loanAccount) {
		this.loanAccount = loanAccount;
	}

	public Date getTransactionDate() {
		return transactionDate;
	}

	public void setTransactionDate(Date transactionDate) {
		this.transactionDate = transactionDate;
	}

	public BigDecimal getInterestAccrued() {
		return interestAccrued;
	}

	public void setInterestAccrued(BigDecimal interestAccrued) {
		this.interestAccrued = interestAccrued;
	}

	public BigDecimal getPenalityAccrued() {
		return penalityAccrued;
	}

	public void setPenalityAccrued(BigDecimal penalityAccrued) {
		this.penalityAccrued = penalityAccrued;
	}

}
