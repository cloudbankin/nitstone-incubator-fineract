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
package org.apache.fineract.portfolio.loanaccount.jointBorrower.exception;

import org.apache.fineract.infrastructure.core.exception.AbstractPlatformResourceNotFoundException;

/**
 * A {@link RuntimeException} thrown when guarantor resources are not found.
 */
public class JointBorrowerNotFoundException extends AbstractPlatformResourceNotFoundException {

    public JointBorrowerNotFoundException(final Long id) {
        super("error.msg.loan.borrower.not.found", "Borrower with identifier " + id + " does not exist", id);
    }

    public JointBorrowerNotFoundException(final Long loanId, final Long guarantorId) {
        super("error.msg.loan.borrower.not.found", "Borrower with identifier " + guarantorId
                + " does not exist for loan with Identifier " + loanId, loanId, guarantorId);
    }

    public JointBorrowerNotFoundException(final Long loanId, final Long guarantorId, final Long guarantorFundingId) {
        super("error.msg.loan.borrower.not.found", "Borrower with identifier " + guarantorId + "and with funding detail "
                + guarantorFundingId + " does not exist for loan with Identifier " + loanId, loanId, guarantorId, guarantorFundingId);
    }
}