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
package org.apache.fineract.portfolio.loanaccount.data;

import java.math.BigDecimal;

import org.joda.time.LocalDate;

/**
 * Immutable data object representing loan summary information.
 */
public class LoanDueData {

    private final String currency;
    private final String loanAccountNumber;
    private final String clientAccountNumber;
    private final String clientName;
    private final String clientMobileNumber;
    private final BigDecimal dueAmount;
    private final LocalDate dueSinceDate;
    
    

    public LoanDueData(final String currency, final String loanAccountNumber, final String clientAccountNumber,
            final String clientName, final String clientMobileNumber, final BigDecimal dueAmount,
            final LocalDate dueSinceDate) {
        this.currency = currency;
        this.loanAccountNumber = loanAccountNumber;
        this.clientAccountNumber = clientAccountNumber;
        this.clientName = clientName;
        this.clientMobileNumber = clientMobileNumber;
        this.dueAmount = dueAmount;
        this.dueSinceDate = dueSinceDate;
    }


	public String getCurrency() {
		return currency;
	}


	public String getLoanAccountNumber() {
		return loanAccountNumber;
	}


	public String getClientAccountNumber() {
		return clientAccountNumber;
	}


	public String getClientName() {
		return clientName;
	}


	public String getClientMobileNumber() {
		return clientMobileNumber;
	}


	public BigDecimal getDueAmount() {
		return dueAmount;
	}


	public LocalDate getDueSinceDate() {
		return dueSinceDate;
	}

    
}