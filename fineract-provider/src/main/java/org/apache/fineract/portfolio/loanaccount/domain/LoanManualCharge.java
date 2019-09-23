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
package org.apache.fineract.portfolio.loanaccount.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeCalculationType;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeTimeType;
import org.apache.fineract.portfolio.charge.exception.LoanChargeWithoutMandatoryFieldException;
import org.apache.fineract.portfolio.loanaccount.command.LoanChargeCommand;
import org.apache.fineract.portfolio.loanaccount.data.LoanChargePaidDetail;
import org.apache.fineract.portfolio.loanaccount.data.LoanTransactionData;
import org.apache.fineract.portfolio.loanaccount.service.LoanReadPlatformServiceImpl;
import org.joda.time.LocalDate;

@Entity
@Table(name = "m_loan_manual_charge")
public class LoanManualCharge extends AbstractPersistableCustom<Long> {

	@ManyToOne(optional = false)
    @JoinColumn(name = "loan_id", referencedColumnName = "id", nullable = false)
    private Loan loan;

    @ManyToOne(optional = false)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    private GLAccount glAccount;

    @Temporal(TemporalType.DATE)
    @Column(name = "add_charge_date")
    private Date addChargeDate;
    
    @Temporal(TemporalType.DATE)
    @Column(name = "charge_paid_date")
    private Date chargePaidDate;
    
    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "amount", scale = 6, precision = 19, nullable = false)
    private BigDecimal amount;

    @Column(name = "amount_paid_derived", scale = 6, precision = 19, nullable = true)
    private BigDecimal amountPaid;

    @Column(name = "is_paid_derived", nullable = false)
    private boolean paid = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    
    public LoanManualCharge(final Loan loan, final GLAccount glAccount, final BigDecimal amount, 
            final String description, final Date addChargeDate) {
        this.loan = loan;
        this.glAccount = glAccount;
        this.amount = amount;
        this.amountPaid=BigDecimal.ZERO;
        this.description = description;
        this.addChargeDate = addChargeDate;
     
    }
    
    public Loan getLoan() {
		return loan;
	}



	public void setLoan(Loan loan) {
		this.loan = loan;
	}



	public Date getAddChargeDate() {
		return addChargeDate;
	}



	public void setAddChargeDate(Date addChargeDate) {
		this.addChargeDate = addChargeDate;
	}



	public String getDescription() {
		return description;
	}



	public void setDescription(String description) {
		this.description = description;
	}



	public BigDecimal getAmount() {
		return amount;
	}



	public void setAmount(BigDecimal amount) {
		this.amount = amount;
	}



	public BigDecimal getAmountPaid() {
		return amountPaid;
	}



	public void setAmountPaid(BigDecimal amountPaid) {
		this.amountPaid = amountPaid;
	}



	public boolean isPaid() {
		return paid;
	}



	public void setPaid(boolean paid) {
		this.paid = paid;
	}



	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
    
    public GLAccount getGlAccount() {
		return glAccount;
	}

	public void setGlAccount(GLAccount glAccount) {
		this.glAccount = glAccount;
	}
	

	public Date getChargePaidDate() {
		return chargePaidDate;
	}

	public void setChargePaidDate(Date chargePaidDate) {
		this.chargePaidDate = chargePaidDate;
	}

    
}