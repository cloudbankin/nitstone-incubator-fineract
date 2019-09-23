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
package org.apache.fineract.portfolio.loanaccount.rescheduleloan.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.monetary.domain.MoneyHelper;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.address.service.AddressReadPlatformServiceImpl;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetails;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.data.LoanTermVariationsData;
import org.apache.fineract.portfolio.loanaccount.data.ScheduleGeneratorDTO;
import org.apache.fineract.portfolio.loanaccount.domain.ChangedTransactionDetail;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanAccountDomainService;
import org.apache.fineract.portfolio.loanaccount.domain.LoanCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidBy;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargePaidByRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanChargeRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanInterestRecalcualtionAdditionalDetails;
import org.apache.fineract.portfolio.loanaccount.domain.LoanLifecycleStateMachine;
import org.apache.fineract.portfolio.loanaccount.domain.LoanOverdueInstallmentCharge;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallment;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleInstallmentRepository;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepaymentScheduleTransactionProcessorFactory;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanRescheduleRequestToTermVariationMapping;
import org.apache.fineract.portfolio.loanaccount.domain.LoanStatus;
import org.apache.fineract.portfolio.loanaccount.domain.LoanSummaryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariationType;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTermVariations;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransactionRepository;
import org.apache.fineract.portfolio.loanaccount.domain.transactionprocessor.LoanRepaymentScheduleTransactionProcessor;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleDTO;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanApplicationTerms;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanRepaymentScheduleHistoryRepository;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleGeneratorFactory;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.RescheduleLoansApiConstants;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.data.LoanRescheduleRequestDataValidator;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequestRepository;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.exception.LoanRescheduleRequestNotFoundException;
import org.apache.fineract.portfolio.loanaccount.service.LoanAccrualPlatformService;
import org.apache.fineract.portfolio.loanaccount.service.LoanAssembler;
import org.apache.fineract.portfolio.loanaccount.service.LoanSchedularService;
import org.apache.fineract.portfolio.loanaccount.service.LoanUtilService;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.exception.InvalidCurrencyException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.finabile.fineract.portfolio.code.data.FinabileCodeValueData;
import org.finabile.fineract.portfolio.code.service.FinabileCodeValueReadPlatformService;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanRescheduleRequestWritePlatformServiceImpl implements LoanRescheduleRequestWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(LoanRescheduleRequestWritePlatformServiceImpl.class);

    private final CodeValueRepositoryWrapper codeValueRepositoryWrapper;
    private final PlatformSecurityContext platformSecurityContext;
    private final LoanRescheduleRequestDataValidator loanRescheduleRequestDataValidator;
    private final LoanRescheduleRequestRepository loanRescheduleRequestRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final LoanRepaymentScheduleHistoryRepository loanRepaymentScheduleHistoryRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final LoanTransactionRepository loanTransactionRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final LoanRepositoryWrapper loanRepositoryWrapper;
    private final LoanAssembler loanAssembler;
    private final LoanUtilService loanUtilService;
    private final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory;
    private final LoanScheduleGeneratorFactory loanScheduleFactory;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final DefaultScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
    private final LoanAccountDomainService loanAccountDomainService;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    /** Habile changes for extension fees Here we initialize objects */
	private final LoanChargeRepository loanChargeRepository;
	private final LoanChargePaidByRepository loanChargePaidByRepository;
	private final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository;
	private final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository;
	private static LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepositoryBean;
	private final LoanSchedularService loanSchedularService;
	private final LoanAccrualPlatformService loanAccrualPlatformService;
	private final ClientRepositoryWrapper clientRepositoryWrapper;
	private final AddressReadPlatformServiceImpl addressReadPlatformServiceImpl;
	private final ConfigurationDomainService configurationDomainService;
	private final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService;

	/** Habile changes end */

    /**
     * LoanRescheduleRequestWritePlatformServiceImpl constructor
     * 
     * 
     **/
    @Autowired
    public LoanRescheduleRequestWritePlatformServiceImpl(final CodeValueRepositoryWrapper codeValueRepositoryWrapper,
            final PlatformSecurityContext platformSecurityContext,
            final LoanRescheduleRequestDataValidator loanRescheduleRequestDataValidator,
            final LoanRescheduleRequestRepository loanRescheduleRequestRepository,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
            final LoanRepaymentScheduleHistoryRepository loanRepaymentScheduleHistoryRepository,
            final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService,
            final LoanTransactionRepository loanTransactionRepository,
            final JournalEntryWritePlatformService journalEntryWritePlatformService, final LoanRepositoryWrapper loanRepositoryWrapper,
            final LoanAssembler loanAssembler, final LoanUtilService loanUtilService,
            final LoanRepaymentScheduleTransactionProcessorFactory loanRepaymentScheduleTransactionProcessorFactory,
            final LoanScheduleGeneratorFactory loanScheduleFactory, final LoanSummaryWrapper loanSummaryWrapper,
            final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
            final LoanAccountDomainService loanAccountDomainService,
            final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
			final LoanChargeRepository loanChargeRepository,
			final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository,
			final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository,
			LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepositoryBean,
			final LoanChargePaidByRepository loanChargePaidByRepository,
			final LoanSchedularService loanSchedularService,
			final LoanAccrualPlatformService loanAccrualPlatformService,
			final ClientRepositoryWrapper clientRepositoryWrapper,
			final AddressReadPlatformServiceImpl addressReadPlatformServiceImpl,
			final ConfigurationDomainService configurationDomainService,
			final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService) {
        this.codeValueRepositoryWrapper = codeValueRepositoryWrapper;
        this.platformSecurityContext = platformSecurityContext;
        this.loanRescheduleRequestDataValidator = loanRescheduleRequestDataValidator;
        this.loanRescheduleRequestRepository = loanRescheduleRequestRepository;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.loanRepaymentScheduleHistoryRepository = loanRepaymentScheduleHistoryRepository;
        this.loanScheduleHistoryWritePlatformService = loanScheduleHistoryWritePlatformService;
        this.loanTransactionRepository = loanTransactionRepository;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.loanRepositoryWrapper = loanRepositoryWrapper;
        this.loanAssembler = loanAssembler;
        this.loanUtilService = loanUtilService;
        this.loanRepaymentScheduleTransactionProcessorFactory = loanRepaymentScheduleTransactionProcessorFactory;
        this.loanScheduleFactory = loanScheduleFactory;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.loanAccountDomainService = loanAccountDomainService;
        this.repaymentScheduleInstallmentRepository = repaymentScheduleInstallmentRepository;
        this.loanChargeRepository = loanChargeRepository;
		this.loanChargeAdditionalDetailsRepository = loanChargeAdditionalDetailsRepository;
		this.chargeAdditionalDetailsRepository = chargeAdditionalDetailsRepository;
		this.loanChargeAdditionalDetailsRepositoryBean = loanChargeAdditionalDetailsRepositoryBean;
		this.loanChargePaidByRepository = loanChargePaidByRepository;
		this.loanSchedularService = loanSchedularService;
		this.loanAccrualPlatformService = loanAccrualPlatformService;
		this.clientRepositoryWrapper = clientRepositoryWrapper;
		this.addressReadPlatformServiceImpl = addressReadPlatformServiceImpl;
		this.configurationDomainService = configurationDomainService;
		this.finabileCodeValueReadPlatformService = finabileCodeValueReadPlatformService;
    }

    /**
     * create a new instance of the LoanRescheduleRequest object from the
     * JsonCommand object and persist
     * 
     * @return CommandProcessingResult object
     **/
    @Override
    @Transactional
    public CommandProcessingResult create(JsonCommand jsonCommand) {

        try {
            // get the loan id from the JsonCommand object
            final Long loanId = jsonCommand.longValueOfParameterNamed(RescheduleLoansApiConstants.loanIdParamName);

            // use the loan id to get a Loan entity object
            final Loan loan = this.loanAssembler.assembleFrom(loanId);

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForCreateAction(jsonCommand, loan);

            // get the reschedule reason code value id from the JsonCommand
            // object
            final Long rescheduleReasonId = jsonCommand.longValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleReasonIdParamName);

            // use the reschedule reason code value id to get a CodeValue entity
            // object
            final CodeValue rescheduleReasonCodeValue = this.codeValueRepositoryWrapper.findOneWithNotFoundDetection(rescheduleReasonId);

            // get the grace on principal integer value from the JsonCommand
            // object
            final Integer graceOnPrincipal = jsonCommand
                    .integerValueOfParameterNamed(RescheduleLoansApiConstants.graceOnPrincipalParamName);

            // get the grace on interest integer value from the JsonCommand
            // object
            final Integer graceOnInterest = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.graceOnInterestParamName);

            // get the extra terms to be added at the end of the new schedule
            // from the JsonCommand object
            final Integer extraTerms = jsonCommand.integerValueOfParameterNamed(RescheduleLoansApiConstants.extraTermsParamName);

            // get the new interest rate that would be applied to the new loan
            // schedule
            final BigDecimal interestRate = jsonCommand
                    .bigDecimalValueOfParameterNamed(RescheduleLoansApiConstants.newInterestRateParamName);

            // get the reschedule reason comment text from the JsonCommand
            // object
            final String rescheduleReasonComment = jsonCommand
                    .stringValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleReasonCommentParamName);

            // get the recalculate interest option
            final Boolean recalculateInterest = jsonCommand
                    .booleanObjectValueOfParameterNamed(RescheduleLoansApiConstants.recalculateInterestParamName);

            // initialize set the value to null
            Date submittedOnDate = null;

            // check if the parameter is in the JsonCommand object
            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.submittedOnDateParamName)) {
                // create a LocalDate object from the "submittedOnDate" Date
                // string
                LocalDate localDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.submittedOnDateParamName);

                if (localDate != null) {
                    // update the value of the "submittedOnDate" variable
                    submittedOnDate = localDate.toDate();
                }
            }

            // initially set the value to null
            Date rescheduleFromDate = null;

            // start point of the rescheduling exercise
            Integer rescheduleFromInstallment = null;

            // initially set the value to null
            Date adjustedDueDate = null;

            // check if the parameter is in the JsonCommand object
            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.rescheduleFromDateParamName)) {
                // create a LocalDate object from the "rescheduleFromDate" Date
                // string
                LocalDate localDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.rescheduleFromDateParamName);

                if (localDate != null) {
                    // get installment by due date
                    LoanRepaymentScheduleInstallment installment = loan.getRepaymentScheduleInstallment(localDate);
                    rescheduleFromInstallment = installment.getInstallmentNumber();

                    // update the value of the "rescheduleFromDate" variable
                    rescheduleFromDate = localDate.toDate();
                }
            }

            if (jsonCommand.hasParameter(RescheduleLoansApiConstants.adjustedDueDateParamName)) {
                // create a LocalDate object from the "adjustedDueDate" Date
                // string
                LocalDate localDate = jsonCommand.localDateValueOfParameterNamed(RescheduleLoansApiConstants.adjustedDueDateParamName);

                if (localDate != null) {
                    // update the value of the "adjustedDueDate"variable
                    adjustedDueDate = localDate.toDate();
                }
            }

            final LoanRescheduleRequest loanRescheduleRequest = LoanRescheduleRequest.instance(loan,
                    LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), rescheduleFromInstallment, rescheduleFromDate,
                    recalculateInterest, rescheduleReasonCodeValue, rescheduleReasonComment, submittedOnDate,
                    this.platformSecurityContext.authenticatedUser(), null, null, null, null);

            // update reschedule request to term variations mapping
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = new ArrayList<>();
            final Boolean isActive = false;
            final boolean isSpecificToInstallment = false;
            BigDecimal decimalValue = null;
            Date dueDate = null;
            // create term variations for flat and declining balance loans
            createLoanTermVariationsForRegularLoans(loan, graceOnPrincipal, graceOnInterest, extraTerms, interestRate, rescheduleFromDate,
                    adjustedDueDate, loanRescheduleRequest, loanRescheduleRequestToTermVariationMappings, isActive,
                    isSpecificToInstallment, decimalValue, dueDate);

            // create a new entry in the m_loan_reschedule_request table
            this.loanRescheduleRequestRepository.save(loanRescheduleRequest);
            this.loanRepositoryWrapper.save(loan);

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequest.getId())
                    .withLoanId(loan.getId()).build();
        }

        catch (final DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private void createLoanTermVariationsForRegularLoans(final Loan loan, final Integer graceOnPrincipal, final Integer graceOnInterest,
            final Integer extraTerms, final BigDecimal interestRate, Date rescheduleFromDate, Date adjustedDueDate,
            final LoanRescheduleRequest loanRescheduleRequest,
            List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings, final Boolean isActive,
            final boolean isSpecificToInstallment, BigDecimal decimalValue, Date dueDate) {

        if (rescheduleFromDate != null && adjustedDueDate != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.DUE_DATE.getValue();
            createLoanTermVariations(termType, loan, rescheduleFromDate, adjustedDueDate, loanRescheduleRequestToTermVariationMappings,
                    isActive, isSpecificToInstallment, decimalValue, parent);
        }

        if (rescheduleFromDate != null && interestRate != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.INTEREST_RATE_FROM_INSTALLMENT.getValue();
            createLoanTermVariations(termType, loan, rescheduleFromDate, dueDate, loanRescheduleRequestToTermVariationMappings, isActive,
                    isSpecificToInstallment, interestRate, parent);
        }

        if (rescheduleFromDate != null && graceOnPrincipal != null) {
            final Integer termType = LoanTermVariationType.GRACE_ON_PRINCIPAL.getValue();
            LoanTermVariations parent = null;
            parent = createLoanTermVariations(termType, loan, rescheduleFromDate, dueDate, loanRescheduleRequestToTermVariationMappings,
                    isActive, isSpecificToInstallment, BigDecimal.valueOf(graceOnPrincipal), parent);
            
            BigDecimal extraTermsBasedOnGracePeriods = BigDecimal.valueOf(graceOnPrincipal);
            createLoanTermVariations(LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.getValue(), loan, rescheduleFromDate, dueDate,
                    loanRescheduleRequestToTermVariationMappings, isActive, isSpecificToInstallment, extraTermsBasedOnGracePeriods, parent);

        }

        if (rescheduleFromDate != null && graceOnInterest != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.GRACE_ON_INTEREST.getValue();
            createLoanTermVariations(termType, loan, rescheduleFromDate, dueDate, loanRescheduleRequestToTermVariationMappings, isActive,
                    isSpecificToInstallment, BigDecimal.valueOf(graceOnInterest), parent);
        }

        if (rescheduleFromDate != null && extraTerms != null) {
            LoanTermVariations parent = null;
            final Integer termType = LoanTermVariationType.EXTEND_REPAYMENT_PERIOD.getValue();
            createLoanTermVariations(termType, loan, rescheduleFromDate, dueDate, loanRescheduleRequestToTermVariationMappings, isActive,
                    isSpecificToInstallment, BigDecimal.valueOf(extraTerms), parent);
        }
        loanRescheduleRequest.updateLoanRescheduleRequestToTermVariationMappings(loanRescheduleRequestToTermVariationMappings);
    }

    private LoanTermVariations createLoanTermVariations(final Integer termType, final Loan loan, Date rescheduleFromDate,
            Date adjustedDueDate, List<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings,
            final Boolean isActive, final boolean isSpecificToInstallment, final BigDecimal decimalValue, LoanTermVariations parent) {
        LoanTermVariations loanTermVariation = new LoanTermVariations(termType, rescheduleFromDate, decimalValue, adjustedDueDate,
                isSpecificToInstallment, loan, loan.status().getValue(), isActive, parent);
        loanRescheduleRequestToTermVariationMappings.add(LoanRescheduleRequestToTermVariationMapping.createNew(loanTermVariation));
        return loanTermVariation;
    }
    
    /** Habile changes for extension fee */
	private BigDecimal extensionFeeAmount = BigDecimal.ZERO;
	private BigDecimal extensionServiceFeeAmount = BigDecimal.ZERO;
	private LocalDate rescheduleToDate = new LocalDate();
	private LocalDate rescheduleApprovedDate = new LocalDate();

	/** Habile changes end */
	
    @Override
    @Transactional
    public CommandProcessingResult approve(JsonCommand jsonCommand) {

        try {
        	
        	List<LoanChargeAdditionalDetails> loanChargeAdditionalDetails = new ArrayList<>();
			extensionFeeAmount = BigDecimal.ZERO;
			extensionServiceFeeAmount = BigDecimal.ZERO;
			
            final Long loanRescheduleRequestId = jsonCommand.entityId();

            final LoanRescheduleRequest loanRescheduleRequest = this.loanRescheduleRequestRepository.findOne(loanRescheduleRequestId);

            if (loanRescheduleRequest == null) { throw new LoanRescheduleRequestNotFoundException(loanRescheduleRequestId); }
            /** Habile changes for tax calculation */
			final Long clientId = loanRescheduleRequest.getLoan().getClientId();
			Client client = this.clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
			Office office = client.getOffice();

			final Long clientAddressTypeValueId = this.configurationDomainService.retrieveAddressTypeForClient();
			FinabileCodeValueData clientCodeValueData = this.finabileCodeValueReadPlatformService
					.getAddressTypeValue(clientAddressTypeValueId);
			AddressData clientAddress = this.addressReadPlatformServiceImpl
					.retrieveByEntityIdAndAddressType(client.getId(), "client", clientCodeValueData.getCodeValue());

			final Long officeAddressTypeValueId = this.configurationDomainService.retrieveAddressTypeForOffice();
			FinabileCodeValueData officeCodeValueData = this.finabileCodeValueReadPlatformService
					.getAddressTypeValue(officeAddressTypeValueId);
			AddressData officeAddress = this.addressReadPlatformServiceImpl
					.retrieveByEntityIdAndAddressType(office.getId(), "office", officeCodeValueData.getCodeValue());

			String taxComponentType = null;
			if (clientAddress.getStateName().equalsIgnoreCase(officeAddress.getStateName())) {
				taxComponentType = "Intra State";
			} else {
				taxComponentType = "Inter State";
			}
			/** Habile changes end */

			/** Habile changes - delete penalty charges after reschedule from date */
			Set<LoanCharge> applicableLoanChargesToRemove = new HashSet<>();
			for (LoanCharge loanCharge : loanRescheduleRequest.getLoan().getLoanCharges()) {
				if (loanCharge.isActive() && loanCharge.isPenaltyCharge() && !loanCharge.isPaid()
						&& loanCharge.getDueDate().after(loanRescheduleRequest.getRescheduleFromDate().toDate())) {
					List<LoanChargePaidBy> loanChargePaidBys = this.loanChargePaidByRepository
							.getLoanChargePaidBy(loanCharge.getId());
					if (loanChargePaidBys != null) {
						for (LoanChargePaidBy loanChargePaidBy : loanChargePaidBys) {
							for (LoanTransaction transaction : loanRescheduleRequest.getLoan().getLoanTransactions()) {
								if (loanChargePaidBy != null
										&& loanChargePaidBy.getLoanTransaction().equals(transaction)) {
									transaction.getLoanChargesPaid().remove(loanChargePaidBy);
									break;
								}
							}
						}
					}
					applicableLoanChargesToRemove.add(loanCharge);
				}
			}
			loanRescheduleRequest.getLoan().getLoanCharges().removeAll(applicableLoanChargesToRemove);
			/** Habile changes end */

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForApproveAction(jsonCommand, loanRescheduleRequest);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            LocalDate approvedOnDate = jsonCommand.localDateValueOfParameterNamed("approvedOnDate");
            final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(jsonCommand.dateFormat()).withLocale(
                    jsonCommand.extractLocale());

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("approvedOnDate", approvedOnDate.toString(dateTimeFormatter));
            changes.put("approvedByUserId", appUser.getId());

            Loan loan = loanRescheduleRequest.getLoan();
            final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
            final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

            ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan,
                    loanRescheduleRequest.getRescheduleFromDate());

            Collection<LoanRepaymentScheduleHistory> loanRepaymentScheduleHistoryList = this.loanScheduleHistoryWritePlatformService
                    .createLoanScheduleArchive(loan.getRepaymentScheduleInstallments(), loan, loanRescheduleRequest);

            final LoanApplicationTerms loanApplicationTerms = loan.constructLoanApplicationTerms(scheduleGeneratorDTO);

            LocalDate rescheduleFromDate = null;
            Set<LoanTermVariations> activeLoanTermVariations = loan.getActiveLoanTermVariations();
            LoanTermVariations dueDateVariationInCurrentRequest = loanRescheduleRequest.getDueDateTermVariationIfExists();
            if (dueDateVariationInCurrentRequest != null && activeLoanTermVariations != null) {
                LocalDate fromScheduleDate = dueDateVariationInCurrentRequest.fetchTermApplicaDate();
                LocalDate currentScheduleDate = fromScheduleDate;
                LocalDate modifiedScheduleDate = dueDateVariationInCurrentRequest.fetchDateValue();
                Map<LocalDate, LocalDate> changeMap = new HashMap<>();
                changeMap.put(currentScheduleDate, modifiedScheduleDate);
                for (LoanTermVariations activeLoanTermVariation : activeLoanTermVariations) {
                    if (activeLoanTermVariation.getTermType().isDueDateVariation()
                            && activeLoanTermVariation.fetchDateValue().equals(dueDateVariationInCurrentRequest.fetchTermApplicaDate())) {
                        activeLoanTermVariation.markAsInactive();
                        rescheduleFromDate = activeLoanTermVariation.fetchTermApplicaDate();
                        dueDateVariationInCurrentRequest.setTermApplicableFrom(rescheduleFromDate.toDate());
                    } else if (!activeLoanTermVariation.fetchTermApplicaDate().isBefore(fromScheduleDate)) {
                        while (currentScheduleDate.isBefore(activeLoanTermVariation.fetchTermApplicaDate())) {
                            currentScheduleDate = this.scheduledDateGenerator.generateNextRepaymentDate(currentScheduleDate,
                                    loanApplicationTerms, false);
                            modifiedScheduleDate = this.scheduledDateGenerator.generateNextRepaymentDate(modifiedScheduleDate,
                                    loanApplicationTerms, false);
                            changeMap.put(currentScheduleDate, modifiedScheduleDate);
                        }
                        if (changeMap.containsKey(activeLoanTermVariation.fetchTermApplicaDate())) {
                            activeLoanTermVariation.setTermApplicableFrom(changeMap.get(activeLoanTermVariation.fetchTermApplicaDate())
                                    .toDate());
                        }
                    }
                }
            }
            if (rescheduleFromDate == null) {
                rescheduleFromDate = loanRescheduleRequest.getRescheduleFromDate();
            }
            for (LoanRescheduleRequestToTermVariationMapping mapping : loanRescheduleRequest
                    .getLoanRescheduleRequestToTermVariationMappings()) {
                mapping.getLoanTermVariations().updateIsActive(true);
            }
            BigDecimal annualNominalInterestRate = null;
            List<LoanTermVariationsData> loanTermVariations = new ArrayList<>();
            loan.constructLoanTermVariations(scheduleGeneratorDTO.getFloatingRateDTO(), annualNominalInterestRate, loanTermVariations);
            loanApplicationTerms.getLoanTermVariations().setExceptionData(loanTermVariations);

            /*for (LoanTermVariationsData loanTermVariation : loanApplicationTerms.getLoanTermVariations().getDueDateVariation()) {
                if (rescheduleFromDate.isBefore(loanTermVariation.getTermApplicableFrom())) {
                    LocalDate applicableDate = this.scheduledDateGenerator.generateNextRepaymentDate(rescheduleFromDate,
                            loanApplicationTerms, false, loanApplicationTerms.getHolidayDetailDTO());
                    if (loanTermVariation.getTermApplicableFrom().equals(applicableDate)) {
                        LocalDate adjustedDate = this.scheduledDateGenerator.generateNextRepaymentDate(adjustedApplicableDate,
                                loanApplicationTerms, false, loanApplicationTerms.getHolidayDetailDTO());
                        loanTermVariation.setApplicableFromDate(adjustedDate);
                    }
                }
            }*/

            final RoundingMode roundingMode = MoneyHelper.getRoundingMode();
            final MathContext mathContext = new MathContext(8, roundingMode);
            final LoanRepaymentScheduleTransactionProcessor loanRepaymentScheduleTransactionProcessor = this.loanRepaymentScheduleTransactionProcessorFactory
                    .determineProcessor(loan.transactionProcessingStrategy());
            final LoanScheduleGenerator loanScheduleGenerator = this.loanScheduleFactory.create(loanApplicationTerms.getInterestMethod());
            final LoanLifecycleStateMachine loanLifecycleStateMachine = null;
            loan.setHelpers(loanLifecycleStateMachine, this.loanSummaryWrapper, this.loanRepaymentScheduleTransactionProcessorFactory);
            /** Habile changes for extension fee calculation */
			// calculating total extension fees amount
			for (LoanCharge charge : loan.getLoanCharges()) {
				LoanChargeAdditionalDetails lcad = this.loanChargeAdditionalDetailsRepository
						.getLoanChargeAdditionalDetails(charge);
				if (charge.isActive() && charge.isInstallmentRescheduled() && lcad.getIsEnabledAutoPaid() == 1) {
					extensionFeeAmount = extensionFeeAmount.add(charge.getAmount(loan.getCurrency()).getAmount());
				} else if (charge.isActive() && charge.isInstallmentRescheduled()) {
					extensionServiceFeeAmount = extensionServiceFeeAmount
							.add(charge.getAmountOutstanding(loan.getCurrency()).getAmount());
				}
			}

			rescheduleApprovedDate = approvedOnDate;
			rescheduleToDate = loanRescheduleRequest.getDueDateTermVariationIfExists().fetchDateValue();
			int numberOfDays = 0;

			numberOfDays = Days.daysBetween(loanRescheduleRequest.getRescheduleFromDate(), rescheduleToDate).getDays();

			LoanProduct loanProduct = loan.getLoanProduct();
			// creating new extension fee charge
			for (Charge charge : loanProduct.getLoanProductCharges()) {
				ChargeAdditionalDetails cad = this.chargeAdditionalDetailsRepository.getChargeAdditionalDetails(charge);

				if (charge != null && charge.isActive() && charge.isInstallmentRescheduled() && numberOfDays > 0) {

					LoanChargeAdditionalDetails loanChargeAdditionalDetail = new LoanChargeAdditionalDetails(null,
							cad.getIsEnabledFeeCalculationBasedOnTenure(), cad.getIsEnabledAutoPaid(),
							cad.getIsTaxIncluded(), rescheduleApprovedDate.toDate(), null, null, appUser, null);
					loanChargeAdditionalDetail.setNumberOfDays(numberOfDays);
					loanChargeAdditionalDetail.setTaxComponentType(taxComponentType);
					LoanCharge newLoanCharge;
					if (cad.getIsEnabledAutoPaid() == 1) {
						newLoanCharge = LoanCharge.createNewFromJson(null, loan, charge, loanChargeAdditionalDetail);
					} else {
						newLoanCharge = LoanCharge.createNewFromJson(loanRescheduleRequest.getRescheduleFromDate(),
								loan, charge, loanChargeAdditionalDetail);
					}

					// ChargeAdditionalDetails cad = chargeDefinition.getChargeAdditionalDetails();

					// newLoanCharge.setLoanChargeAdditionalDetails(loanChargeAdditionalDetail);
					addCharge(loan, charge, newLoanCharge, loanChargeAdditionalDetail);

					loanChargeAdditionalDetails.add(loanChargeAdditionalDetail);

					if (newLoanCharge.isActive() && newLoanCharge.isInstallmentRescheduled()
							&& loanChargeAdditionalDetail.getIsEnabledAutoPaid() == 1) {
						extensionFeeAmount = extensionFeeAmount
								.add(newLoanCharge.getAmount(loan.getCurrency()).getAmount());
					} else if (newLoanCharge.isActive() && newLoanCharge.isInstallmentRescheduled()) {
						extensionServiceFeeAmount = extensionServiceFeeAmount
								.add(newLoanCharge.getAmount(loan.getCurrency()).getAmount());
					}
				}
			}
			loan.setIsLoanExtension(true);
			/*
			 * Habile changes end
			 */
            final LoanScheduleDTO loanSchedule = loanScheduleGenerator.rescheduleNextInstallments(mathContext, loanApplicationTerms,
                    loan, loanApplicationTerms.getHolidayDetailDTO(),
                    loanRepaymentScheduleTransactionProcessor, rescheduleFromDate);

            loan.updateLoanSchedule(loanSchedule.getInstallments(), appUser);
            loan.recalculateAllCharges();
            
            loan.setLoanSubStatus(702);

			/** Habile changes for LPF - updating penalty installment in overdue charges */
			LocalDate lastChargeDate = null;
			Boolean penaltyCharges = false;
			for (LoanCharge loanCharge : loan.charges()) {
				/** Habile Changes **/
				if (loanCharge.isActive() && loanCharge.isPenaltyCharge()) {
					penaltyCharges = true;
					if (lastChargeDate == null) {
						lastChargeDate = loanCharge.getDueLocalDate();
					} else if (!lastChargeDate.isAfter(loanCharge.getDueLocalDate())) {
						lastChargeDate = loanCharge.getDueLocalDate();
					}
				}
			}
			if (penaltyCharges) {
				addInstallmentIfPenaltyAppliedAfterLastDueDate(loan, lastChargeDate);
				updateOverdueScheduleInstallment(loan);
			}
            ChangedTransactionDetail changedTransactionDetail =  loan.processTransactions();

            for (LoanRepaymentScheduleHistory loanRepaymentScheduleHistory : loanRepaymentScheduleHistoryList) {
                this.loanRepaymentScheduleHistoryRepository.save(loanRepaymentScheduleHistory);
            }

            loan.updateRescheduledByUser(appUser);
            loan.updateRescheduledOnDate(new LocalDate());

            // update the status of the request
            loanRescheduleRequest.approve(appUser, approvedOnDate);

            // update the loan object
            saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
            
            /*
			 * Habile changes
			 */
			loan.updateLoanSummaryDerivedFields();
			/*
			 * Habile changes end
			 */
            
            if (changedTransactionDetail != null) {
                for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                    this.loanTransactionRepository.save(mapEntry.getValue());
                    // update loan with references to the newly created
                    // transactions
                    loan.addLoanTransaction(mapEntry.getValue());
                    this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                }
            }
            postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds);
            
            /** Habile Changes **/
			this.loanAccountDomainService.recalculateAccruals(loan, true);

			/**
			 * Habile changes - running below mentioned jobs to the end of loan reschedule
			 */
            
            this.loanAccountDomainService.recalculateAccruals(loan, true);

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes).build();
        }

        catch (final DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    private void saveAndFlushLoanWithDataIntegrityViolationChecks(final Loan loan) {
        try {
            List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
            for (LoanRepaymentScheduleInstallment installment : installments) {
                if (installment.getId() == null) {
                    this.repaymentScheduleInstallmentRepository.save(installment);
                }
            }
            this.loanRepositoryWrapper.saveAndFlush(loan);
        } catch (final DataIntegrityViolationException e) {
            final Throwable realCause = e.getCause();
            final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
            final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan.transaction");
            if (realCause.getMessage().toLowerCase().contains("external_id_unique")) {
                baseDataValidator.reset().parameter("externalId").failWithCode("value.must.be.unique");
            }
            if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist",
                    "Validation errors exist.", dataValidationErrors); }
        }
    }

    private void postJournalEntries(Loan loan, List<Long> existingTransactionIds, List<Long> existingReversedTransactionIds) {
        final MonetaryCurrency currency = loan.getCurrency();
        final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
        boolean isAccountTransfer = false;
        final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
        this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData, null);
    }

    @Override
    @Transactional
    public CommandProcessingResult reject(JsonCommand jsonCommand) {

        try {
            final Long loanRescheduleRequestId = jsonCommand.entityId();

            final LoanRescheduleRequest loanRescheduleRequest = loanRescheduleRequestRepository.findOne(loanRescheduleRequestId);

            if (loanRescheduleRequest == null) { throw new LoanRescheduleRequestNotFoundException(loanRescheduleRequestId); }

            // validate the request in the JsonCommand object passed as
            // parameter
            this.loanRescheduleRequestDataValidator.validateForRejectAction(jsonCommand, loanRescheduleRequest);

            final AppUser appUser = this.platformSecurityContext.authenticatedUser();
            final Map<String, Object> changes = new LinkedHashMap<>();

            LocalDate rejectedOnDate = jsonCommand.localDateValueOfParameterNamed("rejectedOnDate");
            final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern(jsonCommand.dateFormat()).withLocale(
                    jsonCommand.extractLocale());

            changes.put("locale", jsonCommand.locale());
            changes.put("dateFormat", jsonCommand.dateFormat());
            changes.put("rejectedOnDate", rejectedOnDate.toString(dateTimeFormatter));
            changes.put("rejectedByUserId", appUser.getId());

            if (!changes.isEmpty()) {
                loanRescheduleRequest.reject(appUser, rejectedOnDate);
                Set<LoanRescheduleRequestToTermVariationMapping> loanRescheduleRequestToTermVariationMappings = loanRescheduleRequest
                        .getLoanRescheduleRequestToTermVariationMappings();
                for (LoanRescheduleRequestToTermVariationMapping loanRescheduleRequestToTermVariationMapping : loanRescheduleRequestToTermVariationMappings) {
                    loanRescheduleRequestToTermVariationMapping.getLoanTermVariations().markAsInactive();
                }
            }

            return new CommandProcessingResultBuilder().withCommandId(jsonCommand.commandId()).withEntityId(loanRescheduleRequestId)
                    .withLoanId(loanRescheduleRequest.getLoan().getId()).with(changes).build();
        }

        catch (final DataIntegrityViolationException dve) {
            // handle the data integrity violation
            handleDataIntegrityViolation(dve);

            // return an empty command processing result object
            return CommandProcessingResult.empty();
        }
    }

    /**
     * handles the data integrity violation exception for loan reschedule write
     * services
     *
     * @param dve
     *            data integrity violation exception
     * 
     **/
    private void handleDataIntegrityViolation(final DataIntegrityViolationException dve) {

        logger.error(dve.getMessage(), dve);

        throw new PlatformDataIntegrityException("error.msg.loan.reschedule.unknown.data.integrity.issue",
                "Unknown data integrity issue with resource.");
    }
    /** Habile changes Added new fees (extension fees) */
	public boolean addCharge(final Loan loan, final Charge chargeDefinition, final LoanCharge loanCharge,
			final LoanChargeAdditionalDetails loanChargeAdditionalDetails) {

		AppUser currentUser = this.platformSecurityContext.authenticatedUser();
		if (!loan.hasCurrencyCodeOf(chargeDefinition.getCurrencyCode())) {
			final String errorMessage = "Charge and Loan must have the same currency.";
			throw new InvalidCurrencyException("loanCharge", "attach.to.loan", errorMessage);
		}

		loan.addLoanCharge(loanCharge);

		// loanCharge.setLoanChargeAdditionalDetails(null);

		this.loanChargeRepository.save(loanCharge);

		// this.loanChargeAdditionalDetailsRepository.save(loanChargeAdditionalDetails);

		/**
		 * we want to apply charge transactions only for those loans charges that are
		 * applied when a loan is active and the loan product uses Upfront Accruals
		 **/
		if (loan.status().isActive() && loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
			if (loanChargeAdditionalDetails.getIsEnabledAutoPaid() == 1) {
				loan.handleInstallmentRescheduleTransaction(rescheduleApprovedDate, currentUser, loanCharge, null,
						Money.of(loan.getCurrency(), loanCharge.amount()));
			} /*
				 * else { final LoanTransaction applyLoanChargeTransaction =
				 * loan.handleChargeAppliedTransaction(loanCharge, null, currentUser);
				 * this.loanTransactionRepository.save(applyLoanChargeTransaction); }
				 */
		}
		boolean isAppliedOnBackDate = false;
		if (loanCharge.getDueLocalDate() == null
				|| DateUtils.getLocalDateOfTenant().isAfter(loanCharge.getDueLocalDate())) {
			isAppliedOnBackDate = true;
		}
		return isAppliedOnBackDate;
	}

	private void addInstallmentIfPenaltyAppliedAfterLastDueDate(Loan loan, LocalDate lastChargeDate) {
		if (lastChargeDate != null) {
			List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
			LoanRepaymentScheduleInstallment lastInstallment = loan
					.fetchRepaymentScheduleInstallment(installments.size());
			if (lastChargeDate.isAfter(lastInstallment.getDueDate())) {
				if (lastInstallment.isRecalculatedInterestComponent()) {
					installments.remove(lastInstallment);
					lastInstallment = loan.fetchRepaymentScheduleInstallment(installments.size());
				}
				boolean recalculatedInterestComponent = true;
				BigDecimal principal = BigDecimal.ZERO;
				BigDecimal interest = BigDecimal.ZERO;
				BigDecimal feeCharges = BigDecimal.ZERO;
				BigDecimal penaltyCharges = BigDecimal.ONE;
				final Set<LoanInterestRecalcualtionAdditionalDetails> compoundingDetails = null;
				LoanRepaymentScheduleInstallment newEntry = new LoanRepaymentScheduleInstallment(loan,
						installments.size() + 1, lastInstallment.getDueDate(), lastChargeDate, principal, interest,
						feeCharges, penaltyCharges, recalculatedInterestComponent, compoundingDetails);
				loan.addLoanRepaymentScheduleInstallment(newEntry);
			}
		}
	}

	private void updateOverdueScheduleInstallment(final Loan loan) {
		for (LoanCharge charge : loan.charges()) {
			charge.setApplied(false);
		}
		for (LoanRepaymentScheduleInstallment installment : loan.getRepaymentScheduleInstallments()) {
			for (LoanCharge loanCharge : loan.charges()) {
				if (loanCharge.isOverdueInstallmentCharge() && loanCharge.isActive()) {
					LoanOverdueInstallmentCharge overdueInstallmentCharge = loanCharge.getOverdueInstallmentCharge();
					if (overdueInstallmentCharge != null) {
						LoanRepaymentScheduleInstallment overdueInstallment = overdueInstallmentCharge.getInstallment();
						// LoanRepaymentScheduleInstallment installment =
						// fetchRepaymentScheduleInstallment(installmentNumber);
						if (!loan.getRepaymentScheduleInstallments().contains(overdueInstallment)) {
							if (!loanCharge.getApplied() && loanCharge.isDueForCollectionFromAndUpToAndIncluding(
									installment.getFromDate(), installment.getDueDate())) {
								/*
								 * if (loanCharge.getDueLocalDate().equals(installment.getFromDate()) ||
								 * loanCharge.getDueLocalDate().equals(installment.getDueDate()) ||
								 * loanCharge.getDueLocalDate().isBefore(installment.getDueDate()) ||
								 * loanCharge.getDueLocalDate().isAfter(installment.getFromDate())) {
								 */
								overdueInstallmentCharge.updateLoanRepaymentScheduleInstallment(installment);
								loanCharge.setApplied(true);
							}
						}
					}
				}
			}
		}
	}

	public static LoanChargeAdditionalDetailsRepository getLoanChargeAdditionalDetailsRepositoryBean() {
		return loanChargeAdditionalDetailsRepositoryBean;
	}

	public static void setLoanChargeAdditionalDetailsRepositoryBean(
			LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepositoryBean) {
		LoanRescheduleRequestWritePlatformServiceImpl.loanChargeAdditionalDetailsRepositoryBean = loanChargeAdditionalDetailsRepositoryBean;
	}
	// Habile changes end
}
