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
package org.apache.fineract.portfolio.loanaccount.service;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.fineract.accounting.common.AccountingConstants.ACCRUAL_ACCOUNTS_FOR_LOAN;
import org.apache.fineract.accounting.common.AccountingConstants.CASH_ACCOUNTS_FOR_LOAN;
import org.apache.fineract.accounting.glaccount.domain.GLAccount;
import org.apache.fineract.accounting.glaccount.domain.GLAccountRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntry;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryRepository;
import org.apache.fineract.accounting.journalentry.domain.JournalEntryType;
import org.apache.fineract.accounting.journalentry.service.AccountingProcessorHelper;
import org.apache.fineract.accounting.journalentry.service.JournalEntryReadPlatformService;
import org.apache.fineract.accounting.journalentry.service.JournalEntryWritePlatformService;
import org.apache.fineract.accounting.producttoaccountmapping.domain.PortfolioProductType;
import org.apache.fineract.infrastructure.codes.domain.CodeValue;
import org.apache.fineract.infrastructure.codes.domain.CodeValueRepositoryWrapper;
import org.apache.fineract.infrastructure.configuration.domain.ConfigurationDomainService;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.GeneralPlatformDomainRuleException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.exception.PlatformServiceUnavailableException;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.dataqueries.data.EntityTables;
import org.apache.fineract.infrastructure.dataqueries.data.StatusEnum;
import org.apache.fineract.infrastructure.dataqueries.service.EntityDatatableChecksWritePlatformService;
import org.apache.fineract.infrastructure.jobs.annotation.CronTarget;
import org.apache.fineract.infrastructure.jobs.exception.JobExecutionException;
import org.apache.fineract.infrastructure.jobs.service.JobName;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.organisation.holiday.domain.Holiday;
import org.apache.fineract.organisation.holiday.domain.HolidayRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrency;
import org.apache.fineract.organisation.monetary.domain.ApplicationCurrencyRepositoryWrapper;
import org.apache.fineract.organisation.monetary.domain.MonetaryCurrency;
import org.apache.fineract.organisation.monetary.domain.Money;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.organisation.staff.domain.Staff;
import org.apache.fineract.organisation.teller.data.CashierTransactionDataValidator;
import org.apache.fineract.organisation.workingdays.domain.WorkingDays;
import org.apache.fineract.organisation.workingdays.domain.WorkingDaysRepositoryWrapper;
import org.apache.fineract.portfolio.account.PortfolioAccountType;
import org.apache.fineract.portfolio.account.data.AccountTransferDTO;
import org.apache.fineract.portfolio.account.data.PortfolioAccountData;
import org.apache.fineract.portfolio.account.domain.*;
import org.apache.fineract.portfolio.account.service.AccountAssociationsReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersReadPlatformService;
import org.apache.fineract.portfolio.account.service.AccountTransfersWritePlatformService;
import org.apache.fineract.portfolio.accountdetails.domain.AccountType;
import org.apache.fineract.portfolio.address.data.AddressData;
import org.apache.fineract.portfolio.address.service.AddressReadPlatformServiceImpl;
import org.apache.fineract.portfolio.calendar.domain.*;
import org.apache.fineract.portfolio.calendar.domain.Calendar;
import org.apache.fineract.portfolio.calendar.exception.CalendarParameterUpdateNotSupportedException;
import org.apache.fineract.portfolio.charge.domain.Charge;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetails;
import org.apache.fineract.portfolio.charge.domain.ChargeAdditionalDetailsRepository;
import org.apache.fineract.portfolio.charge.domain.ChargePaymentMode;
import org.apache.fineract.portfolio.charge.domain.ChargeRepositoryWrapper;
import org.apache.fineract.portfolio.charge.exception.*;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBeDeletedException.LOAN_CHARGE_CANNOT_BE_DELETED_REASON;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBePayedException.LOAN_CHARGE_CANNOT_BE_PAYED_REASON;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBeUpdatedException.LOAN_CHARGE_CANNOT_BE_UPDATED_REASON;
import org.apache.fineract.portfolio.charge.exception.LoanChargeCannotBeWaivedException.LOAN_CHARGE_CANNOT_BE_WAIVED_REASON;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.client.exception.ClientNotActiveException;
import org.apache.fineract.portfolio.collectionsheet.command.CollectionSheetBulkDisbursalCommand;
import org.apache.fineract.portfolio.collectionsheet.command.CollectionSheetBulkRepaymentCommand;
import org.apache.fineract.portfolio.collectionsheet.command.SingleDisbursalCommand;
import org.apache.fineract.portfolio.collectionsheet.command.SingleRepaymentCommand;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_ENTITY;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants.BUSINESS_EVENTS;
import org.apache.fineract.portfolio.common.domain.PeriodFrequencyType;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.exception.GroupNotActiveException;
import org.apache.fineract.portfolio.loanaccount.api.LoanApiConstants;
import org.apache.fineract.portfolio.loanaccount.command.LoanUpdateCommand;
import org.apache.fineract.portfolio.loanaccount.data.*;
import org.apache.fineract.portfolio.loanaccount.domain.*;
import org.apache.fineract.portfolio.loanaccount.exception.*;
import org.apache.fineract.portfolio.loanaccount.guarantor.service.GuarantorDomainService;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.OverdueLoanScheduleData;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.DefaultScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModel;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.LoanScheduleModelPeriod;
import org.apache.fineract.portfolio.loanaccount.loanschedule.domain.ScheduledDateGenerator;
import org.apache.fineract.portfolio.loanaccount.loanschedule.service.LoanScheduleHistoryWritePlatformService;
import org.apache.fineract.portfolio.loanaccount.rescheduleloan.domain.LoanRescheduleRequest;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanApplicationCommandFromApiJsonHelper;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanEventApiJsonValidator;
import org.apache.fineract.portfolio.loanaccount.serialization.LoanUpdateCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.loanproduct.data.LoanOverdueDTO;
import org.apache.fineract.portfolio.loanproduct.data.LoanProductData;
import org.apache.fineract.portfolio.loanproduct.domain.LoanProduct;
import org.apache.fineract.portfolio.loanproduct.exception.InvalidCurrencyException;
import org.apache.fineract.portfolio.loanproduct.exception.LinkedAccountRequiredException;
import org.apache.fineract.portfolio.loanproduct.service.LoanProductReadPlatformService;
import org.apache.fineract.portfolio.note.domain.Note;
import org.apache.fineract.portfolio.note.domain.NoteRepository;
import org.apache.fineract.portfolio.paymentdetail.domain.PaymentDetail;
import org.apache.fineract.portfolio.paymentdetail.service.PaymentDetailWritePlatformService;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.exception.InsufficientAccountBalanceException;
import org.apache.fineract.useradministration.domain.AppUser;
import org.finabile.fineract.portfolio.code.data.FinabileCodeValueData;
import org.finabile.fineract.portfolio.code.service.FinabileCodeValueReadPlatformService;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.mifosplatform.infrastructure.sms.service.SmsProcessingService;
import org.mifosplatform.infrastructure.sms.vo.SMSDataVO;
import org.mifosplatform.infrastructure.utils.CommonMethodsUtil;
import org.apache.fineract.portfolio.loanaccount.loanschedule.data.LoanSchedulePeriodData;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

@Service
public class LoanWritePlatformServiceJpaRepositoryImpl implements LoanWritePlatformService {

    private final static Logger logger = LoggerFactory.getLogger(LoanWritePlatformServiceJpaRepositoryImpl.class);

    private final PlatformSecurityContext context;
    private final LoanEventApiJsonValidator loanEventApiJsonValidator;
    private final LoanUpdateCommandFromApiJsonDeserializer loanUpdateCommandFromApiJsonDeserializer;
    private final LoanRepositoryWrapper loanRepositoryWrapper ;
    private final LoanAccountDomainService loanAccountDomainService;
    private final NoteRepository noteRepository;
    private final LoanTransactionRepository loanTransactionRepository;
    private final LoanAssembler loanAssembler;
    private final ChargeRepositoryWrapper chargeRepository;
    private final LoanChargeRepository loanChargeRepository;
    private final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository;
    private final JournalEntryWritePlatformService journalEntryWritePlatformService;
    private final CalendarInstanceRepository calendarInstanceRepository;
    private final PaymentDetailWritePlatformService paymentDetailWritePlatformService;
    private final HolidayRepositoryWrapper holidayRepository;
    private final ConfigurationDomainService configurationDomainService;
    private final WorkingDaysRepositoryWrapper workingDaysRepository;
    private final LoanProductReadPlatformService loanProductReadPlatformService;
    private final AccountTransfersWritePlatformService accountTransfersWritePlatformService;
    private final AccountTransfersReadPlatformService accountTransfersReadPlatformService;
    private final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService;
    private final LoanChargeReadPlatformService loanChargeReadPlatformService;
    private final LoanReadPlatformService loanReadPlatformService;
    private final FromJsonHelper fromApiJsonHelper;
    private final AccountTransferRepository accountTransferRepository;
    private final CalendarRepository calendarRepository;
    private final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository;
    private final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService;
    private final LoanApplicationCommandFromApiJsonHelper loanApplicationCommandFromApiJsonHelper;
    private final AccountAssociationsRepository accountAssociationRepository;
    private final AccountTransferDetailRepository accountTransferDetailRepository;
    private final BusinessEventNotifierService businessEventNotifierService;
    private final GuarantorDomainService guarantorDomainService;
    private final LoanUtilService loanUtilService;
    private final LoanSummaryWrapper loanSummaryWrapper;
    private final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService;
    private final LoanRepaymentScheduleTransactionProcessorFactory transactionProcessingStrategy;
    private final CodeValueRepositoryWrapper codeValueRepository;
    private final CashierTransactionDataValidator cashierTransactionDataValidator;
    private final SmsProcessingService smsProcessingService;
    private final LoanAccrualWritePlatformService loanAccrualWritePlatformService;
    private final AccountingProcessorHelper helper;
    private final JdbcTemplate jdbcTemplate;
    private final DataSource dataSource;
    private final GLAccountRepository glAccountRepository;
    private final LoanManualChargeRepository loanManualChargeRepository;
    private final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalEntryReadPlatformService journalEntryReadPlatformService;
    private final ClientRepositoryWrapper clientRepositoryWrapper;
    private final AddressReadPlatformServiceImpl addressReadPlatformServiceImpl;
    private final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService;
    /** Habile changes start */
	private final LoanSchedularService loanSchedularService;
	private final LoanAccrualPlatformService loanAccrualPlatformService;
	private final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository;
	

	/** Habile changes end */
    
    public static int prepay = 0;//0 denotes its not prepayment 1 denotes prepayment which will be set the value in another class in case prepayment
    

    @Autowired
    public LoanWritePlatformServiceJpaRepositoryImpl(final PlatformSecurityContext context,
            final LoanEventApiJsonValidator loanEventApiJsonValidator,
            final LoanUpdateCommandFromApiJsonDeserializer loanUpdateCommandFromApiJsonDeserializer, final LoanAssembler loanAssembler,
            final LoanAccountDomainService loanAccountDomainService,
            final LoanTransactionRepository loanTransactionRepository, final NoteRepository noteRepository,
            final ChargeRepositoryWrapper chargeRepository, final LoanChargeRepository loanChargeRepository,
            final ApplicationCurrencyRepositoryWrapper applicationCurrencyRepository,
            final JournalEntryWritePlatformService journalEntryWritePlatformService,
            final CalendarInstanceRepository calendarInstanceRepository,
            final PaymentDetailWritePlatformService paymentDetailWritePlatformService, final HolidayRepositoryWrapper holidayRepository,
            final ConfigurationDomainService configurationDomainService, final WorkingDaysRepositoryWrapper workingDaysRepository,
            final LoanProductReadPlatformService loanProductReadPlatformService,
            final AccountTransfersWritePlatformService accountTransfersWritePlatformService,
            final AccountTransfersReadPlatformService accountTransfersReadPlatformService,
            final AccountAssociationsReadPlatformService accountAssociationsReadPlatformService,
            final LoanChargeReadPlatformService loanChargeReadPlatformService, final LoanReadPlatformService loanReadPlatformService,
            final FromJsonHelper fromApiJsonHelper, final AccountTransferRepository accountTransferRepository,
            final CalendarRepository calendarRepository,
            final LoanRepaymentScheduleInstallmentRepository repaymentScheduleInstallmentRepository,
            final LoanScheduleHistoryWritePlatformService loanScheduleHistoryWritePlatformService,
            final LoanApplicationCommandFromApiJsonHelper loanApplicationCommandFromApiJsonHelper,
            final AccountAssociationsRepository accountAssociationRepository,
            final AccountTransferDetailRepository accountTransferDetailRepository,
            final BusinessEventNotifierService businessEventNotifierService, final GuarantorDomainService guarantorDomainService,
            final LoanUtilService loanUtilService, final LoanSummaryWrapper loanSummaryWrapper,
            final EntityDatatableChecksWritePlatformService entityDatatableChecksWritePlatformService,
            final LoanRepaymentScheduleTransactionProcessorFactory transactionProcessingStrategy,
            final CodeValueRepositoryWrapper codeValueRepository,
            final LoanRepositoryWrapper loanRepositoryWrapper,
            final CashierTransactionDataValidator cashierTransactionDataValidator,final SmsProcessingService smsProcessingService,
            final LoanAccrualWritePlatformService loanAccrualWritePlatformService,
            final AccountingProcessorHelper accountingProcessorHelper, final RoutingDataSource dataSource,
            final GLAccountRepository glAccountRepository, final LoanManualChargeRepository loanManualChargeRepository,
            final JournalEntryRepository journalEntryRepository,
            final JournalEntryReadPlatformService journalEntryReadPlatformService,
            final ClientRepositoryWrapper clientRepositoryWrapper,final AddressReadPlatformServiceImpl addressReadPlatformServiceImpl,final FinabileCodeValueReadPlatformService finabileCodeValueReadPlatformService,
			@Lazy final LoanSchedularService loanSchedularService,
			final LoanAccrualPlatformService loanAccrualPlatformService,
			final LoanChargeAdditionalDetailsRepository loanChargeAdditionalDetailsRepository,final ChargeAdditionalDetailsRepository chargeAdditionalDetailsRepository
			) {
        this.context = context;
        this.loanEventApiJsonValidator = loanEventApiJsonValidator;
        this.loanAssembler = loanAssembler;
        this.loanRepositoryWrapper = loanRepositoryWrapper ;
        this.loanAccountDomainService = loanAccountDomainService;
        this.loanTransactionRepository = loanTransactionRepository;
        this.noteRepository = noteRepository;
        this.chargeRepository = chargeRepository;
        this.loanChargeRepository = loanChargeRepository;
        this.applicationCurrencyRepository = applicationCurrencyRepository;
        this.journalEntryWritePlatformService = journalEntryWritePlatformService;
        this.loanUpdateCommandFromApiJsonDeserializer = loanUpdateCommandFromApiJsonDeserializer;
        this.calendarInstanceRepository = calendarInstanceRepository;
        this.paymentDetailWritePlatformService = paymentDetailWritePlatformService;
        this.holidayRepository = holidayRepository;
        this.configurationDomainService = configurationDomainService;
        this.workingDaysRepository = workingDaysRepository;
        this.loanProductReadPlatformService = loanProductReadPlatformService;
        this.accountTransfersWritePlatformService = accountTransfersWritePlatformService;
        this.accountTransfersReadPlatformService = accountTransfersReadPlatformService;
        this.accountAssociationsReadPlatformService = accountAssociationsReadPlatformService;
        this.loanChargeReadPlatformService = loanChargeReadPlatformService;
        this.loanReadPlatformService = loanReadPlatformService;
        this.fromApiJsonHelper = fromApiJsonHelper;
        this.accountTransferRepository = accountTransferRepository;
        this.calendarRepository = calendarRepository;
        this.repaymentScheduleInstallmentRepository = repaymentScheduleInstallmentRepository;
        this.loanScheduleHistoryWritePlatformService = loanScheduleHistoryWritePlatformService;
        this.loanApplicationCommandFromApiJsonHelper = loanApplicationCommandFromApiJsonHelper;
        this.accountAssociationRepository = accountAssociationRepository;
        this.accountTransferDetailRepository = accountTransferDetailRepository;
        this.businessEventNotifierService = businessEventNotifierService;
        this.guarantorDomainService = guarantorDomainService;
        this.loanUtilService = loanUtilService;
        this.loanSummaryWrapper = loanSummaryWrapper;
        this.transactionProcessingStrategy = transactionProcessingStrategy;
        this.entityDatatableChecksWritePlatformService = entityDatatableChecksWritePlatformService;
        this.codeValueRepository = codeValueRepository;
        this.cashierTransactionDataValidator = cashierTransactionDataValidator;
        this.smsProcessingService = smsProcessingService;
        this.loanAccrualWritePlatformService = loanAccrualWritePlatformService;
        this.helper = accountingProcessorHelper;
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
        this.glAccountRepository = glAccountRepository;
        this.loanManualChargeRepository = loanManualChargeRepository;
        
        this.journalEntryRepository = journalEntryRepository;
        this.journalEntryReadPlatformService = journalEntryReadPlatformService;
        this.clientRepositoryWrapper=clientRepositoryWrapper;
        this.addressReadPlatformServiceImpl=addressReadPlatformServiceImpl;
        this.finabileCodeValueReadPlatformService=finabileCodeValueReadPlatformService;
        this.loanSchedularService = loanSchedularService;
		this.loanAccrualPlatformService = loanAccrualPlatformService;
		this.loanChargeAdditionalDetailsRepository = loanChargeAdditionalDetailsRepository;
		this.chargeAdditionalDetailsRepository=chargeAdditionalDetailsRepository;
		
    }

    private LoanLifecycleStateMachine defaultLoanLifecycleStateMachine() {
        final List<LoanStatus> allowedLoanStatuses = Arrays.asList(LoanStatus.values());
        return new DefaultLoanLifecycleStateMachine(allowedLoanStatuses);
    }

    @SuppressWarnings("unchecked")
   	@Transactional
       @Override
       public CommandProcessingResult disburseLoan(final Long loanId, final JsonCommand command, Boolean isAccountTransfer) {

           final AppUser currentUser = getAppUserIfPresent();

           this.loanEventApiJsonValidator.validateDisbursement(command.json(), isAccountTransfer);

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           
           final LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
           
           // validate ActualDisbursement Date Against Expected Disbursement Date
           LoanProduct loanProduct = loan.loanProduct();
           if(loanProduct.syncExpectedWithDisbursementDate()){
           	syncExpectedDateWithActualDisbursementDate(loan, actualDisbursementDate);
           }
           checkClientOrGroupActive(loan);

           final LocalDate nextPossibleRepaymentDate = loan.getNextPossibleRepaymentDateForRescheduling();
           final Date rescheduledRepaymentDate = command.DateValueOfParameterNamed("adjustRepaymentDate");

           entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                   StatusEnum.DISBURSE.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

           // check for product mix validations
           checkForProductMixRestrictions(loan);
           
           LocalDate recalculateFrom = null;
           if(!loan.isMultiDisburmentLoan()){
           	loan.setActualDisbursementDate(actualDisbursementDate.toDate());
           }        
           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           // validate actual disbursement date against meeting date
           final CalendarInstance calendarInstance = this.calendarInstanceRepository.findCalendarInstaneByEntityId(loan.getId(),
                   CalendarEntityType.LOANS.getValue());
           if (loan.isSyncDisbursementWithMeeting()) {
               this.loanEventApiJsonValidator.validateDisbursementDateWithMeetingDate(actualDisbursementDate, calendarInstance, 
                       scheduleGeneratorDTO.isSkipRepaymentOnFirstDayofMonth(), scheduleGeneratorDTO.getNumberOfdays());
           }

           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_DISBURSAL,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           final Map<String, Object> changes = new LinkedHashMap<>();

           final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
   		if (paymentDetail != null && paymentDetail.getPaymentType() != null
   				&& paymentDetail.getPaymentType().isCashPayment()) {
   			BigDecimal transactionAmount = command
   					.bigDecimalValueOfParameterNamed("transactionAmount");
   			this.cashierTransactionDataValidator.validateOnLoanDisbursal(
   					currentUser, loan.getCurrencyCode(), transactionAmount);
   		}     
           final Boolean isPaymnetypeApplicableforDisbursementCharge = configurationDomainService
                   .isPaymnetypeApplicableforDisbursementCharge();

           // Recalculate first repayment date based in actual disbursement date.
           updateLoanCounters(loan, actualDisbursementDate);
           Money amountBeforeAdjust = loan.getPrincpal();
           loan.validateAccountStatus(LoanEvent.LOAN_DISBURSED);
           boolean canDisburse = loan.canDisburse(actualDisbursementDate);
           ChangedTransactionDetail changedTransactionDetail = null;
           Money amountDisbursed = null;
           if (canDisburse) {
               Money disburseAmount = loan.adjustDisburseAmount(command, actualDisbursementDate);
               Money amountToDisburse = disburseAmount.copy();
               amountDisbursed = disburseAmount.copy();
               boolean recalculateSchedule = amountBeforeAdjust.isNotEqualTo(loan.getPrincpal());
               final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");

               if(loan.isTopup() && loan.getClientId() != null){
                   final Long loanIdToClose = loan.getTopupLoanDetails().getLoanIdToClose();
                   final Loan loanToClose = this.loanRepositoryWrapper.findNonClosedLoanThatBelongsToClient(loanIdToClose, loan.getClientId());
                   if(loanToClose == null){
                       throw new GeneralPlatformDomainRuleException("error.msg.loan.to.be.closed.with.topup.is.not.active",
                               "Loan to be closed with this topup is not active.");
                   }
                   final LocalDate lastUserTransactionOnLoanToClose = loanToClose.getLastUserTransactionDate();
                   if(loan.getDisbursementDate().isBefore(lastUserTransactionOnLoanToClose)){
                       throw new GeneralPlatformDomainRuleException(
                               "error.msg.loan.disbursal.date.should.be.after.last.transaction.date.of.loan.to.be.closed",
                               "Disbursal date of this loan application "+loan.getDisbursementDate()
                                       +" should be after last transaction date of loan to be closed "+ lastUserTransactionOnLoanToClose);
                   }

                   BigDecimal loanOutstanding = this.loanReadPlatformService.retrieveLoanPrePaymentTemplate(loanIdToClose,
                               actualDisbursementDate).getAmount();
                   final BigDecimal firstDisbursalAmount = loan.getFirstDisbursalAmount();
                   if(loanOutstanding.compareTo(firstDisbursalAmount) > 0){
                       throw new GeneralPlatformDomainRuleException("error.msg.loan.amount.less.than.outstanding.of.loan.to.be.closed",
                               "Topup loan amount should be greater than outstanding amount of loan to be closed.");
                   }

                   amountToDisburse = disburseAmount.minus(loanOutstanding);

                   disburseLoanToLoan(loan, command, loanOutstanding);
               }

               if (isAccountTransfer) {
                   disburseLoanToSavings(loan, command, amountToDisburse, paymentDetail);
                   existingTransactionIds.addAll(loan.findExistingTransactionIds());
                   existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
               } else {
                   existingTransactionIds.addAll(loan.findExistingTransactionIds());
                   existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
                   LoanTransaction disbursementTransaction = LoanTransaction.disbursement(loan.getOffice(), amountToDisburse, paymentDetail,
                           actualDisbursementDate, txnExternalId, DateUtils.getLocalDateTimeOfTenant(), currentUser);
                   disbursementTransaction.updateLoan(loan);
                   loan.addLoanTransaction(disbursementTransaction);
               }

               regenerateScheduleOnDisbursement(command, loan, recalculateSchedule, scheduleGeneratorDTO, nextPossibleRepaymentDate, rescheduledRepaymentDate);
               if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               	createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);
               }
               if (isPaymnetypeApplicableforDisbursementCharge) {
                   changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, paymentDetail);
               } else {
                   changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, null);
               }
               
               
           }
           if (!changes.isEmpty()) {
               saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

               final String noteText = command.stringValueOfParameterNamed("note");
               if (StringUtils.isNotBlank(noteText)) {
                   final Note note = Note.loanNote(loan, noteText);
                   this.noteRepository.save(note);
               }

               if (changedTransactionDetail != null) {
                   for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                       this.loanTransactionRepository.save(mapEntry.getValue());
                       this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                   }
               }

               // auto create standing instruction
               createStandingInstruction(loan);
                String desc="";
               if(loan.getClient() != null) {
               	desc = "for the loan " + loan.getAccountNumber() +" of the client " + loan.getClient().getDisplayName();
               }
               postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, desc);

           }

           final Set<LoanCharge> loanCharges = loan.charges();
           final Map<Long, BigDecimal> disBuLoanCharges = new HashMap<>();
           for (final LoanCharge loanCharge : loanCharges) {
               if (loanCharge.isDueAtDisbursement() && loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()
                       && loanCharge.isChargePending()) {
                   disBuLoanCharges.put(loanCharge.getId(), loanCharge.amountOutstanding());
               }
           }

           final Locale locale = command.extractLocale();
           final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
           for (final Map.Entry<Long, BigDecimal> entrySet : disBuLoanCharges.entrySet()) {
               final PortfolioAccountData savingAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
               final SavingsAccount fromSavingsAccount = null;
               final boolean isRegularTransaction = true;
               final boolean isExceptionForBalanceCheck = false;
               final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(actualDisbursementDate, entrySet.getValue(),
                       PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN, savingAccountData.accountId(), loanId, "Loan Charge Payment",
                       locale, fmt, null, null, LoanTransactionType.REPAYMENT_AT_DISBURSEMENT.getValue(), entrySet.getKey(), null,
                       AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, null, null, null, fromSavingsAccount, isRegularTransaction,
                       isExceptionForBalanceCheck);
               this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
           }
           
           updateRecurringCalendarDatesForInterestRecalculation(loan);
           this.loanAccountDomainService.recalculateAccruals(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_DISBURSAL,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           
           
         //Code to send SMS Starts
           if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(amountDisbursed)) {

           
           	 LoanScheduleData repaymentSchedule = null;
                
                LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loanId);
         
                repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loanId, loanBasicDetails.repaymentScheduleRelatedData(),
                		this.loanReadPlatformService.retrieveLoanDisbursementDetails(loanId), loanBasicDetails.isInterestRecalculationEnabled(), loanBasicDetails.getTotalPaidFeeCharges());	
                List<LoanSchedulePeriodData> repaymentPeriods = new ArrayList<LoanSchedulePeriodData> (repaymentSchedule.getPeriods()) ;
                if(CommonMethodsUtil.isNotBlank(repaymentPeriods)&& repaymentPeriods.size()>1) {
                BigDecimal  installmentAmount =  ((LoanSchedulePeriodData) repaymentPeriods.get(1)).getTotalDueForPeriod();
                
                String templateParamString ="templateParameters[A]="+amountDisbursed.toString()+"&&templateParameters[B]="+loan.getAccountNumber();
//                String templateParamString = "templateParameters[A]="+amountDisbursed.toString()+"&templateParameters[B]="+actualDisbursementDate.toString("dd/MM/yyyy");
           SMSDataVO smsdata = new SMSDataVO("disbursement", loan.client().mobileNo(), templateParamString);
           this.smsProcessingService.sendSMS(smsdata);
                }
           }
           //Code to send SMS Ends
           
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loan.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

   	private void createAndSaveLoanScheduleArchive(final Loan loan, ScheduleGeneratorDTO scheduleGeneratorDTO) {
   		LoanRescheduleRequest loanRescheduleRequest = null;
   		LoanScheduleModel loanScheduleModel = loan.regenerateScheduleModel(scheduleGeneratorDTO);
   		List<LoanRepaymentScheduleInstallment> installments = retrieveRepaymentScheduleFromModel(loanScheduleModel);
   		this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(installments,
   		        loan, loanRescheduleRequest);
   	}

       /**
        * create standing instruction for disbursed loan
        * 
        * @param loan
        *            the disbursed loan
        * 
        **/
       private void createStandingInstruction(Loan loan) {

           if (loan.shouldCreateStandingInstructionAtDisbursement()) {
               AccountAssociations accountAssociations = this.accountAssociationRepository.findByLoanIdAndType(loan.getId(),
                       AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());

               if (accountAssociations != null) {

                   SavingsAccount linkedSavingsAccount = accountAssociations.linkedSavingsAccount();

                   // name is auto-generated
                   final String name = "To loan " + loan.getAccountNumber() + " from savings " + linkedSavingsAccount.getAccountNumber();
                   final Office fromOffice = loan.getOffice();
                   final Client fromClient = loan.getClient();
                   final Office toOffice = loan.getOffice();
                   final Client toClient = loan.getClient();
                   final Integer priority = StandingInstructionPriority.MEDIUM.getValue();
                   final Integer transferType = AccountTransferType.LOAN_REPAYMENT.getValue();
                   final Integer instructionType = StandingInstructionType.DUES.getValue();
                   final Integer status = StandingInstructionStatus.ACTIVE.getValue();
                   final Integer recurrenceType = AccountTransferRecurrenceType.AS_PER_DUES.getValue();
                   final LocalDate validFrom = new LocalDate();

                   AccountTransferDetails accountTransferDetails = AccountTransferDetails.savingsToLoanTransfer(fromOffice, fromClient,
                           linkedSavingsAccount, toOffice, toClient, loan, transferType);

                   AccountTransferStandingInstruction accountTransferStandingInstruction = AccountTransferStandingInstruction.create(
                           accountTransferDetails, name, priority, instructionType, status, null, validFrom, null, recurrenceType, null, null,
                           null);
                   accountTransferDetails.updateAccountTransferStandingInstruction(accountTransferStandingInstruction);

                   this.accountTransferDetailRepository.save(accountTransferDetails);
               }
           }
       }

       private void updateRecurringCalendarDatesForInterestRecalculation(final Loan loan) {

           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()
                   && loan.loanInterestRecalculationDetails().getRestFrequencyType().isSameAsRepayment()) {
               final CalendarInstance calendarInstanceForInterestRecalculation = this.calendarInstanceRepository
                       .findByEntityIdAndEntityTypeIdAndCalendarTypeId(loan.loanInterestRecalculationDetailId(),
                               CalendarEntityType.LOAN_RECALCULATION_REST_DETAIL.getValue(), CalendarType.COLLECTION.getValue());

            Calendar calendarForInterestRecalculation = calendarInstanceForInterestRecalculation.getCalendar();
            calendarForInterestRecalculation.updateStartAndEndDate(loan.getDisbursementDate(), loan.getMaturityDate());
            
            //
//            final CalendarInstance calendarInstanceForInterestRecalculation1 = this.calendarInstanceRepository
//                    .findByEntityIdAndEntityTypeIdAndCalendarTypeId(loan.loanInterestRecalculationDetailId(),
//                            CalendarEntityType.LOAN_RECALCULATION_COMPOUNDING_DETAIL.getValue(), CalendarType.COLLECTION.getValue());
//
//            Calendar calendarForInterestRecalculation1 = calendarInstanceForInterestRecalculation1.getCalendar();
//            calendarForInterestRecalculation1.updateStart(loan.getDisbursementDate());
            //
            this.calendarRepository.save(calendarForInterestRecalculation);
//            this.calendarRepository.save(calendarForInterestRecalculation1);//balaji
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

       private void saveLoanWithDataIntegrityViolationChecks(final Loan loan) {
           try {
               List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
               for (LoanRepaymentScheduleInstallment installment : installments) {
                   if (installment.getId() == null) {
                       this.repaymentScheduleInstallmentRepository.save(installment);
                   }
               }
               this.loanRepositoryWrapper.save(loan);
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

       /****
        * TODO Vishwas: Pair with Ashok and re-factor collection sheet code-base
        * 
        * May of the changes made to disburseLoan aren't being made here, should
        * refactor to reuse disburseLoan ASAP
        *****/
       @SuppressWarnings("unchecked")
   	@Transactional
       @Override
       public Map<String, Object> bulkLoanDisbursal(final JsonCommand command, final CollectionSheetBulkDisbursalCommand bulkDisbursalCommand,
               Boolean isAccountTransfer) {
           final AppUser currentUser = getAppUserIfPresent();

           final SingleDisbursalCommand[] disbursalCommand = bulkDisbursalCommand.getDisburseTransactions();
           final Map<String, Object> changes = new LinkedHashMap<>();
           if (disbursalCommand == null) { return changes; }

           final LocalDate nextPossibleRepaymentDate = null;
           final Date rescheduledRepaymentDate = null;

           for (int i = 0; i < disbursalCommand.length; i++) {
               final SingleDisbursalCommand singleLoanDisbursalCommand = disbursalCommand[i];

               final Loan loan = this.loanAssembler.assembleFrom(singleLoanDisbursalCommand.getLoanId());
               final LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
               
               // validate ActualDisbursement Date Against Expected Disbursement Date
               LoanProduct loanProduct = loan.loanProduct();
               if(loanProduct.syncExpectedWithDisbursementDate()){
               	syncExpectedDateWithActualDisbursementDate(loan, actualDisbursementDate);
               }
               checkClientOrGroupActive(loan);
               this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_DISBURSAL,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

               final List<Long> existingTransactionIds = new ArrayList<>();
               final List<Long> existingReversedTransactionIds = new ArrayList<>();

               final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);

               // Bulk disbursement should happen on meeting date (mostly from
               // collection sheet).
               // FIXME: AA - this should be first meeting date based on
               // disbursement date and next available meeting dates
               // assuming repayment schedule won't regenerate because expected
               // disbursement and actual disbursement happens on same date
               loan.validateAccountStatus(LoanEvent.LOAN_DISBURSED);
               updateLoanCounters(loan, actualDisbursementDate);
               boolean canDisburse = loan.canDisburse(actualDisbursementDate);
               ChangedTransactionDetail changedTransactionDetail = null;
               Money amountDisbursed = null;
               if (canDisburse) {
                   Money amountBeforeAdjust = loan.getPrincpal();
                   Money disburseAmount = loan.adjustDisburseAmount(command, actualDisbursementDate);
                   amountDisbursed = disburseAmount.copy();
                   boolean recalculateSchedule = amountBeforeAdjust.isNotEqualTo(loan.getPrincpal());
                   final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");
                   if (isAccountTransfer) {
                       disburseLoanToSavings(loan, command, disburseAmount, paymentDetail);
                       existingTransactionIds.addAll(loan.findExistingTransactionIds());
                       existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());

                   } else {
                       existingTransactionIds.addAll(loan.findExistingTransactionIds());
                       existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
                       LoanTransaction disbursementTransaction = LoanTransaction.disbursement(loan.getOffice(), disburseAmount, paymentDetail,
                               actualDisbursementDate, txnExternalId, DateUtils.getLocalDateTimeOfTenant(), currentUser);
                       disbursementTransaction.updateLoan(loan);
                       loan.addLoanTransaction(disbursementTransaction);
                   }
                   LocalDate recalculateFrom = null;
                   final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                   regenerateScheduleOnDisbursement(command, loan, recalculateSchedule, scheduleGeneratorDTO, nextPossibleRepaymentDate,
                           rescheduledRepaymentDate);
                   if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                   	createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);
                   }
                   if (configurationDomainService.isPaymnetypeApplicableforDisbursementCharge()) {
                       changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, paymentDetail);
                   } else {
                       changedTransactionDetail = loan.disburse(currentUser, command, changes, scheduleGeneratorDTO, null);
                   }
               }
               if (!changes.isEmpty()) {

                   saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

                   final String noteText = command.stringValueOfParameterNamed("note");
                   if (StringUtils.isNotBlank(noteText)) {
                       final Note note = Note.loanNote(loan, noteText);
                       this.noteRepository.save(note);
                   }
                   if (changedTransactionDetail != null) {
                       for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                           this.loanTransactionRepository.save(mapEntry.getValue());
                           this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                       }
                   }
                   postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, noteText);
               }
               final Set<LoanCharge> loanCharges = loan.charges();
               final Map<Long, BigDecimal> disBuLoanCharges = new HashMap<>();
               for (final LoanCharge loanCharge : loanCharges) {
                   if (loanCharge.isDueAtDisbursement() && loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()
                           && loanCharge.isChargePending()) {
                       disBuLoanCharges.put(loanCharge.getId(), loanCharge.amountOutstanding());
                   }
               }
               final Locale locale = command.extractLocale();
               final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
               for (final Map.Entry<Long, BigDecimal> entrySet : disBuLoanCharges.entrySet()) {
                   final PortfolioAccountData savingAccountData = this.accountAssociationsReadPlatformService
                           .retriveLoanLinkedAssociation(loan.getId());
                   final SavingsAccount fromSavingsAccount = null;
                   final boolean isRegularTransaction = true;
                   final boolean isExceptionForBalanceCheck = false;
                   final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(actualDisbursementDate, entrySet.getValue(),
                           PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN, savingAccountData.accountId(), loan.getId(),
                           "Loan Charge Payment", locale, fmt, null, null, LoanTransactionType.REPAYMENT_AT_DISBURSEMENT.getValue(),
                           entrySet.getKey(), null, AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, null, null, null,
                           fromSavingsAccount, isRegularTransaction, isExceptionForBalanceCheck);
                   this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
               }
               updateRecurringCalendarDatesForInterestRecalculation(loan);
               this.loanAccountDomainService.recalculateAccruals(loan);
               this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_DISBURSAL,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
               
               //Code to send SMS Starts
               if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(amountDisbursed)) {
               	
               	 LoanScheduleData repaymentSchedule = null;
                    
                    LoanAccountData loanBasicDetails = this.loanReadPlatformService.retrieveOne(loan.getId());
             
                    repaymentSchedule = this.loanReadPlatformService.retrieveRepaymentSchedule(loan.getId(), loanBasicDetails.repaymentScheduleRelatedData(),
                    		this.loanReadPlatformService.retrieveLoanDisbursementDetails(loan.getId()), loanBasicDetails.isInterestRecalculationEnabled(), loanBasicDetails.getTotalPaidFeeCharges());	
                    List<LoanSchedulePeriodData> repaymentPeriods = new ArrayList<LoanSchedulePeriodData> (repaymentSchedule.getPeriods());
                    if(CommonMethodsUtil.isNotBlank(repaymentPeriods)&& repaymentPeriods.size()>1) {
                    BigDecimal   installmentAmount =  ((LoanSchedulePeriodData) repaymentPeriods.get(1)).getTotalDueForPeriod();
                    
                    String templateParamString = "templateParameters[A]="+loan.client().getFirstname()+"&templateParameters[B]="+amountDisbursed.toString()+"&templateParameters[C]="+actualDisbursementDate.toString("dd/MM/yyyy")+"&templateParameters[D]="+loan.getLoanRepaymentScheduleDetail().getNumberOfRepayments().toString()+"&templateParameters[E]="+installmentAmount.setScale(2, BigDecimal.ROUND_HALF_EVEN).toString();
                    
//               	String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+amountDisbursed.toString()+"&templateParameters[C]="+actualDisbursementDate.toString("dd/MM/yyyy");
//               String templateParamString = "templateParameters[A]="+amountDisbursed.toString()+"&templateParameters[B]="+actualDisbursementDate.toString("dd/MM/yyyy");
               SMSDataVO smsdata = new SMSDataVO("disbursement", loan.client().mobileNo(), templateParamString);
               this.smsProcessingService.sendSMS(smsdata);
                    }
               }
               //Code to send SMS Ends
           }

           return changes;
       }

       @Transactional
       @Override
       public CommandProcessingResult undoLoanDisbursal(final Long loanId, final JsonCommand command) {

           final AppUser currentUser = getAppUserIfPresent();

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_UNDO_DISBURSAL,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           removeLoanCycle(loan);

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           //
           final MonetaryCurrency currency = loan.getCurrency();
           final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);

           final LocalDate recalculateFrom = null;
           loan.setActualDisbursementDate(null);
           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           final Map<String, Object> changes = loan.undoDisbursal(scheduleGeneratorDTO, existingTransactionIds,
                   existingReversedTransactionIds, currentUser);

           if (!changes.isEmpty()) {
               saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
               this.accountTransfersWritePlatformService.reverseAllTransactions(loanId, PortfolioAccountType.LOAN);
               String noteText = null;
               if (command.hasParameter("note")) {
                   noteText = command.stringValueOfParameterNamed("note");
                   if (StringUtils.isNotBlank(noteText)) {
                       final Note note = Note.loanNote(loan, noteText);
                       this.noteRepository.save(note);
                   }
               }
               boolean isAccountTransfer = false;
               final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                       existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
               this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData, noteText);
               this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_UNDO_DISBURSAL,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           }

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loan.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult makeLoanRepayment(final Long loanId, final JsonCommand command, final boolean isRecoveryRepayment) {

       	  this.loanEventApiJsonValidator.validateNewRepaymentTransaction(command.json());

             final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
             final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
             final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");

             final Map<String, Object> changes = new LinkedHashMap<>();
             changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
             changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
             changes.put("locale", command.locale());
             changes.put("dateFormat", command.dateFormat());
             changes.put("paymentTypeId", command.stringValueOfParameterNamed("paymentTypeId"));

            final String noteText = command.stringValueOfParameterNamed("note") ;
             if (StringUtils.isNotBlank(noteText)) {
                 changes.put("note", noteText);
             }
             
             Days diff = Days.daysBetween(transactionDate, LocalDate.now());
             
             /*if(diff.getDays() > 5)
             {
           	  throw new PlatformServiceUnavailableException(
                         "error.msg.loan.repayment.before.value.date.not.allowed", "Loan Repayment transaction:" + transactionDate
                                 + "Repayment cannot be done before the value date", transactionDate);
             }
             
             else
             {
           	  List<LoanCharge> loanCharges = new ArrayList <LoanCharge>();
           	  
           	  loanCharges = this.loanChargeRepository.findLoanChargesByTransactionDate(transactionDate.toDate(), loanId);
           	  
           	  for(LoanCharge loanCharge : loanCharges)
           	  {
           		  if(loanCharge.isActive())
           		  {
           			  throw new PlatformServiceUnavailableException(
                                 "error.msg.loan.repayment.waive.penalty.charges", "Loan Repayment transaction:" + transactionDate
                                         + "Please waive the penalty charges applied till the transaction date", transactionDate);
           		  }
           	  }
             }*/
            
             final Loan loan1 = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId, true);
             LoanTransactionData transactionData = null;
             
             if(!loan1.getLoanProduct().isNewProductConfig())
               transactionData = this.loanReadPlatformService.retrieveLoanPrePaymentTemplate(loanId, transactionDate);
             
             else if(loan1.getLoanProduct().isNewProductConfig())
                 transactionData = this.loanReadPlatformService.retrieveLoanPrePaymentTemplateNew(loanId, transactionDate);
             
             final Loan loan = this.loanAssembler.assembleFrom(loanId);
             final MonetaryCurrency currency = loan.getCurrency();
             final List<LoanCharge> addCharge=new ArrayList<LoanCharge>();
            boolean isPrepaymentCharge =false;boolean isfullPaid =true; boolean waivedCharge=false;
            LocalDate chargeDate = new LocalDate();
             for(LoanCharge loanCharge: loan.charges()){
             	if(loanCharge.isPrePayment()){   
             		isPrepaymentCharge=true;        	
     	        		if(loanCharge.getDueLocalDate().equals(transactionDate)) {
     	        			addCharge.add(loanCharge);
     	        			if(transactionData.getAmount().compareTo(transactionAmount) != 0) {
     	        				isfullPaid=false;
     	        			}
     	        			
     	        		}
     	        		else if(loanCharge.isWaived()) {
     	        			 waivedCharge=true;
     	        		}
     	        		else if(!loanCharge.isWaived()) {
     	        			waivedCharge=false;   
     	        			chargeDate = loanCharge.getDueLocalDate();
     	        		}
     				}
             	}
             if(isPrepaymentCharge && !isfullPaid && !addCharge.isEmpty()) {
                 final String defaultUserMessage = "Prepayment is allowed for toltal outstanding Amount(" +transactionData.getAmount() +"). Please pay full amount as Prepayment of Loan.";
                 throw new PlatformDataIntegrityException("Prepayment is allowed for toltal outstanding Amount(" +transactionData.getAmount() +"). Please pay full amount as Prepayment of Loan.",
                 		defaultUserMessage);
                 }
             else if(isPrepaymentCharge && addCharge.isEmpty() && !waivedCharge) {
                 final String defaultUserMessage = "Prepayment is allowed on date on which is Prepayment charge is caluclated already hence Prepayment of Loan can not be done except this date(" + chargeDate +")";
                 throw new PlatformDataIntegrityException("Prepayment is allowed on date on which is Prepayment charge is caluclated already hence Prepayment of Loan can not be done except this date(" +chargeDate +")",
                 		defaultUserMessage);
                 }
             
             
             String desc="";
             if(loan.getClient() != null) {
             if(StringUtils.isNotBlank(noteText))
               desc = noteText + "^Repayment for the loan " + loan.getAccountNumber() +" of the client " + loan.getClient().getDisplayName();
             else
             	desc ="Repayment for the loan " + loan.getAccountNumber() +" of the client " + loan.getClient().getDisplayName();
             }
             final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createAndPersistPaymentDetail(command, changes);
             final Boolean isHolidayValidationDone = false;
             final HolidayDetailDTO holidayDetailDto = null;
             boolean isAccountTransfer = false;
             if(command.hasParameter("prepay")) {
           	  StringBuilder sb = new StringBuilder();        	 
           	  try {
           		  prepay=1;
           		 
           		  final Money totalCompondingAmt = loan.getReceivableInterestIncomePosting(transactionDate);
           		  final LocalDate lastCompondingDate = loan.findLastCompondingDate(transactionDate);
           	      Money interestForPendingDate= Money.of(currency, transactionData.getInterestPortion().subtract(totalCompondingAmt.getAmount()));
           	    
                     this.loanAccrualWritePlatformService.addIncomeAndAccrualTransactionsPrepayment(loanId, transactionDate,interestForPendingDate.getAmount());
                     
                 } catch (Exception e) {
                     Throwable realCause = e;
                     if (e.getCause() != null) {
                         realCause = e.getCause();
                     }
                     sb.append("failed to add income and accrual transaction for loan " + loanId + " with message " + realCause.getMessage());
                 } 	 
           	  
   	        	
             }
             final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
             this.loanAccountDomainService.makeRepayment(loan, commandProcessingResultBuilder, transactionDate, transactionAmount,
                     paymentDetail, desc, txnExternalId, isRecoveryRepayment, isAccountTransfer, holidayDetailDto, isHolidayValidationDone);
             
           //Code to send SMS Starts
             if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(transactionAmount)) {
//             String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+transactionAmount.toString()+"&templateParameters[E]="+transactionDate.toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
             	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+transactionAmount.toString();
//             	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+transactionAmount.toString()+"&templateParameters[D]="+transactionDate.toString("dd/MM/yyyy");
             SMSDataVO smsdata = new SMSDataVO("repayment", loan.client().mobileNo(), templateParamString);
             this.smsProcessingService.sendSMS(smsdata);
             }
             //Code to send SMS Ends
             
             	//Code to send SMS Starts
                 if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(loan.getClosedOnDate())&&CommonMethodsUtil.isNotNull(loan.getClosedOnDate())) {
                 	 Date todayDate = loan.getClosedOnDate();
                 	 SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
                 	 String actualClosedDate = dateFormat.format(todayDate);  
                 	BigDecimal loanamt = loan.getApprovedPrincipal();
                     String loanApproveAmt = loanamt.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString();
                 	//            String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+transactionAmount.toString()+"&templateParameters[E]="+transactionDate.toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
                 	String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loanApproveAmt+"&templateParameters[D]="+actualClosedDate;
//                 	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+transactionAmount.toString()+"&templateParameters[D]="+transactionDate.toString("dd/MM/yyyy");
                 SMSDataVO smsdata = new SMSDataVO("closeLoan", loan.client().mobileNo(), templateParamString);
                this.smsProcessingService.sendSMS(smsdata);
                 }
                 //Code to send SMS Ends
             

             return commandProcessingResultBuilder.withCommandId(command.commandId()) //
                     .withLoanId(loanId) //
                     .with(changes) //
                     .build();
       }    
      
       @Transactional
       @Override
       public Map<String, Object> makeLoanBulkRepayment(final CollectionSheetBulkRepaymentCommand bulkRepaymentCommand) {

           final SingleRepaymentCommand[] repaymentCommand = bulkRepaymentCommand.getLoanTransactions();
           final Map<String, Object> changes = new LinkedHashMap<>();
           final boolean isRecoveryRepayment = false;

           if (repaymentCommand == null) { return changes; }
           List<Long> transactionIds = new ArrayList<>();
           boolean isAccountTransfer = false;
           HolidayDetailDTO holidayDetailDTO = null;
           Boolean isHolidayValidationDone = false;
           final boolean allowTransactionsOnHoliday = this.configurationDomainService.allowTransactionsOnHolidayEnabled();
           for (final SingleRepaymentCommand singleLoanRepaymentCommand : repaymentCommand) {
               if (singleLoanRepaymentCommand != null) {
                   Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(singleLoanRepaymentCommand.getLoanId());
                   final List<Holiday> holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(),
                           singleLoanRepaymentCommand.getTransactionDate().toDate());
                   final WorkingDays workingDays = this.workingDaysRepository.findOne();
                   final boolean allowTransactionsOnNonWorkingDay = this.configurationDomainService.allowTransactionsOnNonWorkingDayEnabled();
                   boolean isHolidayEnabled = false;
                   isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
                   holidayDetailDTO = new HolidayDetailDTO(isHolidayEnabled, holidays, workingDays, allowTransactionsOnHoliday,
                           allowTransactionsOnNonWorkingDay);
                   loan.validateRepaymentDateIsOnHoliday(singleLoanRepaymentCommand.getTransactionDate(),
                           holidayDetailDTO.isAllowTransactionsOnHoliday(), holidayDetailDTO.getHolidays());
                   loan.validateRepaymentDateIsOnNonWorkingDay(singleLoanRepaymentCommand.getTransactionDate(),
                           holidayDetailDTO.getWorkingDays(), holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());
                   isHolidayValidationDone = true;
                   break;
               }

           }
           for (final SingleRepaymentCommand singleLoanRepaymentCommand : repaymentCommand) {
               if (singleLoanRepaymentCommand != null) {
                   final Loan loan = this.loanAssembler.assembleFrom(singleLoanRepaymentCommand.getLoanId());
                   final PaymentDetail paymentDetail = singleLoanRepaymentCommand.getPaymentDetail();
                   if (paymentDetail != null && paymentDetail.getId() == null) {
                       this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
                   }
                   final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
                   LoanTransaction loanTransaction = this.loanAccountDomainService.makeRepayment(loan, commandProcessingResultBuilder,
                           bulkRepaymentCommand.getTransactionDate(), singleLoanRepaymentCommand.getTransactionAmount(), paymentDetail,
                           bulkRepaymentCommand.getNote(), null, isRecoveryRepayment, isAccountTransfer, holidayDetailDTO,
                           isHolidayValidationDone);
                   transactionIds.add(loanTransaction.getId());
                 //Code to send SMS Starts
                   if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(singleLoanRepaymentCommand.getTransactionAmount())) {
//                   String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+singleLoanRepaymentCommand.getTransactionAmount().toString()+"&templateParameters[E]="+bulkRepaymentCommand.getTransactionDate().toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
                   	String templateParamString = "templateParameters[A]="+loan.client().getFirstname()+"&templateParameters[B]="+loan.getId().toString()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+singleLoanRepaymentCommand.getTransactionAmount().toString()+"&templateParameters[E]="+bulkRepaymentCommand.getTransactionDate().toString("dd/MM/yyyy");
//                   	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+singleLoanRepaymentCommand.getTransactionAmount().toString()+"&templateParameters[D]="+bulkRepaymentCommand.getTransactionDate().toString("dd/MM/yyyy");
                   SMSDataVO smsdata = new SMSDataVO("repayment", loan.client().mobileNo(), templateParamString);
                   this.smsProcessingService.sendSMS(smsdata);
                   }
                   //Code to send SMS Ends
               }
           }
           changes.put("loanTransactions", transactionIds);
           return changes;
       }

       @Transactional
       @Override
       public CommandProcessingResult adjustLoanTransaction(final Long loanId, final Long transactionId, final JsonCommand command) {

           AppUser currentUser = getAppUserIfPresent();

           this.loanEventApiJsonValidator.validateTransaction(command.json());

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           if(loan.status().isClosed() && loan.getLoanSubStatus() !=null && loan.getLoanSubStatus().equals(LoanSubStatus.FORECLOSED.getValue())) {
               final String defaultUserMessage = "The loan cannot reopend as it is foreclosed.";
               throw new LoanForeclosureException("loan.cannot.be.reopened.as.it.is.foreclosured", defaultUserMessage,
                       loanId);
           }
           checkClientOrGroupActive(loan);
           final LoanTransaction transactionToAdjust = this.loanTransactionRepository.findOne(transactionId);
           if (transactionToAdjust == null) { throw new LoanTransactionNotFoundException(transactionId); }
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_ADJUST_TRANSACTION,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_ADJUSTED_TRANSACTION, transactionToAdjust));
           if (this.accountTransfersReadPlatformService.isAccountTransfer(transactionId, PortfolioAccountType.LOAN)) { throw new PlatformServiceUnavailableException(
                   "error.msg.loan.transfer.transaction.update.not.allowed", "Loan transaction:" + transactionId
                           + " update not allowed as it involves in account transfer", transactionId); }
           if (loan.isClosedWrittenOff()) { throw new PlatformServiceUnavailableException("error.msg.loan.written.off.update.not.allowed",
                   "Loan transaction:" + transactionId + " update not allowed as loan status is written off", transactionId); }

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
           final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
           final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");

           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
           changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
           changes.put("locale", command.locale());
           changes.put("dateFormat", command.dateFormat());
           changes.put("paymentTypeId", command.stringValueOfParameterNamed("paymentTypeId"));

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
           final PaymentDetail paymentDetail = this.paymentDetailWritePlatformService.createPaymentDetail(command, changes);
           LoanTransaction newTransactionDetail = LoanTransaction.repayment(loan.getOffice(), transactionAmountAsMoney, paymentDetail,
                   transactionDate, txnExternalId, DateUtils.getLocalDateTimeOfTenant(), currentUser,loan);
           if (transactionToAdjust.isInterestWaiver()) {
               Money unrecognizedIncome = transactionAmountAsMoney.zero();
               Money interestComponent = transactionAmountAsMoney;
               if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
                   Money receivableInterest = loan.getReceivableInterest(transactionDate);
                   if (transactionAmountAsMoney.isGreaterThan(receivableInterest)) {
                       interestComponent = receivableInterest;
                       unrecognizedIncome = transactionAmountAsMoney.minus(receivableInterest);
                   }
               }
               newTransactionDetail = LoanTransaction.waiver(loan.getOffice(), loan, transactionAmountAsMoney, transactionDate,
                       interestComponent, unrecognizedIncome, DateUtils.getLocalDateTimeOfTenant(), currentUser);
           }

           LocalDate recalculateFrom = null;

           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               recalculateFrom = transactionToAdjust.getTransactionDate().isAfter(transactionDate) ? transactionDate : transactionToAdjust
                       .getTransactionDate();
           }

           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           final ChangedTransactionDetail changedTransactionDetail = loan.adjustExistingTransaction(newTransactionDetail,
                   defaultLoanLifecycleStateMachine(), transactionToAdjust, existingTransactionIds, existingReversedTransactionIds,
                   scheduleGeneratorDTO, currentUser);

           if (newTransactionDetail.isGreaterThanZero(loan.getPrincpal().getCurrency())) {
               if (paymentDetail != null) {
                   this.paymentDetailWritePlatformService.persistPaymentDetail(paymentDetail);
               }
               this.loanTransactionRepository.save(newTransactionDetail);
           }

           /***
            * TODO Vishwas Batch save is giving me a
            * HibernateOptimisticLockingFailureException, looping and saving for
            * the time being, not a major issue for now as this loop is entered
            * only in edge cases (when a adjustment is made before the latest
            * payment recorded against the loan)
            ***/
           saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
           if (changedTransactionDetail != null) {
               for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                   this.loanTransactionRepository.save(mapEntry.getValue());
                   // update loan with references to the newly created transactions
                   loan.addLoanTransaction(mapEntry.getValue());
                   this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
               }
           }

           final String noteText = command.stringValueOfParameterNamed("note");
           if (StringUtils.isNotBlank(noteText)) {
               changes.put("note", noteText);
               Note note = null;
               /**
                * If a new transaction is not created, associate note with the
                * transaction to be adjusted
                **/
               if (newTransactionDetail.isGreaterThanZero(loan.getPrincpal().getCurrency())) {
                   note = Note.loanTransactionNote(loan, newTransactionDetail, noteText);
               } else {
                   note = Note.loanTransactionNote(loan, transactionToAdjust, noteText);
               }
               this.noteRepository.save(note);
           }

           Collection<Long> transactionIds = new ArrayList<>();
           List<LoanTransaction> transactions = loan.getLoanTransactions() ;
           for (LoanTransaction transaction : transactions) {
               if (transaction.isRefund() && transaction.isNotReversed()) {
                   transactionIds.add(transaction.getId());
               }
           }

           if (!transactionIds.isEmpty()) {
               this.accountTransfersWritePlatformService
                       .reverseTransfersWithFromAccountTransactions(transactionIds, PortfolioAccountType.LOAN);
               loan.updateLoanSummarAndStatus();
           }

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, noteText);

           this.loanAccountDomainService.recalculateAccruals(loan);
           Map<BUSINESS_ENTITY, Object> entityMap = constructEntityMap(BUSINESS_ENTITY.LOAN_ADJUSTED_TRANSACTION, transactionToAdjust);
           if (newTransactionDetail.isRepayment() && newTransactionDetail.isGreaterThanZero(loan.getPrincpal().getCurrency())) {
               entityMap.put(BUSINESS_ENTITY.LOAN_TRANSACTION, newTransactionDetail);
           }
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_ADJUST_TRANSACTION, entityMap);

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(transactionId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult waiveInterestOnLoan(final Long loanId, final JsonCommand command) {

           AppUser currentUser = getAppUserIfPresent();

           this.loanEventApiJsonValidator.validateTransaction(command.json());

           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
           changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
           changes.put("locale", command.locale());
           changes.put("dateFormat", command.dateFormat());
           final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
           final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           final Money transactionAmountAsMoney = Money.of(loan.getCurrency(), transactionAmount);
           Money unrecognizedIncome = transactionAmountAsMoney.zero();
           Money interestComponent = transactionAmountAsMoney;
           if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
               Money receivableInterest = loan.getReceivableInterest(transactionDate);
               if (transactionAmountAsMoney.isGreaterThan(receivableInterest)) {
                   interestComponent = receivableInterest;
                   unrecognizedIncome = transactionAmountAsMoney.minus(receivableInterest);
               }
           }
           final LoanTransaction waiveInterestTransaction = LoanTransaction.waiver(loan.getOffice(), loan, transactionAmountAsMoney,
                   transactionDate, interestComponent, unrecognizedIncome, DateUtils.getLocalDateTimeOfTenant(), currentUser);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_WAIVE_INTEREST,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_TRANSACTION, waiveInterestTransaction));
           LocalDate recalculateFrom = null;
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               recalculateFrom = transactionDate;
           }

           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
           final ChangedTransactionDetail changedTransactionDetail = loan.waiveInterest(waiveInterestTransaction,
                   defaultLoanLifecycleStateMachine(), existingTransactionIds, existingReversedTransactionIds, scheduleGeneratorDTO,
                   currentUser);

           this.loanTransactionRepository.save(waiveInterestTransaction);

           /***
            * TODO Vishwas Batch save is giving me a
            * HibernateOptimisticLockingFailureException, looping and saving for
            * the time being, not a major issue for now as this loop is entered
            * only in edge cases (when a waiver is made before the latest payment
            * recorded against the loan)
            ***/
           saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
           if (changedTransactionDetail != null) {
               for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                   this.loanTransactionRepository.save(mapEntry.getValue());
                   // update loan with references to the newly created transactions
                   loan.addLoanTransaction(mapEntry.getValue());
                   this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
               }
           }

           final String noteText = command.stringValueOfParameterNamed("note");
           if (StringUtils.isNotBlank(noteText)) {
               changes.put("note", noteText);
               final Note note = Note.loanTransactionNote(loan, waiveInterestTransaction, noteText);
               this.noteRepository.save(note);
           }

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, noteText);
           this.loanAccountDomainService.recalculateAccruals(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_WAIVE_INTEREST,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_TRANSACTION, waiveInterestTransaction));
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(waiveInterestTransaction.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult writeOff(final Long loanId, final JsonCommand command) {
           final AppUser currentUser = getAppUserIfPresent();

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");        
           this.loanEventApiJsonValidator.validateTransactionWithNoAmount(command.json());

           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
           changes.put("locale", command.locale());
           changes.put("dateFormat", command.dateFormat());
           
         	LoanTransactionData transactionData = this.loanReadPlatformService.retrieveLoanPrePaymentTemplate(loanId, transactionDate);
           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           if(command.hasParameter("writeoffReasonId")){
           	Long writeoffReasonId = command.longValueOfParameterNamed("writeoffReasonId");
           	CodeValue writeoffReason = this.codeValueRepository.findOneByCodeNameAndIdWithNotFoundDetection(LoanApiConstants.WRITEOFFREASONS, writeoffReasonId);
           	changes.put("writeoffReasonId", writeoffReasonId);
           	loan.updateWriteOffReason(writeoffReason);
           }    
           
           checkClientOrGroupActive(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_WRITTEN_OFF,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           entityDatatableChecksWritePlatformService.runTheCheckForProduct(loanId, EntityTables.LOAN.getName(),
                   StatusEnum.WRITE_OFF.getCode().longValue(), EntityTables.LOAN.getForeignKeyColumnNameOnDatatable(), loan.productId());

           StringBuilder sb = new StringBuilder();        	 
     	  try {
     		 prepay=1;
     		final MonetaryCurrency currency = loan.getCurrency();
     		 final Money totalCompondingAmt = loan.getReceivableInterestIncomePosting(transactionDate);
   		  final LocalDate lastCompondingDate = loan.findLastCompondingDate(transactionDate);
   	      Money interestForPendingDate= Money.of(currency, transactionData.getInterestPortion().subtract(totalCompondingAmt.getAmount()));

            this.loanAccrualWritePlatformService.addIncomeAndAccrualTransactionsPrepayment(loanId, transactionDate,interestForPendingDate.getAmount());
             
           } catch (Exception e) {
               Throwable realCause = e;
               if (e.getCause() != null) {
                   realCause = e.getCause();
               }
               sb.append("failed to add income and accrual transaction for loan " + loanId + " with message " + realCause.getMessage());
           } 	
     	  
           removeLoanCycle(loan);

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           updateLoanCounters(loan, loan.getDisbursementDate());

           LocalDate recalculateFrom = null;
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               recalculateFrom = command.localDateValueOfParameterNamed("transactionDate");
           }

           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           final ChangedTransactionDetail changedTransactionDetail = loan.closeAsWrittenOff(command, defaultLoanLifecycleStateMachine(),
                   changes, existingTransactionIds, existingReversedTransactionIds, currentUser, scheduleGeneratorDTO);
           LoanTransaction writeoff = changedTransactionDetail.getNewTransactionMappings().remove(0L);
           this.loanTransactionRepository.save(writeoff);
           for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
               this.loanTransactionRepository.save(mapEntry.getValue());
               this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
           }
           saveLoanWithDataIntegrityViolationChecks(loan);
           final String noteText = command.stringValueOfParameterNamed("note");
           if (StringUtils.isNotBlank(noteText)) {
               changes.put("note", noteText);
               final Note note = Note.loanTransactionNote(loan, writeoff, noteText);
               this.noteRepository.save(note);
           }

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, noteText);
           this.loanAccountDomainService.recalculateAccruals(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_WRITTEN_OFF,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_TRANSACTION, writeoff));
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(writeoff.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult closeLoan(final Long loanId, final JsonCommand command) {

           AppUser currentUser = getAppUserIfPresent();

           this.loanEventApiJsonValidator.validateTransactionWithNoAmount(command.json());

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_CLOSE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
           changes.put("locale", command.locale());
           changes.put("dateFormat", command.dateFormat());

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           updateLoanCounters(loan, loan.getDisbursementDate());

           LocalDate recalculateFrom = null;
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               recalculateFrom = command.localDateValueOfParameterNamed("transactionDate");
           }

           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
           ChangedTransactionDetail changedTransactionDetail = loan.close(command, defaultLoanLifecycleStateMachine(), changes,
                   existingTransactionIds, existingReversedTransactionIds, scheduleGeneratorDTO, currentUser);
           final LoanTransaction possibleClosingTransaction = changedTransactionDetail.getNewTransactionMappings().remove(0L);
           if (possibleClosingTransaction != null) {
               this.loanTransactionRepository.save(possibleClosingTransaction);
           }
           for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
               this.loanTransactionRepository.save(mapEntry.getValue());
               this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
           }
           saveLoanWithDataIntegrityViolationChecks(loan);

           final String noteText = command.stringValueOfParameterNamed("note");
           if (StringUtils.isNotBlank(noteText)) {
               changes.put("note", noteText);
               final Note note = Note.loanNote(loan, noteText);
               this.noteRepository.save(note);
           }

           if (possibleClosingTransaction != null) {
               postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, noteText);
           }
           this.loanAccountDomainService.recalculateAccruals(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_CLOSE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           
           // disable all active standing instructions linked to the loan
           this.loanAccountDomainService.disableStandingInstructionsLinkedToClosedLoan(loan);
           
         //Code to send SMS Starts
           if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(loan.getClosedOnDate()) && CommonMethodsUtil.isNotNull(loan.getClosedOnDate())) {
          	 Date todayDate = loan.getClosedOnDate();
          	 SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
          	 String actualClosedDate = dateFormat.format(todayDate);  
        	BigDecimal loanamt = loan.getApprovedPrincipal();
        	 String loanApproveAmt = loanamt.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString();
          	//            String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+transactionAmount.toString()+"&templateParameters[E]="+transactionDate.toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
          	String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loanApproveAmt+"&templateParameters[D]="+actualClosedDate;
//          	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+transactionAmount.toString()+"&templateParameters[D]="+transactionDate.toString("dd/MM/yyyy");
          SMSDataVO smsdata = new SMSDataVO("closeLoan", loan.client().mobileNo(), templateParamString);
          this.smsProcessingService.sendSMS(smsdata);
          }
           //Code to send SMS Ends
           
           CommandProcessingResult result = null;
           if (possibleClosingTransaction != null) {

               result = new CommandProcessingResultBuilder() //
                       .withCommandId(command.commandId()) //
                       .withEntityId(possibleClosingTransaction.getId()) //
                       .withOfficeId(loan.getOfficeId()) //
                       .withClientId(loan.getClientId()) //
                       .withGroupId(loan.getGroupId()) //
                       .withLoanId(loanId) //
                       .with(changes) //
                       .build();
           } else {
               result = new CommandProcessingResultBuilder() //
                       .withCommandId(command.commandId()) //
                       .withEntityId(loanId) //
                       .withOfficeId(loan.getOfficeId()) //
                       .withClientId(loan.getClientId()) //
                       .withGroupId(loan.getGroupId()) //
                       .withLoanId(loanId) //
                       .with(changes) //
                       .build();
           }

           return result;
       }

       @Transactional
       @Override
       public CommandProcessingResult closeAsRescheduled(final Long loanId, final JsonCommand command) {

           this.loanEventApiJsonValidator.validateTransactionWithNoAmount(command.json());

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           removeLoanCycle(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_CLOSE_AS_RESCHEDULE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
           changes.put("locale", command.locale());
           changes.put("dateFormat", command.dateFormat());

           loan.closeAsMarkedForReschedule(command, defaultLoanLifecycleStateMachine(), changes);

           saveLoanWithDataIntegrityViolationChecks(loan);

           final String noteText = command.stringValueOfParameterNamed("note");
           if (StringUtils.isNotBlank(noteText)) {
               changes.put("note", noteText);
               final Note note = Note.loanNote(loan, noteText);
               this.noteRepository.save(note);
           }
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_CLOSE_AS_RESCHEDULE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           
           // disable all active standing instructions linked to the loan
           this.loanAccountDomainService.disableStandingInstructionsLinkedToClosedLoan(loan);
           
           //Code to send SMS Starts
           if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(loan.getClosedOnDate())&& CommonMethodsUtil.isNotNull(loan.getClosedOnDate())) {
          	 Date todayDate = loan.getClosedOnDate();
          	 SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
          	 String actualClosedDate = dateFormat.format(todayDate);  
        	BigDecimal loanamt = loan.getApprovedPrincipal();
        	 String loanApproveAmt = loanamt.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString();
          	//            String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+transactionAmount.toString()+"&templateParameters[E]="+transactionDate.toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
          	String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loanApproveAmt+"&templateParameters[D]="+actualClosedDate;
//          	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+transactionAmount.toString()+"&templateParameters[D]="+transactionDate.toString("dd/MM/yyyy");
          SMSDataVO smsdata = new SMSDataVO("closeLoan", loan.client().mobileNo(), templateParamString);
          this.smsProcessingService.sendSMS(smsdata);
          }
           //Code to send SMS Ends
           
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       private void validateAddingNewChargeAllowed(List<LoanDisbursementDetails> loanDisburseDetails) {
           boolean pendingDisbursementAvailable = false;
           for (LoanDisbursementDetails disbursementDetail : loanDisburseDetails) {
               if (disbursementDetail.actualDisbursementDate() == null) {
                   pendingDisbursementAvailable = true;
                   break;
               }
           }
           if (!pendingDisbursementAvailable) { throw new ChargeCannotBeUpdatedException(
                   "error.msg.charge.cannot.be.updated.no.pending.disbursements.in.loan",
                   "This charge cannot be added, No disbursement is pending"); }
       }

       @Transactional
       @Override
       public CommandProcessingResult addLoanCharge(final Long loanId, final JsonCommand command) {

           this.loanEventApiJsonValidator.validateAddLoanCharge(command.json());
           AppUser currentUser = getAppUserIfPresent();
           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);

           List<LoanDisbursementDetails> loanDisburseDetails = loan.getDisbursementDetails();
           final Long chargeDefinitionId = command.longValueOfParameterNamed("chargeId");
           final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(chargeDefinitionId);
           /** Habile changes for tenure */
   		ChargeAdditionalDetails chargeAdditionalDetails = this.chargeAdditionalDetailsRepository
   				.getChargeAdditionalDetails(chargeDefinition);

   		LoanChargeAdditionalDetails loanChargeAdditionalDetails = null;
   		if (chargeAdditionalDetails != null) {
   			loanChargeAdditionalDetails = LoanChargeAdditionalDetails.fromJson(null,
   					chargeAdditionalDetails.getIsEnabledFeeCalculationBasedOnTenure(),
   					chargeAdditionalDetails.getIsEnabledAutoPaid(), chargeAdditionalDetails.getIsTaxIncluded(),
   					chargeAdditionalDetails.getCreatedDate(), null, null, chargeAdditionalDetails.getCreatedById(),
   					null);
   			// loanChargeAdditionalDetails.setTaxComponentType(taxComponentType);
   		}
   		// Habiel changes end

           if (loan.isDisbursed() && chargeDefinition.isDisbursementCharge()) {
               validateAddingNewChargeAllowed(loanDisburseDetails); // validates
                                                                    // whether any
                                                                    // pending
                                                                    // disbursements
                                                                    // are
                                                                    // available to
                                                                    // apply this
                                                                    // charge
           }
           final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
           final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

           boolean isAppliedOnBackDate = false;
           LoanCharge loanCharge = null;
           boolean isPrepaymentCharge= false;
           LocalDate recalculateFrom = loan.fetchInterestRecalculateFromDate();
           if (chargeDefinition.isPercentageOfDisbursementAmount()) {
               LoanTrancheDisbursementCharge loanTrancheDisbursementCharge = null;
               for (LoanDisbursementDetails disbursementDetail : loanDisburseDetails) {
                   if (disbursementDetail.actualDisbursementDate() == null) {
                       loanCharge = LoanCharge.createNewWithoutLoan(chargeDefinition, disbursementDetail.principal(), null, null, null,
                               disbursementDetail.expectedDisbursementDateAsLocalDate(), null, null,
   							/* Habile changes */loanChargeAdditionalDetails);
                       loanTrancheDisbursementCharge = new LoanTrancheDisbursementCharge(loanCharge, disbursementDetail);
                       loanCharge.updateLoanTrancheDisbursementCharge(loanTrancheDisbursementCharge);
                       this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_ADD_CHARGE,
                               constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));
                       validateAddLoanCharge(loan, chargeDefinition, loanCharge);
                       addCharge(loan, chargeDefinition, loanCharge);
                       isAppliedOnBackDate = true;
                       if (recalculateFrom.isAfter(disbursementDetail.expectedDisbursementDateAsLocalDate())) {
                           recalculateFrom = disbursementDetail.expectedDisbursementDateAsLocalDate();
                       }
                   }
               }
               loan.addTrancheLoanCharge(chargeDefinition);
           }else if(chargeDefinition.isPercentageOfOustandingAmount()) {
          	 LoanTransactionData transactionData = null;
          	 final LoanSummaryWrapper summary = loan.getLoanSummaryWrapper();
          	 final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
          	 transactionData = this.loanReadPlatformService.retrieveLoanPrePaymentTemplateGetAddCharge(loan.getId(), dueDate);
          	 loan.setLoanSummaryWrapper(summary);
          	 loanCharge = LoanCharge.createNewFromJson(loan, chargeDefinition,transactionData, command,
 					dueDate, /* Habile changes */loanChargeAdditionalDetails);
               this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_ADD_CHARGE,
                       constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));

               validateAddLoanCharge(loan, chargeDefinition, loanCharge);
               final Money chargeAmount = loanCharge.getAmount(loan.getCurrency());
               Money feeCharges = chargeAmount;
               Money penaltyCharges = Money.zero(loan.getCurrency());
               /*final LoanTransaction applyLoanChargeTransaction = LoanTransaction.accrueLoanCharge(loan, loan.getOffice(), chargeAmount,
               		loanCharge.getDueLocalDate(), feeCharges, penaltyCharges, DateUtils.getLocalDateTimeOfTenant(), currentUser);
               Integer installmentNumber = null;
               final LoanChargePaidBy loanChargePaidBy = new LoanChargePaidBy(applyLoanChargeTransaction, loanCharge, loanCharge.getAmount(
                       loan.getCurrency()).getAmount(), installmentNumber);
               applyLoanChargeTransaction.getLoanChargesPaid().add(loanChargePaidBy);
               this.loanTransactionRepository.save(applyLoanChargeTransaction);
               loan.addLoanTransaction(applyLoanChargeTransaction);*/
               isAppliedOnBackDate = addCharge(loan, chargeDefinition, loanCharge);
               if (loanCharge.getDueLocalDate() != null || recalculateFrom.isAfter(loanCharge.getDueLocalDate())) {
                   isAppliedOnBackDate = true;                
                   recalculateFrom = loanCharge.getDueLocalDate();
                   isPrepaymentCharge =true;
               }
          } 
           else {
               loanCharge = LoanCharge.createNewFromJson(loan, chargeDefinition, command, loanChargeAdditionalDetails);
               this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_ADD_CHARGE,
                       constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));

               validateAddLoanCharge(loan, chargeDefinition, loanCharge);
               isAppliedOnBackDate = addCharge(loan, chargeDefinition, loanCharge);
               if (loanCharge.getDueLocalDate() != null || recalculateFrom.isAfter(loanCharge.getDueLocalDate())) {
                   isAppliedOnBackDate = true;
                   recalculateFrom = loanCharge.getDueLocalDate();
               }
           }

           boolean reprocessRequired = true;
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               if (isAppliedOnBackDate && loan.isFeeCompoundingEnabledForInterestRecalculation()) {
               	if(isPrepaymentCharge) {
               		runScheduleRecalculationwhilePreclose(loan, recalculateFrom);
               	}
               	else {
                   runScheduleRecalculation(loan, recalculateFrom);
                   reprocessRequired = false;
               	}
               }
               updateOriginalSchedule(loan);
           }
           if (reprocessRequired) {
               ChangedTransactionDetail changedTransactionDetail = loan.reprocessTransactions();
               if (changedTransactionDetail != null) {
                   for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                       this.loanTransactionRepository.save(mapEntry.getValue());
                       // update loan with references to the newly created
                       // transactions
                       loan.addLoanTransaction(mapEntry.getValue());
                       this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                   }
               }
               saveLoanWithDataIntegrityViolationChecks(loan);
           }

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);

           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled() && isAppliedOnBackDate
                   && loan.isFeeCompoundingEnabledForInterestRecalculation()) {
               this.loanAccountDomainService.recalculateAccruals(loan);
           }
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_ADD_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanCharge.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }
       
       @Transactional
       @Override
       public CommandProcessingResult addManualCharge(final Long loanId, final JsonCommand command) {

           this.loanEventApiJsonValidator.validateAddManualCharge(command.json());
           AppUser currentUser = getAppUserIfPresent();
           final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
           checkClientOrGroupActive(loan);
           final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");
           final LocalDate date = command.localDateValueOfParameterNamed("date");
           final Date transactionDate = command.DateValueOfParameterNamed("date");
           final LocalDateTime createdDate = DateUtils.getLocalDateTimeOfTenant();
           final String  desc = command.stringValueOfParameterNamed("typeOfCharge");
           final Long glAccountId = command.longValueOfParameterNamed("glAccountId");
           
           HolidayDetailDTO holidayDetailDTO = null;
           
           if(transactionDate.before(loan.getActualDisbursementDate())) {
               final String defaultUserMessage = "Cannot add charge before the disbursement date of the loan.";
               throw new PlatformDataIntegrityException("Cannot add charge before the disbursement date of the loan.",
               		defaultUserMessage);
               }
           
           final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, date,
                   null);
           
           holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
           loan.validateChargeDateIsOnHoliday(date, holidayDetailDTO.isAllowTransactionsOnHoliday(),
                   holidayDetailDTO.getHolidays());
           loan.validateChargeDateIsOnNonWorkingDay(date, holidayDetailDTO.getWorkingDays(),
                   holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());
           
           final GLAccount glAccount = this.glAccountRepository.findOne(glAccountId);
           final String  note = desc + " charged";
           
           final BigDecimal oustanding =  loan.updateLoanOutstandingBalacesForManualCharges(amount);
           LoanManualCharge loanManualCharge = new LoanManualCharge(loan, glAccount, amount, note, transactionDate);
           
           loan.getLoanSummary().setTotalManualChargesCharged(amount);
           
           this.loanRepositoryWrapper.save(loan);
           this.loanManualChargeRepository.save(loanManualCharge);
           
           LoanTransaction loanTransaction = LoanTransaction.manualCharges(loan, loan.getOffice(), amount, oustanding, date, createdDate, currentUser);
           this.loanTransactionRepository.save(loanTransaction);
          
           if (StringUtils.isNotBlank(note)) {
               final Note chargenote = Note.loanTransactionNote(loan, loanTransaction, note);
               this.noteRepository.save(chargenote);
           }
           
           
           this.helper.createJournalEntriesForAddLoanManualCharges(loan.getOffice(), loan.getCurrencyCode(),
                  ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), glAccount, loan.getLoanProduct().getId(),
                   new Long (6), loanId, Long.toString(loanTransaction.getId()), transactionDate, amount,note);
         
           
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanManualCharge.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }
       
       @Transactional
       @Override
       public CommandProcessingResult payManualCharge(final Long loanId, final JsonCommand command) {

           this.loanEventApiJsonValidator.validatePayManualCharge(command.json());
           AppUser currentUser = getAppUserIfPresent();
           final Loan loan = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
           checkClientOrGroupActive(loan);
           final BigDecimal amount = command.bigDecimalValueOfParameterNamed("amount");
           final LocalDate date = command.localDateValueOfParameterNamed("date");
           final Date transactionDate = command.DateValueOfParameterNamed("date");
           final LocalDateTime createdDate = DateUtils.getLocalDateTimeOfTenant();
           //final String  desc = command.stringValueOfParameterNamed("comments");
           
           HolidayDetailDTO holidayDetailDTO = null;
           
           final Long chargeId = command.longValueOfParameterNamed("chargeId");
           final LoanManualCharge loanManualCharge = this.loanManualChargeRepository.findOne(chargeId);
           
           if(transactionDate.before(loanManualCharge.getAddChargeDate())) {
               final String defaultUserMessage = "Cannot pay charge before the charged date of the charge.";
               throw new PlatformDataIntegrityException("Cannot pay charge before the charged date of the charge",
               		defaultUserMessage);
               }
           
           final ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, date,
                   null);
           
           holidayDetailDTO = scheduleGeneratorDTO.getHolidayDetailDTO();
           loan.validateChargeDateIsOnHoliday(date, holidayDetailDTO.isAllowTransactionsOnHoliday(),
                   holidayDetailDTO.getHolidays());
           loan.validateChargeDateIsOnNonWorkingDay(date, holidayDetailDTO.getWorkingDays(),
                   holidayDetailDTO.isAllowTransactionsOnNonWorkingDay());
           
           String desc = loanManualCharge.getDescription().replaceAll(" charged", "");
           final String  note = desc + " paid";
           
           final BigDecimal oustanding =  loan.updateLoanOutstandingBalacesForManualCharges(amount);
           //LoanManualCharge loanManualCharge = new LoanManualCharge(loan, glAccount, amount, note, transactionDate);
           
           loan.getLoanSummary().setTotalManualChargesRepaid(amount);
           
           this.loanRepositoryWrapper.save(loan);
           
           loanManualCharge.setAmountPaid(amount);
           loanManualCharge.setPaid(true);
           loanManualCharge.setChargePaidDate(transactionDate);
           
           this.loanManualChargeRepository.save(loanManualCharge);
           
           LoanTransaction loanTransaction = LoanTransaction.payManualCharges(loan, loan.getOffice(), amount, oustanding, date, createdDate, currentUser);
           this.loanTransactionRepository.save(loanTransaction);
          
           if (StringUtils.isNotBlank(note)) {
               final Note chargenote = Note.loanTransactionNote(loan, loanTransaction, note);
               this.noteRepository.save(chargenote);
           }
           
           this.helper.createAccrualBasedJournalEntriesAndReversalsForLoan(loan.getOffice(), loan.getCurrencyCode(),
           		ACCRUAL_ACCOUNTS_FOR_LOAN.FUND_SOURCE.getValue(), ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(), loan.getLoanProduct().getId(),
                   new Long (6), loanId, Long.toString(loanTransaction.getId()), transactionDate, amount, false, note);
         
           
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanManualCharge.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }

       private void validateAddLoanCharge(final Loan loan, final Charge chargeDefinition, final LoanCharge loanCharge) {
           if (chargeDefinition.isOverdueInstallment()) {
               final String defaultUserMessage = "Installment charge cannot be added to the loan.";
               throw new LoanChargeCannotBeAddedException("loanCharge", "overdue.charge", defaultUserMessage, null, chargeDefinition.getName());
           } else if (loanCharge.getDueLocalDate() != null
                   && loanCharge.getDueLocalDate().isBefore(loan.getLastUserTransactionForChargeCalc())) {
               final String defaultUserMessage = "charge with date before last transaction date can not be added to loan.";
               throw new LoanChargeCannotBeAddedException("loanCharge", "date.is.before.last.transaction.date", defaultUserMessage, null,
                       chargeDefinition.getName());
           } else if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {

               if (loanCharge.isInstalmentFee() && loan.status().isActive()) {
                   final String defaultUserMessage = "installment charge addition not allowed after disbursement";
                   throw new LoanChargeCannotBeAddedException("loanCharge", "installment.charge", defaultUserMessage, null,
                           chargeDefinition.getName());
               }
               final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
               final Set<LoanCharge> loanCharges = new HashSet<>(1);
               loanCharges.add(loanCharge);
               this.loanApplicationCommandFromApiJsonHelper.validateLoanCharges(loanCharges, dataValidationErrors);
               if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
           }

       }

       public void runScheduleRecalculation(final Loan loan, final LocalDate recalculateFrom) {
           AppUser currentUser = getAppUserIfPresent();
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               ScheduleGeneratorDTO generatorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
               ChangedTransactionDetail changedTransactionDetail = loan.handleRegenerateRepaymentScheduleWithInterestRecalculation(
                       generatorDTO, currentUser);
               saveLoanWithDataIntegrityViolationChecks(loan);
               if (changedTransactionDetail != null) {
                   for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                       this.loanTransactionRepository.save(mapEntry.getValue());
                       // update loan with references to the newly created
                       // transactions
                       loan.addLoanTransaction(mapEntry.getValue());
                       this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                   }
               }

           }
       }
       
       public void runScheduleRecalculationwhilePreclose(final Loan loan, final LocalDate recalculateFrom) {
           AppUser currentUser = getAppUserIfPresent();
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               ScheduleGeneratorDTO generatorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
               ChangedTransactionDetail changedTransactionDetail = loan.handleRegenerateRepaymentScheduleWithInterestRecalculationwhilePreclose(
                       generatorDTO, currentUser);
               saveLoanWithDataIntegrityViolationChecks(loan);
               if (changedTransactionDetail != null) {
                   for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                       this.loanTransactionRepository.save(mapEntry.getValue());
                       // update loan with references to the newly created
                       // transactions
                       loan.addLoanTransaction(mapEntry.getValue());
                       this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                   }
               }

           }
       }

       public void updateOriginalSchedule(Loan loan) {
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               final LocalDate recalculateFrom = null;
               ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
               createLoanScheduleArchive(loan, scheduleGeneratorDTO);
           }

       }

       private boolean addCharge(final Loan loan, final Charge chargeDefinition, final LoanCharge loanCharge) {

           AppUser currentUser = getAppUserIfPresent();
           if (!loan.hasCurrencyCodeOf(chargeDefinition.getCurrencyCode())) {
               final String errorMessage = "Charge and Loan must have the same currency.";
               throw new InvalidCurrencyException("loanCharge", "attach.to.loan", errorMessage);
           }

           if (loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()) {
               final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loan
                       .getId());
               if (portfolioAccountData == null) {
                   final String errorMessage = loanCharge.name() + "Charge  requires linked savings account for payment";
                   throw new LinkedAccountRequiredException("loanCharge.add", errorMessage, loanCharge.name());
               }
           }

           loan.addLoanCharge(loanCharge);

           this.loanChargeRepository.save(loanCharge);

           /**
            * we want to apply charge transactions only for those loans charges
            * that are applied when a loan is active and the loan product uses
            * Upfront Accruals
            **/
           if (loan.status().isActive() && loan.isNoneOrCashOrUpfrontAccrualAccountingEnabledOnLoanProduct()) {
               final LoanTransaction applyLoanChargeTransaction = loan.handleChargeAppliedTransaction(loanCharge, null, currentUser);
               this.loanTransactionRepository.save(applyLoanChargeTransaction);
           }
           boolean isAppliedOnBackDate = false;
           if (loanCharge.getDueLocalDate() == null || DateUtils.getLocalDateOfTenant().isAfter(loanCharge.getDueLocalDate())) {
               isAppliedOnBackDate = true;
           }
           return isAppliedOnBackDate;
       }

       @Transactional
       @Override
       public CommandProcessingResult updateLoanCharge(final Long loanId, final Long loanChargeId, final JsonCommand command) {

           this.loanEventApiJsonValidator.validateUpdateOfLoanCharge(command.json());

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           final LoanCharge loanCharge = retrieveLoanChargeBy(loanId, loanChargeId);

           // Charges may be edited only when the loan associated with them are
           // yet to be approved (are in submitted and pending status)
           if (!loan.status().isSubmittedAndPendingApproval()) { throw new LoanChargeCannotBeUpdatedException(
                   LOAN_CHARGE_CANNOT_BE_UPDATED_REASON.LOAN_NOT_IN_SUBMITTED_AND_PENDING_APPROVAL_STAGE, loanCharge.getId()); }

           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_UPDATE_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));

           final Map<String, Object> changes = loan.updateLoanCharge(loanCharge, command);

           saveLoanWithDataIntegrityViolationChecks(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_UPDATE_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanChargeId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult waiveLoanCharge(final Long loanId, final Long loanChargeId, final JsonCommand command) {

           AppUser currentUser = getAppUserIfPresent();

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           this.loanEventApiJsonValidator.validateInstallmentChargeTransaction(command.json());
           final LoanCharge loanCharge = retrieveLoanChargeBy(loanId, loanChargeId);

           // Charges may be waived only when the loan associated with them are
           // active
           /*if (!loan.status().isActive()) { throw new LoanChargeCannotBeWaivedException(LOAN_CHARGE_CANNOT_BE_WAIVED_REASON.LOAN_INACTIVE,
                   loanCharge.getId()); }

           // validate loan charge is not already paid or waived
           if (loanCharge.isWaived()) {
               throw new LoanChargeCannotBeWaivedException(LOAN_CHARGE_CANNOT_BE_WAIVED_REASON.ALREADY_WAIVED, loanCharge.getId());
           } else if (loanCharge.isPaid()) { throw new LoanChargeCannotBeWaivedException(LOAN_CHARGE_CANNOT_BE_WAIVED_REASON.ALREADY_PAID,
                   loanCharge.getId()); }
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_WAIVE_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));
           Integer loanInstallmentNumber = null;
           if (loanCharge.isInstalmentFee()) {
               LoanInstallmentCharge chargePerInstallment = null;
               if (!StringUtils.isBlank(command.json())) {
                   final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
                   final Integer installmentNumber = command.integerValueOfParameterNamed("installmentNumber");
                   if (dueDate != null) {
                       chargePerInstallment = loanCharge.getInstallmentLoanCharge(dueDate);
                   } else if (installmentNumber != null) {
                       chargePerInstallment = loanCharge.getInstallmentLoanCharge(installmentNumber);
                   }
               }
               if (chargePerInstallment == null) {
                   chargePerInstallment = loanCharge.getUnpaidInstallmentLoanCharge();
               }
               if (chargePerInstallment.isWaived()) {
                   throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_WAIVED, loanCharge.getId());
               } else if (chargePerInstallment.isPaid()) { throw new LoanChargeCannotBePayedException(
                       LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_PAID, loanCharge.getId()); }
               loanInstallmentNumber = chargePerInstallment.getRepaymentInstallment().getInstallmentNumber();
           }

           final Map<String, Object> changes = new LinkedHashMap<>(3);

           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           LocalDate recalculateFrom = null;
           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           Money accruedCharge = Money.zero(loan.getCurrency());
           if (loan.isPeriodicAccrualAccountingEnabledOnLoanProduct()) {
               Collection<LoanChargePaidByData> chargePaidByDatas = this.loanChargeReadPlatformService.retriveLoanChargesPaidBy(
                       loanCharge.getId(), LoanTransactionType.ACCRUAL, loanInstallmentNumber);
               for (LoanChargePaidByData chargePaidByData : chargePaidByDatas) {
                   accruedCharge = accruedCharge.plus(chargePaidByData.getAmount());
               }
           }
           
           
          for(LoanTransaction loanTransaction : loan.getLoanTransactions())
          {
       	   
       	   if(loanTransaction.getTypeOf().getValue() ==  LoanTransactionType.INCOME_POSTING.getValue() && !loanTransaction.isReversed()
       		  && loanTransaction.getPenaltyChargesPortion(loan.getCurrency()).isGreaterThan(Money.zero(loan.getCurrency())))
       	   {
       		   if(loanTransaction.getTransactionDate().getMonthOfYear() == loanCharge.getDueLocalDate().getMonthOfYear()
       			  && loanTransaction.getTransactionDate().getYear() == loanCharge.getDueLocalDate().getYear())
       		   {
       			   loanTransaction.reverse();
       			   
       		       boolean excludePaidOrWaivedCharge = true;
       		       HashMap<String, Object> feeDetails =  new HashMap<String, Object>();
       		       BigDecimal penalties = BigDecimal.ZERO;
       		       List<LoanCharge> loanCharges = new ArrayList<>();
       		       
       		       for (LoanCharge loanCharge1 : loan.charges()) {
       		    	   
       		    	   if (loanCharge1.isPenaltyCharge() && !loanCharge1.isInstalmentFee()) {
   	    		    	   LocalDate fromDate = loanCharge1.getDueLocalDate().withDayOfMonth(1);
   	    		    	   LocalDate toDate = loanTransaction.getTransactionDate();
       		    	   
   	    		           if (loanCharge1.isDueForCollectionFromAndUpToAndIncluding(fromDate, toDate)) {
   	    		              
   	    		               	if(excludePaidOrWaivedCharge) {
   	    		               		
   	    		               		if(!loanCharge.getId().equals(loanCharge1.getId()))
   	    		              		 	penalties = penalties.add(loanCharge1.amountOutstanding());
   	    		               	}
   	    		               	else
   	    		               		penalties = penalties.add(loanCharge1.amount());
   	    		                   loanCharges.add(loanCharge1);
       		               } 
       		           } 
       		       }
       		       
       		       feeDetails.put("penalties", penalties);
       		       
       		       final LoanTransaction penaltiesTransaction = LoanTransaction.incomePosting(loan, loan.getOffice(), loanTransaction.getTransactionDate().toDate(), penalties, BigDecimal.ZERO, BigDecimal.ZERO, penalties, currentUser);
       		        
       		       loan.addLoanTransaction(penaltiesTransaction);
       		        
       		        this.loanTransactionRepository.save(penaltiesTransaction);
       		        
       		        loan.updateLoanChargesPaidBy(penaltiesTransaction, feeDetails, null);
       		        
       		        break;
       		   }
       		 
       	   }
       	  
          }
          


           final LoanTransaction waiveTransaction = loan.waiveLoanCharge(loanCharge, defaultLoanLifecycleStateMachine(), changes,
                   existingTransactionIds, existingReversedTransactionIds, loanInstallmentNumber, scheduleGeneratorDTO, accruedCharge,
                   currentUser);

           this.loanTransactionRepository.save(waiveTransaction);
           saveLoanWithDataIntegrityViolationChecks(loan);

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);*/
           
           
           for(LoanTransaction loanTransaction : loan.getLoanTransactions())
           {
        	   
        	   if(loanTransaction.getTypeOf().getValue() ==  LoanTransactionType.INCOME_POSTING.getValue() && !loanTransaction.isReversed()
        		  && loanTransaction.getPenaltyChargesPortion(loan.getCurrency()).isGreaterThan(Money.zero(loan.getCurrency())))
        	   {
        		   if(loanTransaction.getTransactionDate().getMonthOfYear() == loanCharge.getDueLocalDate().getMonthOfYear()
        			  && loanTransaction.getTransactionDate().getYear() == loanCharge.getDueLocalDate().getYear())
        		   {
        			   loanTransaction.reverse();
        			   
        		       boolean excludePaidOrWaivedCharge = true;
        		       HashMap<String, Object> feeDetails =  new HashMap<String, Object>();
        		       BigDecimal penalties = BigDecimal.ZERO;
        		       List<LoanCharge> loanCharges = new ArrayList<>();
        		       
        		       for (LoanCharge loanCharge1 : loan.charges()) {
        		    	   
        		    	   if (loanCharge1.isPenaltyCharge() && !loanCharge1.isInstalmentFee()) {
    	    		    	   LocalDate fromDate = loanCharge1.getDueLocalDate().withDayOfMonth(1);
    	    		    	   LocalDate toDate = loanTransaction.getTransactionDate();
        		    	   
    	    		           if (loanCharge1.isDueForCollectionFromAndUpToAndIncluding(fromDate, toDate)) {
    	    		              
    	    		               	if(excludePaidOrWaivedCharge) {
    	    		               		
    	    		               		if(!loanCharge.getId().equals(loanCharge1.getId()))
    	    		              		 	penalties = penalties.add(loanCharge1.amountOutstanding());
    	    		               	}
    	    		               	else
    	    		               		penalties = penalties.add(loanCharge1.amount());
    	    		                   loanCharges.add(loanCharge1);
        		               } 
        		           } 
        		       }
        		       
        		      List<JournalEntry> transactionEntries = this.journalEntryRepository.findJournalEntries("L"+loanTransaction.getId().toString(), PortfolioProductType.LOAN.getValue());
        		      
        		      for(JournalEntry entry : transactionEntries)
        		      {
        		    	  entry.setReversed(true);
        		    	  this.journalEntryRepository.saveAndFlush(entry);
        		    	  
        		    	 if(entry.getType()==(JournalEntryType.DEBIT.getValue()))
       		    	  {
       		    		 final JournalEntry journalEntry = JournalEntry.createNew(loan.getOffice(), null, entry.getGlAccount(), entry.getCurrencyCode(), "L"+loanTransaction.getId().toString(),
       		                    false, loanTransaction.getTransactionDate().toDate(), JournalEntryType.CREDIT, entry.getAmount(), "Reverse Penalty Posted", PortfolioProductType.LOAN.getValue(), loanId, null,
       		                    loanTransaction, null, null, null, journalEntryReadPlatformService);
       		            this.journalEntryRepository.saveAndFlush(journalEntry);
       		    	  }
        		    	  
        		    	 else if(entry.getType()==(JournalEntryType.CREDIT.getValue()))
        		    	  {
        		    		 final JournalEntry journalEntry = JournalEntry.createNew(loan.getOffice(), null, entry.getGlAccount(), entry.getCurrencyCode(), "L"+loanTransaction.getId().toString(),
        		                    false, loanTransaction.getTransactionDate().toDate(), JournalEntryType.DEBIT, entry.getAmount(), "Reverse Penalty Posted", PortfolioProductType.LOAN.getValue(), loanId, null,
        		                    loanTransaction, null, null, null, journalEntryReadPlatformService);
        		            this.journalEntryRepository.saveAndFlush(journalEntry);
        		    	  }
        		    	
        		      }
        		       
        		      
        		       feeDetails.put("penalties", penalties);
        		       
        		       final LoanTransaction penaltiesTransaction = LoanTransaction.incomePosting(loan, loan.getOffice(), loanTransaction.getTransactionDate().toDate(), penalties, BigDecimal.ZERO, BigDecimal.ZERO, penalties, currentUser);
        		        
        		       loan.addLoanTransaction(penaltiesTransaction);
        		        
        		        this.loanTransactionRepository.save(penaltiesTransaction);
        		        
        		        loan.updateLoanChargesPaidBy(penaltiesTransaction, feeDetails, null);
        		        
        		        String creditDesc = "Penalties collected for the loan " +loan.getId().toString()+ " of the client " +loan.getClient().getDisplayName();
        		       
        		        String debitNote = "Penalty for the loan " +loan.getId().toString()+ " of the client " +loan.getClient().getDisplayName();
        		        
        		       this.helper.createCreditJournalEntryOrReversalForLoan(loan.getOffice(), loan.getCurrencyCode(), ACCRUAL_ACCOUNTS_FOR_LOAN.INCOME_FROM_PENALTIES,
        	                     loan.getLoanProduct().getId(), null, loanId, penaltiesTransaction.getId().toString(), penaltiesTransaction.getTransactionDate().toDate()
        	                     , penaltiesTransaction.getAmount(loan.getCurrency()).getAmount(), false, creditDesc);
        		       
        		      this.helper.createDebitJournalEntryOrReversalForLoan(loan.getOffice(), loan.getCurrencyCode(), ACCRUAL_ACCOUNTS_FOR_LOAN.LOAN_PORTFOLIO.getValue(),
        		    		 loan.getLoanProduct().getId(), null, loanId, penaltiesTransaction.getId().toString(), penaltiesTransaction.getTransactionDate().toDate()
        		    		    , penaltiesTransaction.getAmount(loan.getCurrency()).getAmount(), false, debitNote);

        		/*       
        		       
        		       GLAccount creditAccount = this.glAccountRepository.findOne(new Long(168));
        		       final JournalEntry  creditEntry = JournalEntry.createNew(loan.getOffice(), null, creditAccount, loan.getCurrencyCode(), penaltiesTransaction.getId().toString(),
   		                    false, penaltiesTransaction.getTransactionDate().toDate(), JournalEntryType.CREDIT, penaltiesTransaction.getAmount(loan.getCurrency()).getAmount(), "Penalties collected for the loan " +loan.getId().toString()+ " of the client " +loan.getClient().getDisplayName(), PortfolioProductType.LOAN.getValue(), loanId, null,
   		                    penaltiesTransaction, null, null, null, journalEntryReadPlatformService);
   		            this.journalEntryRepository.saveAndFlush(creditEntry);
   		            
   		            
   		            GLAccount debitAccount = this.glAccountRepository.findOne(new Long(168));
   	     		       final JournalEntry  debitEntry = JournalEntry.createNew(loan.getOffice(), null, debitAccount, loan.getCurrencyCode(), penaltiesTransaction.getId().toString(),
   			                    false, penaltiesTransaction.getTransactionDate().toDate(), JournalEntryType.DEBIT, penaltiesTransaction.getAmount(loan.getCurrency()).getAmount(), "Penalty for the loan " +loan.getId().toString()+ " of the client " +loan.getClient().getDisplayName(), PortfolioProductType.LOAN.getValue(), loanId, null,
   			                    penaltiesTransaction, null, null, null, journalEntryReadPlatformService);
   			            this.journalEntryRepository.saveAndFlush(debitEntry);*/
        		        
        		        
        		        break;
        		   }
        		 
        	   }
        	  
           }
          
           loanCharge.setActive(false);
           this.loanChargeRepository.save(loanCharge);
          
           LocalDate recalculateFrom = null;
           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
          // loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO, currentUser);
          
          final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           
           existingTransactionIds.addAll(loan.findExistingTransactionIds());
           existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
           
           //postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);
           
           loan.waiveLoanCharge1(loanCharge, defaultLoanLifecycleStateMachine(),
                   existingTransactionIds, existingReversedTransactionIds, scheduleGeneratorDTO,
                   currentUser);
           
          this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_WAIVE_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanChargeId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   //.with(changes) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult deleteLoanCharge(final Long loanId, final Long loanChargeId, final JsonCommand command) {

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           final LoanCharge loanCharge = retrieveLoanChargeBy(loanId, loanChargeId);

           // Charges may be deleted only when the loan associated with them are
           // yet to be approved (are in submitted and pending status)
           if (!loan.status().isSubmittedAndPendingApproval()) { throw new LoanChargeCannotBeDeletedException(
                   LOAN_CHARGE_CANNOT_BE_DELETED_REASON.LOAN_NOT_IN_SUBMITTED_AND_PENDING_APPROVAL_STAGE, loanCharge.getId()); }
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_DELETE_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));

           loan.removeLoanCharge(loanCharge);
           saveLoanWithDataIntegrityViolationChecks(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_DELETE_CHARGE,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_CHARGE, loanCharge));
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanChargeId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }

       @Override
       @Transactional
       public CommandProcessingResult payLoanCharge(final Long loanId, Long loanChargeId, final JsonCommand command,
               final boolean isChargeIdIncludedInJson) {

           this.loanEventApiJsonValidator.validateChargePaymentTransaction(command.json(), isChargeIdIncludedInJson);
           if (isChargeIdIncludedInJson) {
               loanChargeId = command.longValueOfParameterNamed("chargeId");
           }
           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           final LoanCharge loanCharge = retrieveLoanChargeBy(loanId, loanChargeId);

           // Charges may be waived only when the loan associated with them are
           // active
           if (!loan.status().isActive()) { throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.LOAN_INACTIVE,
                   loanCharge.getId()); }

           // validate loan charge is not already paid or waived
           if (loanCharge.isWaived()) {
               throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_WAIVED, loanCharge.getId());
           } else if (loanCharge.isPaid()) { throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_PAID,
                   loanCharge.getId()); }

           if (!loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()) { throw new LoanChargeCannotBePayedException(
                   LOAN_CHARGE_CANNOT_BE_PAYED_REASON.CHARGE_NOT_ACCOUNT_TRANSFER, loanCharge.getId()); }

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");

           final Locale locale = command.extractLocale();
           final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
           Integer loanInstallmentNumber = null;
           BigDecimal amount = loanCharge.amountOutstanding();
           if (loanCharge.isInstalmentFee()) {
               LoanInstallmentCharge chargePerInstallment = null;
               final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
               final Integer installmentNumber = command.integerValueOfParameterNamed("installmentNumber");
               if (dueDate != null) {
                   chargePerInstallment = loanCharge.getInstallmentLoanCharge(dueDate);
               } else if (installmentNumber != null) {
                   chargePerInstallment = loanCharge.getInstallmentLoanCharge(installmentNumber);
               }
               if (chargePerInstallment == null) {
                   chargePerInstallment = loanCharge.getUnpaidInstallmentLoanCharge();
               }
               if (chargePerInstallment.isWaived()) {
                   throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_WAIVED, loanCharge.getId());
               } else if (chargePerInstallment.isPaid()) { throw new LoanChargeCannotBePayedException(
                       LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_PAID, loanCharge.getId()); }
               loanInstallmentNumber = chargePerInstallment.getRepaymentInstallment().getInstallmentNumber();
               amount = chargePerInstallment.getAmountOutstanding();
           }

           final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
           if (portfolioAccountData == null) {
               final String errorMessage = "Charge with id:" + loanChargeId + " requires linked savings account for payment";
               throw new LinkedAccountRequiredException("loanCharge.pay", errorMessage, loanChargeId);
           }
           final SavingsAccount fromSavingsAccount = null;
           final boolean isRegularTransaction = true;
           final boolean isExceptionForBalanceCheck = false;
           final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount, PortfolioAccountType.SAVINGS,
                   PortfolioAccountType.LOAN, portfolioAccountData.accountId(), loanId, "Loan Charge Payment", locale, fmt, null, null,
                   LoanTransactionType.CHARGE_PAYMENT.getValue(), loanChargeId, loanInstallmentNumber,
                   AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, null, null, null, fromSavingsAccount, isRegularTransaction,
                   isExceptionForBalanceCheck);
           this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanChargeId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .withSavingsId(portfolioAccountData.accountId()).build();
       }

       public void disburseLoanToLoan(final Loan loan, final JsonCommand command, final BigDecimal amount) {

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
           final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");

           final Locale locale = command.extractLocale();
           final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
           final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount,
                   PortfolioAccountType.LOAN, PortfolioAccountType.LOAN, loan.getId(), loan.getTopupLoanDetails().getLoanIdToClose(),
                   "Loan Topup", locale, fmt, LoanTransactionType.DISBURSEMENT.getValue(), LoanTransactionType.REPAYMENT.getValue(),
                   txnExternalId, loan, null);
           AccountTransferDetails accountTransferDetails = this.accountTransfersWritePlatformService.repayLoanWithTopup(accountTransferDTO);
           loan.getTopupLoanDetails().setAccountTransferDetails(accountTransferDetails.getId());
           loan.getTopupLoanDetails().setTopupAmount(amount);
       }

       public void disburseLoanToSavings(final Loan loan, final JsonCommand command, final Money amount, final PaymentDetail paymentDetail) {

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
           final String txnExternalId = command.stringValueOfParameterNamedAllowingNull("externalId");

           final Locale locale = command.extractLocale();
           final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
           final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loan
                   .getId());
           if (portfolioAccountData == null) {
               final String errorMessage = "Disburse Loan with id:" + loan.getId() + " requires linked savings account for payment";
               throw new LinkedAccountRequiredException("loan.disburse.to.savings", errorMessage, loan.getId());
           }
           final SavingsAccount fromSavingsAccount = null;
           final boolean isExceptionForBalanceCheck = false;
           final boolean isRegularTransaction = true;
           final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount.getAmount(),
                   PortfolioAccountType.LOAN, PortfolioAccountType.SAVINGS, loan.getId(), portfolioAccountData.accountId(),
                   "Loan Disbursement", locale, fmt, paymentDetail, LoanTransactionType.DISBURSEMENT.getValue(), null, null, null,
                   AccountTransferType.ACCOUNT_TRANSFER.getValue(), null, null, txnExternalId, loan, null, fromSavingsAccount,
                   isRegularTransaction, isExceptionForBalanceCheck);
           this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);

       }

       @Override
       @CronTarget(jobName = JobName.TRANSFER_FEE_CHARGE_FOR_LOANS)
       public void transferFeeCharges() throws JobExecutionException {
           final Collection<LoanChargeData> chargeDatas = this.loanChargeReadPlatformService.retrieveLoanChargesForFeePayment(
                   ChargePaymentMode.ACCOUNT_TRANSFER.getValue(), LoanStatus.ACTIVE.getValue());
           final boolean isRegularTransaction = true;
           final StringBuilder sb = new StringBuilder();
           if (chargeDatas != null) {
               for (final LoanChargeData chargeData : chargeDatas) {
                   if (chargeData.isInstallmentFee()) {
                       final Collection<LoanInstallmentChargeData> chargePerInstallments = this.loanChargeReadPlatformService
                               .retrieveInstallmentLoanCharges(chargeData.getId(), true);
                       PortfolioAccountData portfolioAccountData = null;
                       for (final LoanInstallmentChargeData installmentChargeData : chargePerInstallments) {
                           if (!installmentChargeData.getDueDate().isAfter(new LocalDate())) {
                               if (portfolioAccountData == null) {
                                   portfolioAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(chargeData
                                           .getLoanId());
                               }
                               final SavingsAccount fromSavingsAccount = null;
                               final boolean isExceptionForBalanceCheck = false;
                               final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(new LocalDate(),
                                       installmentChargeData.getAmountOutstanding(), PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN,
                                       portfolioAccountData.accountId(), chargeData.getLoanId(), "Loan Charge Payment", null, null, null,
                                       null, LoanTransactionType.CHARGE_PAYMENT.getValue(), chargeData.getId(),
                                       installmentChargeData.getInstallmentNumber(), AccountTransferType.CHARGE_PAYMENT.getValue(), null,
                                       null, null, null, null, fromSavingsAccount, isRegularTransaction, isExceptionForBalanceCheck);
                               transferFeeCharge(sb, accountTransferDTO);
                           }
                       }
                   } else if (chargeData.getDueDate() != null && !chargeData.getDueDate().isAfter(new LocalDate())) {
                       final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService
                               .retriveLoanLinkedAssociation(chargeData.getLoanId());
                       final SavingsAccount fromSavingsAccount = null;
                       final boolean isExceptionForBalanceCheck = false;
                       final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(new LocalDate(),
                               chargeData.getAmountOutstanding(), PortfolioAccountType.SAVINGS, PortfolioAccountType.LOAN,
                               portfolioAccountData.accountId(), chargeData.getLoanId(), "Loan Charge Payment", null, null, null, null,
                               LoanTransactionType.CHARGE_PAYMENT.getValue(), chargeData.getId(), null,
                               AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, null, null, null, fromSavingsAccount,
                               isRegularTransaction, isExceptionForBalanceCheck);
                       transferFeeCharge(sb, accountTransferDTO);
                   }
               }
           }
           if (sb.length() > 0) { throw new JobExecutionException(sb.toString()); }
       }

       /**
        * @param sb
        * @param accountTransferDTO
        */
       private void transferFeeCharge(final StringBuilder sb, final AccountTransferDTO accountTransferDTO) {
           try {
               this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);
           } catch (final PlatformApiDataValidationException e) {
               sb.append("Validation exception while paying charge ").append(accountTransferDTO.getChargeId()).append(" for loan id:")
                       .append(accountTransferDTO.getToAccountId()).append("--------");
           } catch (final InsufficientAccountBalanceException e) {
               sb.append("InsufficientAccountBalance Exception while paying charge ").append(accountTransferDTO.getChargeId())
                       .append("for loan id:").append(accountTransferDTO.getToAccountId()).append("--------");

           }
       }

       private LoanCharge retrieveLoanChargeBy(final Long loanId, final Long loanChargeId) {
           final LoanCharge loanCharge = this.loanChargeRepository.findOne(loanChargeId);
           if (loanCharge == null) { throw new LoanChargeNotFoundException(loanChargeId); }

           if (loanCharge.hasNotLoanIdentifiedBy(loanId)) { throw new LoanChargeNotFoundException(loanChargeId, loanId); }
           return loanCharge;
       }

       @Transactional
       @Override
       public LoanTransaction initiateLoanTransfer(final Loan loan, final LocalDate transferDate) {

           AppUser currentUser = getAppUserIfPresent();
           this.loanAssembler.setHelpers(loan);
           checkClientOrGroupActive(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_INITIATE_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
           final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

           final LoanTransaction newTransferTransaction = LoanTransaction.initiateTransfer(loan.getOffice(), loan, transferDate,
                   DateUtils.getLocalDateTimeOfTenant(), currentUser);
           loan.addLoanTransaction(newTransferTransaction);
           loan.setLoanStatus(LoanStatus.TRANSFER_IN_PROGRESS.getValue());

           this.loanTransactionRepository.save(newTransferTransaction);
           saveLoanWithDataIntegrityViolationChecks(loan);

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds ,null);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_INITIATE_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           return newTransferTransaction;
       }

       @Transactional
       @Override
       public LoanTransaction acceptLoanTransfer(final Loan loan, final LocalDate transferDate, final Office acceptedInOffice,
               final Staff loanOfficer) {
           AppUser currentUser = getAppUserIfPresent();
           this.loanAssembler.setHelpers(loan) ;
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_ACCEPT_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
           final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

           final LoanTransaction newTransferAcceptanceTransaction = LoanTransaction.approveTransfer(acceptedInOffice, loan, transferDate,
                   DateUtils.getLocalDateTimeOfTenant(), currentUser);
           loan.addLoanTransaction(newTransferAcceptanceTransaction);
           if (loan.getTotalOverpaid() != null) {
               loan.setLoanStatus(LoanStatus.OVERPAID.getValue());
           } else {
               loan.setLoanStatus(LoanStatus.ACTIVE.getValue());
           }
           if (loanOfficer != null) {
               loan.reassignLoanOfficer(loanOfficer, transferDate);
           }

           this.loanTransactionRepository.save(newTransferAcceptanceTransaction);
           saveLoanWithDataIntegrityViolationChecks(loan);

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);
           
         //Code to send SMS Starts
           if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())) {
         
          	//    String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+transactionAmount.toString()+"&templateParameters[E]="+transactionDate.toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
          	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getOffice().getName()+"&templateParameters[D]="+acceptedInOffice.getName();
//          	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+transactionAmount.toString()+"&templateParameters[D]="+transactionDate.toString("dd/MM/yyyy");
           SMSDataVO smsdata = new SMSDataVO("clientTransfer", loan.client().mobileNo(), templateParamString);
          this.smsProcessingService.sendSMS(smsdata);
          }
           //Code to send SMS Ends
         
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_ACCEPT_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           return newTransferAcceptanceTransaction;
       }

       @Transactional
       @Override
       public LoanTransaction withdrawLoanTransfer(final Loan loan, final LocalDate transferDate) {
           AppUser currentUser = getAppUserIfPresent();
           this.loanAssembler.setHelpers(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_WITHDRAW_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           final List<Long> existingTransactionIds = new ArrayList<>(loan.findExistingTransactionIds());
           final List<Long> existingReversedTransactionIds = new ArrayList<>(loan.findExistingReversedTransactionIds());

           final LoanTransaction newTransferAcceptanceTransaction = LoanTransaction.withdrawTransfer(loan.getOffice(), loan, transferDate,
                   DateUtils.getLocalDateTimeOfTenant(), currentUser);
           loan.addLoanTransaction(newTransferAcceptanceTransaction);
           loan.setLoanStatus(LoanStatus.ACTIVE.getValue());

           this.loanTransactionRepository.save(newTransferAcceptanceTransaction);
           saveLoanWithDataIntegrityViolationChecks(loan);

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_WITHDRAW_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           return newTransferAcceptanceTransaction;
       }

       @Transactional
       @Override
       public void rejectLoanTransfer(final Loan loan) {
           this.loanAssembler.setHelpers(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_REJECT_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           loan.setLoanStatus(LoanStatus.TRANSFER_ON_HOLD.getValue());
           saveLoanWithDataIntegrityViolationChecks(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_REJECT_TRANSFER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
       }

       @Transactional
       @Override
       public CommandProcessingResult loanReassignment(final Long loanId, final JsonCommand command) {

           this.loanEventApiJsonValidator.validateUpdateOfLoanOfficer(command.json());

           final Long fromLoanOfficerId = command.longValueOfParameterNamed("fromLoanOfficerId");
           final Long toLoanOfficerId = command.longValueOfParameterNamed("toLoanOfficerId");

           final Staff fromLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(fromLoanOfficerId);
           final Staff toLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(toLoanOfficerId);
           final LocalDate dateOfLoanOfficerAssignment = command.localDateValueOfParameterNamed("assignmentDate");

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_REASSIGN_OFFICER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           if (!loan.hasLoanOfficer(fromLoanOfficer)) { throw new LoanOfficerAssignmentException(loanId, fromLoanOfficerId); }

           loan.reassignLoanOfficer(toLoanOfficer, dateOfLoanOfficerAssignment);

           saveLoanWithDataIntegrityViolationChecks(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_REASSIGN_OFFICER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult bulkLoanReassignment(final JsonCommand command) {

           this.loanEventApiJsonValidator.validateForBulkLoanReassignment(command.json());

           final Long fromLoanOfficerId = command.longValueOfParameterNamed("fromLoanOfficerId");
           final Long toLoanOfficerId = command.longValueOfParameterNamed("toLoanOfficerId");
           final String[] loanIds = command.arrayValueOfParameterNamed("loans");

           final LocalDate dateOfLoanOfficerAssignment = command.localDateValueOfParameterNamed("assignmentDate");

           final Staff fromLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(fromLoanOfficerId);
           final Staff toLoanOfficer = this.loanAssembler.findLoanOfficerByIdIfProvided(toLoanOfficerId);

           for (final String loanIdString : loanIds) {
               final Long loanId = Long.valueOf(loanIdString);
               final Loan loan = this.loanAssembler.assembleFrom(loanId);
               this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_REASSIGN_OFFICER,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
               checkClientOrGroupActive(loan);

               if (!loan.hasLoanOfficer(fromLoanOfficer)) { throw new LoanOfficerAssignmentException(loanId, fromLoanOfficerId); }

               loan.reassignLoanOfficer(toLoanOfficer, dateOfLoanOfficerAssignment);
               saveLoanWithDataIntegrityViolationChecks(loan);
               this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_REASSIGN_OFFICER,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           }
           this.loanRepositoryWrapper.flush();

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .build();
       }

       @Transactional
       @Override
       public CommandProcessingResult removeLoanOfficer(final Long loanId, final JsonCommand command) {

           final LoanUpdateCommand loanUpdateCommand = this.loanUpdateCommandFromApiJsonDeserializer.commandFromApiJson(command.json());

           loanUpdateCommand.validate();

           final LocalDate dateOfLoanOfficerunAssigned = command.localDateValueOfParameterNamed("unassignedDate");

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);

           if (loan.getLoanOfficer() == null) { throw new LoanOfficerUnassignmentException(loanId); }
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_REMOVE_OFFICER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           loan.removeLoanOfficer(dateOfLoanOfficerunAssigned);

           saveLoanWithDataIntegrityViolationChecks(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_REMOVE_OFFICER,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loanId) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }

       private void postJournalEntries(final Loan loan, final List<Long> existingTransactionIds,
               final List<Long> existingReversedTransactionIds, String note) {

           final MonetaryCurrency currency = loan.getCurrency();
           final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
           boolean isAccountTransfer = false;
           final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                   existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
           this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData, note);
       }

       @Transactional
       @Override
       public void applyMeetingDateChanges(final Calendar calendar, final Collection<CalendarInstance> loanCalendarInstances) {

           final Boolean reschedulebasedOnMeetingDates = null;
           final LocalDate presentMeetingDate = null;
           final LocalDate newMeetingDate = null;

           applyMeetingDateChanges(calendar, loanCalendarInstances, reschedulebasedOnMeetingDates, presentMeetingDate, newMeetingDate);

       }

       @Transactional
       @Override
       public void applyMeetingDateChanges(final Calendar calendar, final Collection<CalendarInstance> loanCalendarInstances,
               final Boolean reschedulebasedOnMeetingDates, final LocalDate presentMeetingDate, final LocalDate newMeetingDate) {

           final boolean isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();
           final WorkingDays workingDays = this.workingDaysRepository.findOne();
           final AppUser currentUser = getAppUserIfPresent();
           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           final Collection<Integer> loanStatuses = new ArrayList<>(Arrays.asList(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(),
                   LoanStatus.APPROVED.getValue(), LoanStatus.ACTIVE.getValue()));
           final Collection<Integer> loanTypes = new ArrayList<>(Arrays.asList(AccountType.GROUP.getValue(), AccountType.JLG.getValue()));
           final Collection<Long> loanIds = new ArrayList<>(loanCalendarInstances.size());
           // loop through loanCalendarInstances to get loan ids
           for (final CalendarInstance calendarInstance : loanCalendarInstances) {
               loanIds.add(calendarInstance.getEntityId());
           }

           final List<Loan> loans = this.loanRepositoryWrapper.findByIdsAndLoanStatusAndLoanType(loanIds, loanStatuses, loanTypes);
           List<Holiday> holidays = null;
           final LocalDate recalculateFrom = null;
           // loop through each loan to reschedule the repayment dates
           for (final Loan loan : loans) {
               if (loan != null) {
                   if (loan.getExpectedFirstRepaymentOnDate() != null && loan.getExpectedFirstRepaymentOnDate().equals(presentMeetingDate)) {
                       final String defaultUserMessage = "Meeting calendar date update is not supported since its a first repayment date";
                       throw new CalendarParameterUpdateNotSupportedException("meeting.for.first.repayment.date", defaultUserMessage,
                               loan.getExpectedFirstRepaymentOnDate(), presentMeetingDate);
                   }

                   Boolean isSkipRepaymentOnFirstMonth = false;
                   Integer numberOfDays = 0;
                   boolean isSkipRepaymentOnFirstMonthEnabled = configurationDomainService.isSkippingMeetingOnFirstDayOfMonthEnabled();
                   if(isSkipRepaymentOnFirstMonthEnabled){
                       isSkipRepaymentOnFirstMonth = this.loanUtilService.isLoanRepaymentsSyncWithMeeting(loan.group(), calendar);
                       if(isSkipRepaymentOnFirstMonth) { numberOfDays = configurationDomainService.retreivePeroidInNumberOfDaysForSkipMeetingDate().intValue(); }  
                   }
      

                   holidays = this.holidayRepository.findByOfficeIdAndGreaterThanDate(loan.getOfficeId(), loan.getDisbursementDate().toDate());
                   if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                       ScheduleGeneratorDTO scheduleGeneratorDTO = loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);
                       loan.setHelpers(null, this.loanSummaryWrapper, this.transactionProcessingStrategy);
                       loan.recalculateScheduleFromLastTransaction(scheduleGeneratorDTO, existingTransactionIds,
                               existingReversedTransactionIds, currentUser);
                       createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);
                   } else if (reschedulebasedOnMeetingDates != null && reschedulebasedOnMeetingDates) {
                       loan.updateLoanRepaymentScheduleDates(calendar.getStartDateLocalDate(), calendar.getRecurrence(), isHolidayEnabled,
                               holidays, workingDays, reschedulebasedOnMeetingDates, presentMeetingDate, newMeetingDate,
                               isSkipRepaymentOnFirstMonth, numberOfDays);
                   } else {
                       loan.updateLoanRepaymentScheduleDates(calendar.getStartDateLocalDate(), calendar.getRecurrence(), isHolidayEnabled,
                               holidays, workingDays, isSkipRepaymentOnFirstMonth, numberOfDays);
                   }

                   saveLoanWithDataIntegrityViolationChecks(loan);
               }
           }
       }

       private void removeLoanCycle(final Loan loan) {
           final List<Loan> loansToUpdate;
           if (loan.isGroupLoan()) {
               if (loan.loanProduct().isIncludeInBorrowerCycle()) {
                   loansToUpdate = this.loanRepositoryWrapper.getGroupLoansToUpdateLoanCounter(loan.getCurrentLoanCounter(), loan.getGroupId(),
                           AccountType.GROUP.getValue());
               } else {
                   loansToUpdate = this.loanRepositoryWrapper.getGroupLoansToUpdateLoanProductCounter(loan.getLoanProductLoanCounter(),
                           loan.getGroupId(), AccountType.GROUP.getValue());
               }

           } else {
               if (loan.loanProduct().isIncludeInBorrowerCycle()) {
                   loansToUpdate = this.loanRepositoryWrapper
                           .getClientOrJLGLoansToUpdateLoanCounter(loan.getCurrentLoanCounter(), loan.getClientId());
               } else {
                   loansToUpdate = this.loanRepositoryWrapper.getClientLoansToUpdateLoanProductCounter(loan.getLoanProductLoanCounter(),
                           loan.getClientId());
               }

           }
           if (loansToUpdate != null) {
               updateLoanCycleCounter(loansToUpdate, loan);
           }
           loan.updateClientLoanCounter(null);
           loan.updateLoanProductLoanCounter(null);

       }

       private void updateLoanCounters(final Loan loan, final LocalDate actualDisbursementDate) {

           if (loan.isGroupLoan()) {
               final List<Loan> loansToUpdateForLoanCounter = this.loanRepositoryWrapper.getGroupLoansDisbursedAfter(actualDisbursementDate.toDate(),
                       loan.getGroupId(), AccountType.GROUP.getValue());
               final Integer newLoanCounter = getNewGroupLoanCounter(loan);
               final Integer newLoanProductCounter = getNewGroupLoanProductCounter(loan);
               updateLoanCounter(loan, loansToUpdateForLoanCounter, newLoanCounter, newLoanProductCounter);
           } else {
               final List<Loan> loansToUpdateForLoanCounter = this.loanRepositoryWrapper.getClientOrJLGLoansDisbursedAfter(
                       actualDisbursementDate.toDate(), loan.getClientId());
               final Integer newLoanCounter = getNewClientOrJLGLoanCounter(loan);
               final Integer newLoanProductCounter = getNewClientOrJLGLoanProductCounter(loan);
               updateLoanCounter(loan, loansToUpdateForLoanCounter, newLoanCounter, newLoanProductCounter);
           }
       }

       private Integer getNewGroupLoanCounter(final Loan loan) {

           Integer maxClientLoanCounter = this.loanRepositoryWrapper.getMaxGroupLoanCounter(loan.getGroupId(), AccountType.GROUP.getValue());
           if (maxClientLoanCounter == null) {
               maxClientLoanCounter = 1;
           } else {
               maxClientLoanCounter = maxClientLoanCounter + 1;
           }
           return maxClientLoanCounter;
       }

       private Integer getNewGroupLoanProductCounter(final Loan loan) {

           Integer maxLoanProductLoanCounter = this.loanRepositoryWrapper.getMaxGroupLoanProductCounter(loan.loanProduct().getId(),
                   loan.getGroupId(), AccountType.GROUP.getValue());
           if (maxLoanProductLoanCounter == null) {
               maxLoanProductLoanCounter = 1;
           } else {
               maxLoanProductLoanCounter = maxLoanProductLoanCounter + 1;
           }
           return maxLoanProductLoanCounter;
       }

       private void updateLoanCounter(final Loan loan, final List<Loan> loansToUpdateForLoanCounter, Integer newLoanCounter,
               Integer newLoanProductCounter) {

           final boolean includeInBorrowerCycle = loan.loanProduct().isIncludeInBorrowerCycle();
           for (final Loan loanToUpdate : loansToUpdateForLoanCounter) {
               // Update client loan counter if loan product includeInBorrowerCycle
               // is true
               if (loanToUpdate.loanProduct().isIncludeInBorrowerCycle()) {
                   Integer currentLoanCounter = loanToUpdate.getCurrentLoanCounter() == null ? 1 : loanToUpdate.getCurrentLoanCounter();
                   if (newLoanCounter > currentLoanCounter) {
                       newLoanCounter = currentLoanCounter;
                   }
                   loanToUpdate.updateClientLoanCounter(++currentLoanCounter);
               }

               if (loanToUpdate.loanProduct().getId().equals(loan.loanProduct().getId())) {
                   Integer loanProductLoanCounter = loanToUpdate.getLoanProductLoanCounter();
                   if (newLoanProductCounter > loanProductLoanCounter) {
                       newLoanProductCounter = loanProductLoanCounter;
                   }
                   loanToUpdate.updateLoanProductLoanCounter(++loanProductLoanCounter);
               }
           }

           if (includeInBorrowerCycle) {
               loan.updateClientLoanCounter(newLoanCounter);
           } else {
               loan.updateClientLoanCounter(null);
           }
           loan.updateLoanProductLoanCounter(newLoanProductCounter);
           this.loanRepositoryWrapper.save(loansToUpdateForLoanCounter);
       }

       private Integer getNewClientOrJLGLoanCounter(final Loan loan) {

           Integer maxClientLoanCounter = this.loanRepositoryWrapper.getMaxClientOrJLGLoanCounter(loan.getClientId());
           if (maxClientLoanCounter == null) {
               maxClientLoanCounter = 1;
           } else {
               maxClientLoanCounter = maxClientLoanCounter + 1;
           }
           return maxClientLoanCounter;
       }

       private Integer getNewClientOrJLGLoanProductCounter(final Loan loan) {

           Integer maxLoanProductLoanCounter = this.loanRepositoryWrapper.getMaxClientOrJLGLoanProductCounter(loan.loanProduct().getId(),
                   loan.getClientId());
           if (maxLoanProductLoanCounter == null) {
               maxLoanProductLoanCounter = 1;
           } else {
               maxLoanProductLoanCounter = maxLoanProductLoanCounter + 1;
           }
           return maxLoanProductLoanCounter;
       }

       private void updateLoanCycleCounter(final List<Loan> loansToUpdate, final Loan loan) {

           final Integer currentLoancounter = loan.getCurrentLoanCounter();
           final Integer currentLoanProductCounter = loan.getLoanProductLoanCounter();

           for (final Loan loanToUpdate : loansToUpdate) {
               if (loan.loanProduct().isIncludeInBorrowerCycle()) {
                   Integer runningLoancounter = loanToUpdate.getCurrentLoanCounter();
                   if (runningLoancounter > currentLoancounter) {
                       loanToUpdate.updateClientLoanCounter(--runningLoancounter);
                   }
               }
               if (loan.loanProduct().getId().equals(loanToUpdate.loanProduct().getId())) {
                   Integer runningLoanProductCounter = loanToUpdate.getLoanProductLoanCounter();
                   if (runningLoanProductCounter > currentLoanProductCounter) {
                       loanToUpdate.updateLoanProductLoanCounter(--runningLoanProductCounter);
                   }
               }
           }
           this.loanRepositoryWrapper.save(loansToUpdate);
       }

       @Transactional
       @Override
       @CronTarget(jobName = JobName.APPLY_HOLIDAYS_TO_LOANS)
       public void applyHolidaysToLoans() {

           final boolean isHolidayEnabled = this.configurationDomainService.isRescheduleRepaymentsOnHolidaysEnabled();

           if (!isHolidayEnabled) { return; }

           final Collection<Integer> loanStatuses = new ArrayList<>(Arrays.asList(LoanStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(),
                   LoanStatus.APPROVED.getValue(), LoanStatus.ACTIVE.getValue()));
           // Get all Holidays which are active and not processed
           final List<Holiday> holidays = this.holidayRepository.findUnprocessed();

           // Loop through all holidays
           for (final Holiday holiday : holidays) {
               // All offices to which holiday is applied
               final Set<Office> offices = holiday.getOffices();
               final Collection<Long> officeIds = new ArrayList<>(offices.size());
               for (final Office office : offices) {
                   officeIds.add(office.getId());
               }

               // get all loans
               final List<Loan> loans = new ArrayList<>();
               // get all individual and jlg loans
               loans.addAll(this.loanRepositoryWrapper.findByClientOfficeIdsAndLoanStatus(officeIds, loanStatuses));
               // FIXME: AA optimize to get all client and group loans belongs to a
               // office id
               // get all group loans
               loans.addAll(this.loanRepositoryWrapper.findByGroupOfficeIdsAndLoanStatus(officeIds, loanStatuses));

               for (final Loan loan : loans) {
                   // apply holiday
                   loan.applyHolidayToRepaymentScheduleDates(holiday);
               }
               this.loanRepositoryWrapper.save(loans);
               holiday.processed();
           }
           this.holidayRepository.save(holidays);
       }

       private void checkForProductMixRestrictions(final Loan loan) {

           final List<Long> activeLoansLoanProductIds;
           final Long productId = loan.loanProduct().getId();

           if (loan.isGroupLoan()) {
               activeLoansLoanProductIds = this.loanRepositoryWrapper.findActiveLoansLoanProductIdsByGroup(loan.getGroupId(),
                       LoanStatus.ACTIVE.getValue());
           } else {
               activeLoansLoanProductIds = this.loanRepositoryWrapper.findActiveLoansLoanProductIdsByClient(loan.getClientId(),
                       LoanStatus.ACTIVE.getValue());
           }
           checkForProductMixRestrictions(activeLoansLoanProductIds, productId, loan.loanProduct().productName());
       }

       private void checkForProductMixRestrictions(final List<Long> activeLoansLoanProductIds, final Long productId, final String productName) {

           if (!CollectionUtils.isEmpty(activeLoansLoanProductIds)) {
               final Collection<LoanProductData> restrictedPrdouctsList = this.loanProductReadPlatformService
                       .retrieveRestrictedProductsForMix(productId);
               for (final LoanProductData restrictedProduct : restrictedPrdouctsList) {
                   if (activeLoansLoanProductIds.contains(restrictedProduct.getId())) { throw new LoanDisbursalException(productName,
                           restrictedProduct.getName()); }
               }
           }
       }

       private void checkClientOrGroupActive(final Loan loan) {
           final Client client = loan.client();
           if (client != null) {
               if (client.isNotActive()) { throw new ClientNotActiveException(client.getId()); }
           }
           final Group group = loan.group();
           if (group != null) {
               if (group.isNotActive()) { throw new GroupNotActiveException(group.getId()); }
           }
       }

       @Override
       @Transactional
       public void applyOverdueChargesForLoan(final Long loanId, Collection<OverdueLoanScheduleData> overdueLoanScheduleDatas) {

           Loan loan = null;
           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           boolean runInterestRecalculation = false;
           LocalDate recalculateFrom = DateUtils.getLocalDateOfTenant();
           LocalDate lastChargeDate = null;
           for (final OverdueLoanScheduleData overdueInstallment : overdueLoanScheduleDatas) {

               final JsonElement parsedCommand = this.fromApiJsonHelper.parse(overdueInstallment.toString());
               final JsonCommand command = JsonCommand.from(overdueInstallment.toString(), parsedCommand, this.fromApiJsonHelper, null, null,
                       null, null, null, loanId, null, null, null, null,null,null);
               LoanOverdueDTO overdueDTO = applyChargeToOverdueLoanInstallment(loanId, overdueInstallment.getChargeId(),
                       overdueInstallment.getPeriodNumber(), command, loan, existingTransactionIds, existingReversedTransactionIds);
               loan = overdueDTO.getLoan();
               runInterestRecalculation = runInterestRecalculation || overdueDTO.isRunInterestRecalculation();
               if (recalculateFrom.isAfter(overdueDTO.getRecalculateFrom())) {
                   recalculateFrom = overdueDTO.getRecalculateFrom();
               }
               if (lastChargeDate == null || overdueDTO.getLastChargeAppliedDate().isAfter(lastChargeDate)) {
                   lastChargeDate = overdueDTO.getLastChargeAppliedDate();
               }
           }
           if (loan != null) {
               boolean reprocessRequired = true;
               LocalDate recalculatedTill = loan.fetchInterestRecalculateFromDate();
               if (recalculateFrom.isAfter(recalculatedTill)) {
                   recalculateFrom = recalculatedTill;
               }

               if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                   if (runInterestRecalculation && loan.isFeeCompoundingEnabledForInterestRecalculation()) {
                       runScheduleRecalculation(loan, recalculateFrom);
                       reprocessRequired = false;
                   }
                   updateOriginalSchedule(loan);
               }

               if (reprocessRequired) {
                   addInstallmentIfPenaltyAppliedAfterLastDueDate(loan, lastChargeDate);
                   ChangedTransactionDetail changedTransactionDetail = loan.reprocessTransactions();
                   /** Habile changes to add extension fee if exists */
   				BigDecimal extensionFeeAmount = BigDecimal.ZERO;
   				for (LoanCharge loanCharge : loan.charges()) {
   					if (loanCharge.isActive() && loanCharge.isInstallmentRescheduled()
   							&& loanCharge.getLoanChargeAdditionalDetails().getIsEnabledAutoPaid() == 1) {
   						extensionFeeAmount = extensionFeeAmount
   								.add(loanCharge.getAmount(loan.getCurrency()).getAmount());
   					}
   				}
   				if (extensionFeeAmount.floatValue() > 0) {
   					LoanRepaymentScheduleInstallment installment = loan.getRepaymentScheduleInstallments()
   							.get(loan.getRepaymentScheduleInstallments().size() - 1);
   					if (installment.getFeeChargesCharged() != null) {
   						installment.setFeeChargesCharged(installment.getFeeChargesCharged().add(extensionFeeAmount));
   						if (installment.getFeeChargesPaid() != null) {
   							installment.setFeeChargesPaid(installment.getFeeChargesPaid().add(extensionFeeAmount));
   						} else {
   							installment.setFeeChargesPaid(extensionFeeAmount);
   						}
   					} else {
   						installment.setFeeChargesCharged(extensionFeeAmount);
   						installment.setFeeChargesPaid(extensionFeeAmount);
   					}
   					loan.updateLoanSummaryDerivedFields();
   				}
   				/** Habile changes end */
                   if (changedTransactionDetail != null) {
                       for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                           this.loanTransactionRepository.save(mapEntry.getValue());
                           // update loan with references to the newly created
                           // transactions
                           loan.addLoanTransaction(mapEntry.getValue());
                           this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
                       }
                   }
                   saveLoanWithDataIntegrityViolationChecks(loan);
               }

               postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);

               if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled() && runInterestRecalculation
                       && loan.isFeeCompoundingEnabledForInterestRecalculation()) {
                   this.loanAccountDomainService.recalculateAccruals(loan);
               }
               this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_APPLY_OVERDUE_CHARGE,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           }
       }

       private void addInstallmentIfPenaltyAppliedAfterLastDueDate(Loan loan, LocalDate lastChargeDate) {
           if (lastChargeDate != null) {
               List<LoanRepaymentScheduleInstallment> installments = loan.getRepaymentScheduleInstallments();
               LoanRepaymentScheduleInstallment lastInstallment = loan.fetchRepaymentScheduleInstallment(installments.size());
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
                   LoanRepaymentScheduleInstallment newEntry = new LoanRepaymentScheduleInstallment(loan, installments.size() + 1,
                           lastInstallment.getDueDate(), lastChargeDate, principal, interest, feeCharges, penaltyCharges,
                           recalculatedInterestComponent, compoundingDetails);
                   loan.addLoanRepaymentScheduleInstallment(newEntry);
               }
           }
       }

       public LoanOverdueDTO applyChargeToOverdueLoanInstallment(final Long loanId, final Long loanChargeId, final Integer periodNumber,
               final JsonCommand command, Loan loan, final List<Long> existingTransactionIds, final List<Long> existingReversedTransactionIds) {
           boolean runInterestRecalculation = false;
           final Charge chargeDefinition = this.chargeRepository.findOneWithNotFoundDetection(loanChargeId);
           final ChargeAdditionalDetails chargeAdditionalDetails = this.chargeAdditionalDetailsRepository
   				.getChargeAdditionalDetails(chargeDefinition);

           Collection<Integer> frequencyNumbers = loanChargeReadPlatformService.retrieveOverdueInstallmentChargeFrequencyNumber(loanId,
                   chargeDefinition.getId(), periodNumber);

           Integer feeFrequency = chargeDefinition.feeFrequency();
           final ScheduledDateGenerator scheduledDateGenerator = new DefaultScheduledDateGenerator();
           Map<Integer, LocalDate> scheduleDates = new HashMap<>();
           Loan loanCheckExistingPenalties = this.loanRepositoryWrapper.findOneWithNotFoundDetection(loanId);
   		/** Habile changes for tax calculation */
   		String taxComponentType = null;
   		if (chargeDefinition.getTaxGroup() != null) {
   			final Long clientId = loanCheckExistingPenalties.getClientId();
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

   			if (clientAddress.getStateName().equalsIgnoreCase(officeAddress.getStateName())) {
   				taxComponentType = "Intra State";
   			} else {
   				taxComponentType = "Inter State";
   			}
   		}
   		/** Habile changes end */
           final Long penaltyWaitPeriodValue = this.configurationDomainService.retrievePenaltyWaitPeriod();
           final Long penaltyPostingWaitPeriodValue = this.configurationDomainService.retrieveGraceOnPenaltyPostingPeriod();
           final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
           Long diff = penaltyWaitPeriodValue + 1 - penaltyPostingWaitPeriodValue;
           if (diff < 1) {
               diff = 1L;
           }
           LocalDate startDate = dueDate.plusDays(penaltyWaitPeriodValue.intValue() + 1);
           Integer frequencyNunber = 1;
           if (feeFrequency == null) {
               scheduleDates.put(frequencyNunber++, startDate.minusDays(diff.intValue()));
           } else {
               while (!startDate.isAfter(DateUtils.getLocalDateOfTenant())) {
                   scheduleDates.put(frequencyNunber++, startDate.minusDays(diff.intValue()));
                   LocalDate scheduleDate = scheduledDateGenerator.getRepaymentPeriodDate(PeriodFrequencyType.fromInt(feeFrequency),
                           chargeDefinition.feeInterval(), startDate);

                   startDate = scheduleDate;
               }
           }

           for (Integer frequency : frequencyNumbers) {
               scheduleDates.remove(frequency);
           }

           LoanRepaymentScheduleInstallment installment = null;
           LocalDate lastChargeAppliedDate = dueDate;
           if (!scheduleDates.isEmpty()) {
               if (loan == null) {
                   loan = this.loanAssembler.assembleFrom(loanId);
                   checkClientOrGroupActive(loan);
                   existingTransactionIds.addAll(loan.findExistingTransactionIds());
                   existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
               }
               installment = loan.fetchRepaymentScheduleInstallment(periodNumber);
               lastChargeAppliedDate = installment.getDueDate();
           }
           LocalDate recalculateFrom = DateUtils.getLocalDateOfTenant();

           if (loan != null) {
               this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_APPLY_OVERDUE_CHARGE,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
               for (Map.Entry<Integer, LocalDate> entry : scheduleDates.entrySet()) {
            	   /** Habile changes for tenure */
   				LoanChargeAdditionalDetails loanChargeAdditionalDetails = null;
   				if (chargeAdditionalDetails != null) {
   					loanChargeAdditionalDetails = LoanChargeAdditionalDetails.fromJson(null,
   							chargeAdditionalDetails.getIsEnabledFeeCalculationBasedOnTenure(),
   							chargeAdditionalDetails.getIsEnabledAutoPaid(), chargeAdditionalDetails.getIsTaxIncluded(),
   							chargeAdditionalDetails.getCreatedDate(), null, null, chargeAdditionalDetails.getCreatedById(),
   							null);
   					loanChargeAdditionalDetails.setTaxComponentType(taxComponentType);
   				}
   				/** Habiel changes end*/

                   final LoanCharge loanCharge = LoanCharge.createNewFromJson(loan, chargeDefinition, command, entry.getValue(), loanChargeAdditionalDetails);

                   LoanOverdueInstallmentCharge overdueInstallmentCharge = new LoanOverdueInstallmentCharge(loanCharge, installment,
                           entry.getKey());
                   loanCharge.updateOverdueInstallmentCharge(overdueInstallmentCharge);

                   boolean isAppliedOnBackDate = addCharge(loan, chargeDefinition, loanCharge);
                   runInterestRecalculation = runInterestRecalculation || isAppliedOnBackDate;
                   if (entry.getValue().isBefore(recalculateFrom)) {
                       recalculateFrom = entry.getValue();
                   }
                   if (entry.getValue().isAfter(lastChargeAppliedDate)) {
                       lastChargeAppliedDate = entry.getValue();
                   }
               }
           }

           return new LoanOverdueDTO(loan, runInterestRecalculation, recalculateFrom, lastChargeAppliedDate);
       }

       @Override
       public CommandProcessingResult undoWriteOff(Long loanId) {
           final AppUser currentUser = getAppUserIfPresent();

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           if (!loan.isClosedWrittenOff()) { throw new PlatformServiceUnavailableException(
                   "error.msg.loan.status.not.written.off.update.not.allowed", "Loan :" + loanId
                           + " update not allowed as loan status is not written off", loanId); }
           LocalDate recalculateFrom = null;
           LoanTransaction writeOffTransaction = loan.findWriteOffTransaction();
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_UNDO_WRITTEN_OFF,
                   constructEntityMap(BUSINESS_ENTITY.LOAN_TRANSACTION, writeOffTransaction));

           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           ChangedTransactionDetail changedTransactionDetail = loan.undoWrittenOff(existingTransactionIds, existingReversedTransactionIds,
                   scheduleGeneratorDTO, currentUser);
           if (changedTransactionDetail != null) {
               for (final Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                   this.loanTransactionRepository.save(mapEntry.getValue());
                   this.accountTransfersWritePlatformService.updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
               }
           }
           saveLoanWithDataIntegrityViolationChecks(loan);

           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);
           this.loanAccountDomainService.recalculateAccruals(loan);
           if (writeOffTransaction != null) {
               this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_UNDO_WRITTEN_OFF,
                       constructEntityMap(BUSINESS_ENTITY.LOAN_TRANSACTION, writeOffTransaction));
           }
           return new CommandProcessingResultBuilder() //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .build();
       }

       private void validateMultiDisbursementData(final JsonCommand command, LocalDate expectedDisbursementDate) {
           final String json = command.json();
           final JsonElement element = this.fromApiJsonHelper.parse(json);

           final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
           final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("loan");
           final JsonArray disbursementDataArray = command.arrayOfParameterNamed(LoanApiConstants.disbursementDataParameterName);
           if (disbursementDataArray == null || disbursementDataArray.size() == 0) {
               final String errorMessage = "For this loan product, disbursement details must be provided";
               throw new MultiDisbursementDataRequiredException(LoanApiConstants.disbursementDataParameterName, errorMessage);
           }
           final BigDecimal principal = this.fromApiJsonHelper.extractBigDecimalWithLocaleNamed("approvedLoanAmount", element);

           loanApplicationCommandFromApiJsonHelper.validateLoanMultiDisbursementdate(element, baseDataValidator, expectedDisbursementDate,
                   principal);
           if (!dataValidationErrors.isEmpty()) { throw new PlatformApiDataValidationException(dataValidationErrors); }
       }

       private void validateForAddAndDeleteTranche(final Loan loan) {

           BigDecimal totalDisbursedAmount = BigDecimal.ZERO;
           Collection<LoanDisbursementDetails> loanDisburseDetails = loan.getDisbursementDetails();
           for (LoanDisbursementDetails disbursementDetails : loanDisburseDetails) {
               if (disbursementDetails.actualDisbursementDate() != null) {
                   totalDisbursedAmount = totalDisbursedAmount.add(disbursementDetails.principal());
               }
           }
           if (totalDisbursedAmount.compareTo(loan.getApprovedPrincipal()) == 0) {
               final String errorMessage = "loan.disbursement.cannot.be.a.edited";
               throw new LoanMultiDisbursementException(errorMessage);
           }
       }

       @Override
       @Transactional
       public CommandProcessingResult addAndDeleteLoanDisburseDetails(Long loanId, JsonCommand command) {

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           final Map<String, Object> actualChanges = new LinkedHashMap<>();
           LocalDate expectedDisbursementDate = loan.getExpectedDisbursedOnLocalDate();
           if (!loan.loanProduct().isMultiDisburseLoan()) {
               final String errorMessage = "loan.product.does.not.support.multiple.disbursals";
               throw new LoanMultiDisbursementException(errorMessage);
           }
           if (loan.isSubmittedAndPendingApproval() || loan.isClosed() || loan.isClosedWrittenOff() || loan.status().isClosedObligationsMet()
                   || loan.status().isOverpaid()) {
               final String errorMessage = "cannot.modify.tranches.if.loan.is.pendingapproval.closed.overpaid.writtenoff";
               throw new LoanMultiDisbursementException(errorMessage);
           }
           validateMultiDisbursementData(command, expectedDisbursementDate);

           this.validateForAddAndDeleteTranche(loan);

           loan.updateDisbursementDetails(command, actualChanges);

           if (loan.getDisbursementDetails().isEmpty()) {
               final String errorMessage = "For this loan product, disbursement details must be provided";
               throw new MultiDisbursementDataRequiredException(LoanApiConstants.disbursementDataParameterName, errorMessage);
           }

           if (loan.getDisbursementDetails().size() > loan.loanProduct().maxTrancheCount()) {
               final String errorMessage = "Number of tranche shouldn't be greter than " + loan.loanProduct().maxTrancheCount();
               throw new ExceedingTrancheCountException(LoanApiConstants.disbursementDataParameterName, errorMessage, loan.loanProduct()
                       .maxTrancheCount(), loan.getDisbursementDetails().size());
           }
           LoanDisbursementDetails updateDetails = null;
           return processLoanDisbursementDetail(loan, loanId, command, updateDetails);

       }

       private CommandProcessingResult processLoanDisbursementDetail(final Loan loan, Long loanId, JsonCommand command,
               LoanDisbursementDetails loanDisbursementDetails) {
           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();
           existingTransactionIds.addAll(loan.findExistingTransactionIds());
           existingReversedTransactionIds.addAll(loan.findExistingReversedTransactionIds());
           final Map<String, Object> changes = new LinkedHashMap<>();
           LocalDate recalculateFrom = null;
           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           ChangedTransactionDetail changedTransactionDetail = null;
           AppUser currentUser = getAppUserIfPresent();

           if (command.entityId() != null) {

               changedTransactionDetail = loan.updateDisbursementDateAndAmountForTranche(loanDisbursementDetails, command, changes,
                       scheduleGeneratorDTO, currentUser);
           } else {
               // BigDecimal setAmount = loan.getApprovedPrincipal();
               Collection<LoanDisbursementDetails> loanDisburseDetails = loan.getDisbursementDetails();
               BigDecimal setAmount = BigDecimal.ZERO;
               for (LoanDisbursementDetails details : loanDisburseDetails) {
                   if (details.actualDisbursementDate() != null) {
                       setAmount = setAmount.add(details.principal());
                   }
               }

               loan.repaymentScheduleDetail().setPrincipal(setAmount);

               if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
                   loan.regenerateRepaymentScheduleWithInterestRecalculation(scheduleGeneratorDTO, currentUser);
               } else {
                   loan.regenerateRepaymentSchedule(scheduleGeneratorDTO, currentUser);
                   loan.processPostDisbursementTransactions();
               }
           }

           saveAndFlushLoanWithDataIntegrityViolationChecks(loan);

           if (command.entityId() != null && changedTransactionDetail != null) {
               for (Map.Entry<Long, LoanTransaction> mapEntry : changedTransactionDetail.getNewTransactionMappings().entrySet()) {
                   updateLoanTransaction(mapEntry.getKey(), mapEntry.getValue());
               }
           }
           if (loan.repaymentScheduleDetail().isInterestRecalculationEnabled()) {
               createLoanScheduleArchive(loan, scheduleGeneratorDTO);
           }
           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);
           this.loanAccountDomainService.recalculateAccruals(loan);
           return new CommandProcessingResultBuilder() //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes).build();
       }

       @Override
       @Transactional
       public CommandProcessingResult updateDisbursementDateAndAmountForTranche(final Long loanId, final Long disbursementId,
               final JsonCommand command) {

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           LoanDisbursementDetails loanDisbursementDetails = loan.fetchLoanDisbursementsById(disbursementId);
           this.loanEventApiJsonValidator.validateUpdateDisbursementDateAndAmount(command.json(), loanDisbursementDetails);

           return processLoanDisbursementDetail(loan, loanId, command, loanDisbursementDetails);

       }

       public LoanTransaction disburseLoanAmountToSavings(final Long loanId, Long loanChargeId, final JsonCommand command,
               final boolean isChargeIdIncludedInJson) {

           LoanTransaction transaction = null;

           this.loanEventApiJsonValidator.validateChargePaymentTransaction(command.json(), isChargeIdIncludedInJson);
           if (isChargeIdIncludedInJson) {
               loanChargeId = command.longValueOfParameterNamed("chargeId");
           }
           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           checkClientOrGroupActive(loan);
           final LoanCharge loanCharge = retrieveLoanChargeBy(loanId, loanChargeId);

           // Charges may be waived only when the loan associated with them are
           // active
           if (!loan.status().isActive()) { throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.LOAN_INACTIVE,
                   loanCharge.getId()); }

           // validate loan charge is not already paid or waived
           if (loanCharge.isWaived()) {
               throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_WAIVED, loanCharge.getId());
           } else if (loanCharge.isPaid()) { throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_PAID,
                   loanCharge.getId()); }

           if (!loanCharge.getChargePaymentMode().isPaymentModeAccountTransfer()) { throw new LoanChargeCannotBePayedException(
                   LOAN_CHARGE_CANNOT_BE_PAYED_REASON.CHARGE_NOT_ACCOUNT_TRANSFER, loanCharge.getId()); }

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");

           final Locale locale = command.extractLocale();
           final DateTimeFormatter fmt = DateTimeFormat.forPattern(command.dateFormat()).withLocale(locale);
           Integer loanInstallmentNumber = null;
           BigDecimal amount = loanCharge.amountOutstanding();
           if (loanCharge.isInstalmentFee()) {
               LoanInstallmentCharge chargePerInstallment = null;
               final LocalDate dueDate = command.localDateValueOfParameterNamed("dueDate");
               final Integer installmentNumber = command.integerValueOfParameterNamed("installmentNumber");
               if (dueDate != null) {
                   chargePerInstallment = loanCharge.getInstallmentLoanCharge(dueDate);
               } else if (installmentNumber != null) {
                   chargePerInstallment = loanCharge.getInstallmentLoanCharge(installmentNumber);
               }
               if (chargePerInstallment == null) {
                   chargePerInstallment = loanCharge.getUnpaidInstallmentLoanCharge();
               }
               if (chargePerInstallment.isWaived()) {
                   throw new LoanChargeCannotBePayedException(LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_WAIVED, loanCharge.getId());
               } else if (chargePerInstallment.isPaid()) { throw new LoanChargeCannotBePayedException(
                       LOAN_CHARGE_CANNOT_BE_PAYED_REASON.ALREADY_PAID, loanCharge.getId()); }
               loanInstallmentNumber = chargePerInstallment.getRepaymentInstallment().getInstallmentNumber();
               amount = chargePerInstallment.getAmountOutstanding();
           }

           final PortfolioAccountData portfolioAccountData = this.accountAssociationsReadPlatformService.retriveLoanLinkedAssociation(loanId);
           if (portfolioAccountData == null) {
               final String errorMessage = "Charge with id:" + loanChargeId + " requires linked savings account for payment";
               throw new LinkedAccountRequiredException("loanCharge.pay", errorMessage, loanChargeId);
           }
           final SavingsAccount fromSavingsAccount = null;
           final boolean isRegularTransaction = true;
           final boolean isExceptionForBalanceCheck = false;
           final AccountTransferDTO accountTransferDTO = new AccountTransferDTO(transactionDate, amount, PortfolioAccountType.SAVINGS,
                   PortfolioAccountType.LOAN, portfolioAccountData.accountId(), loanId, "Loan Charge Payment", locale, fmt, null, null,
                   LoanTransactionType.CHARGE_PAYMENT.getValue(), loanChargeId, loanInstallmentNumber,
                   AccountTransferType.CHARGE_PAYMENT.getValue(), null, null, null, null, null, fromSavingsAccount, isRegularTransaction,
                   isExceptionForBalanceCheck);
           this.accountTransfersWritePlatformService.transferFunds(accountTransferDTO);

           return transaction;
       }

       @Transactional
       @Override
       public void recalculateInterest(final long loanId) {
           Loan loan = this.loanAssembler.assembleFrom(loanId);
           LocalDate recalculateFrom = loan.fetchInterestRecalculateFromDate();
           AppUser currentUser = getAppUserIfPresent();
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_INTEREST_RECALCULATION,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           ScheduleGeneratorDTO generatorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFrom);

           ChangedTransactionDetail changedTransactionDetail = loan.recalculateScheduleFromLastTransaction(generatorDTO,
                   existingTransactionIds, existingReversedTransactionIds, currentUser);

           saveLoanWithDataIntegrityViolationChecks(loan);

           /*
   		 * Habile changes Adding extension fee chanrges into the installments
   		 */
   		Set<LoanCharge> charges = loan.charges();
   		LocalDate lastRepaymentDate = loan.getLastRepaymentPeriodDueDate(true);
   		BigDecimal extensionFeeCharged = BigDecimal.ZERO;
   		BigDecimal extensionFeePaid = BigDecimal.ZERO;
   		for (final LoanCharge loanCharge : charges) {
   			if (loanCharge.isInstallmentRescheduled() && (loanCharge.getLoanChargeAdditionalDetails() != null
   					&& loanCharge.getLoanChargeAdditionalDetails().getIsEnabledAutoPaid() == 1)) {
   				extensionFeeCharged = extensionFeeCharged.add(loanCharge.getAmount(loan.getCurrency()).getAmount());
   				extensionFeePaid = extensionFeePaid.add(loanCharge.getAmountPaid(loan.getCurrency()).getAmount());
   			}
   		}

   		for (LoanRepaymentScheduleInstallment loanRepaymentSchedule : loan.getRepaymentScheduleInstallments()) {
   			if (loanRepaymentSchedule.getDueDate().equals(lastRepaymentDate)
   					&& loanRepaymentSchedule.getFeeChargesCharged() != null) {
   				loanRepaymentSchedule
   						.setFeeChargesCharged(loanRepaymentSchedule.getFeeChargesCharged().add(extensionFeeCharged));
   				if (loanRepaymentSchedule.getFeeChargesPaid() != null) {
   					loanRepaymentSchedule
   							.setFeeChargesPaid(loanRepaymentSchedule.getFeeChargesPaid().add(extensionFeePaid));
   				} else {
   					loanRepaymentSchedule.setFeeChargesPaid(extensionFeePaid);
   				}
   			} else if (loanRepaymentSchedule.getDueDate().equals(lastRepaymentDate)) {
   				loanRepaymentSchedule.setFeeChargesCharged(extensionFeeCharged);
   				loanRepaymentSchedule.setFeeChargesPaid(extensionFeePaid);
   			}
   		}
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
           postJournalEntries(loan, existingTransactionIds, existingReversedTransactionIds, null);
           this.loanAccountDomainService.recalculateAccruals(loan);
           this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_INTEREST_RECALCULATION,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
       }

       @Override
       public CommandProcessingResult recoverFromGuarantor(final Long loanId) {
           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           this.guarantorDomainService.transaferFundsFromGuarantor(loan);
           return new CommandProcessingResultBuilder().withLoanId(loanId).build();
       }

       private void updateLoanTransaction(final Long loanTransactionId, final LoanTransaction newLoanTransaction) {
           final AccountTransferTransaction transferTransaction = this.accountTransferRepository.findByToLoanTransactionId(loanTransactionId);
           if (transferTransaction != null) {
               transferTransaction.updateToLoanTransaction(newLoanTransaction);
               this.accountTransferRepository.save(transferTransaction);
           }
       }

       private void createLoanScheduleArchive(final Loan loan, final ScheduleGeneratorDTO scheduleGeneratorDTO) {
           createAndSaveLoanScheduleArchive(loan, scheduleGeneratorDTO);

       }

       private void regenerateScheduleOnDisbursement(final JsonCommand command, final Loan loan, final boolean recalculateSchedule,
               final ScheduleGeneratorDTO scheduleGeneratorDTO, final LocalDate nextPossibleRepaymentDate, final Date rescheduledRepaymentDate) {
           AppUser currentUser = getAppUserIfPresent();
           final LocalDate actualDisbursementDate = command.localDateValueOfParameterNamed("actualDisbursementDate");
           BigDecimal emiAmount = command.bigDecimalValueOfParameterNamed(LoanApiConstants.emiAmountParameterName);
           loan.regenerateScheduleOnDisbursement(scheduleGeneratorDTO, recalculateSchedule, actualDisbursementDate, emiAmount, currentUser,
                   nextPossibleRepaymentDate, rescheduledRepaymentDate);
       }

       private List<LoanRepaymentScheduleInstallment> retrieveRepaymentScheduleFromModel(LoanScheduleModel model) {
           final List<LoanRepaymentScheduleInstallment> installments = new ArrayList<>();
           for (final LoanScheduleModelPeriod scheduledLoanInstallment : model.getPeriods()) {
               if (scheduledLoanInstallment.isRepaymentPeriod()) {
                   final LoanRepaymentScheduleInstallment installment = new LoanRepaymentScheduleInstallment(null,
                           scheduledLoanInstallment.periodNumber(), scheduledLoanInstallment.periodFromDate(),
                           scheduledLoanInstallment.periodDueDate(), scheduledLoanInstallment.principalDue(),
                           scheduledLoanInstallment.interestDue(), scheduledLoanInstallment.feeChargesDue(),
                           scheduledLoanInstallment.penaltyChargesDue(), scheduledLoanInstallment.isRecalculatedInterestComponent(),
                           scheduledLoanInstallment.getLoanCompoundingDetails());
                   installments.add(installment);
               }
           }
           return installments;
       }

       @Override
       @Transactional
       public CommandProcessingResult makeLoanRefund(Long loanId, JsonCommand command) {
           // TODO Auto-generated method stub

           this.loanEventApiJsonValidator.validateNewRefundTransaction(command.json());

           final LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");

           // checkRefundDateIsAfterAtLeastOneRepayment(loanId, transactionDate);

           final BigDecimal transactionAmount = command.bigDecimalValueOfParameterNamed("transactionAmount");
           checkIfLoanIsPaidInAdvance(loanId, transactionAmount);

           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", command.stringValueOfParameterNamed("transactionDate"));
           changes.put("transactionAmount", command.stringValueOfParameterNamed("transactionAmount"));
           changes.put("locale", command.locale());
           changes.put("dateFormat", command.dateFormat());

           final String noteText = command.stringValueOfParameterNamed("note");
           if (StringUtils.isNotBlank(noteText)) {
               changes.put("note", noteText);
           }

           final PaymentDetail paymentDetail = null;

           final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();

           this.loanAccountDomainService.makeRefundForActiveLoan(loanId, commandProcessingResultBuilder, transactionDate, transactionAmount,
                   paymentDetail, noteText, null);

           return commandProcessingResultBuilder.withCommandId(command.commandId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();

       }

       private void checkIfLoanIsPaidInAdvance(final Long loanId, final BigDecimal transactionAmount) {
           BigDecimal overpaid = this.loanReadPlatformService.retrieveTotalPaidInAdvance(loanId).getPaidInAdvance();

           if (overpaid == null || overpaid.equals(new BigDecimal(0)) || transactionAmount.floatValue() > overpaid.floatValue()) {
               if (overpaid == null) overpaid = BigDecimal.ZERO;
               throw new InvalidPaidInAdvanceAmountException(overpaid.toPlainString());
           }
       }

       private AppUser getAppUserIfPresent() {
           AppUser user = null;
           if (this.context != null) {
               user = this.context.getAuthenticatedUserIfPresent();
           }
           return user;
       }

       private Map<BUSINESS_ENTITY, Object> constructEntityMap(final BUSINESS_ENTITY entityEvent, Object entity) {
           Map<BUSINESS_ENTITY, Object> map = new HashMap<>(1);
           map.put(entityEvent, entity);
           return map;
       }

       @Override
       @Transactional
       public CommandProcessingResult undoLastLoanDisbursal(Long loanId, JsonCommand command) {
           final AppUser currentUser = getAppUserIfPresent();

           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           final LocalDate recalculateFromDate = loan.getLastRepaymentDate();
           validateIsMultiDisbursalLoanAndDisbursedMoreThanOneTranche(loan);
           checkClientOrGroupActive(loan);
           this.businessEventNotifierService.notifyBusinessEventToBeExecuted(BUSINESS_EVENTS.LOAN_UNDO_LASTDISBURSAL,
                   constructEntityMap(BUSINESS_ENTITY.LOAN, loan));

           final MonetaryCurrency currency = loan.getCurrency();
           final ApplicationCurrency applicationCurrency = this.applicationCurrencyRepository.findOneWithNotFoundDetection(currency);
           final List<Long> existingTransactionIds = new ArrayList<>();
           final List<Long> existingReversedTransactionIds = new ArrayList<>();

           ScheduleGeneratorDTO scheduleGeneratorDTO = this.loanUtilService.buildScheduleGeneratorDTO(loan, recalculateFromDate);

           final Map<String, Object> changes = loan.undoLastDisbursal(scheduleGeneratorDTO, existingTransactionIds,
                   existingReversedTransactionIds, currentUser, loan);
           if (!changes.isEmpty()) {
               saveAndFlushLoanWithDataIntegrityViolationChecks(loan);
               String noteText = null;
               if (command.hasParameter("note")) {
                   noteText = command.stringValueOfParameterNamed("note");
                   if (StringUtils.isNotBlank(noteText)) {
                       final Note note = Note.loanNote(loan, noteText);
                       this.noteRepository.save(note);
                   }
               }
               boolean isAccountTransfer = false;
               final Map<String, Object> accountingBridgeData = loan.deriveAccountingBridgeData(applicationCurrency.toData(),
                       existingTransactionIds, existingReversedTransactionIds, isAccountTransfer);
               this.journalEntryWritePlatformService.createJournalEntriesForLoan(accountingBridgeData, noteText);
               this.businessEventNotifierService.notifyBusinessEventWasExecuted(BUSINESS_EVENTS.LOAN_UNDO_LASTDISBURSAL,
                       constructEntityMap(BUSINESS_ENTITY.LOAN, loan));
           }

           return new CommandProcessingResultBuilder() //
                   .withCommandId(command.commandId()) //
                   .withEntityId(loan.getId()) //
                   .withOfficeId(loan.getOfficeId()) //
                   .withClientId(loan.getClientId()) //
                   .withGroupId(loan.getGroupId()) //
                   .withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       @Override
       @Transactional
       public CommandProcessingResult forecloseLoan(final Long loanId, final JsonCommand command) {
           final String json = command.json();
           final JsonElement element = fromApiJsonHelper.parse(json);
           final Loan loan = this.loanAssembler.assembleFrom(loanId);
           final LocalDate transactionDate = this.fromApiJsonHelper.extractLocalDateNamed(LoanApiConstants.transactionDateParamName, element);
           this.loanEventApiJsonValidator.validateLoanForeclosure(command.json());
           final Map<String, Object> changes = new LinkedHashMap<>();
           changes.put("transactionDate", transactionDate);
           String noteText = this.fromApiJsonHelper.extractStringNamed(LoanApiConstants.noteParamName, element);
           LoanRescheduleRequest loanRescheduleRequest = null;
           for (LoanDisbursementDetails loanDisbursementDetails : loan.getDisbursementDetails()) {
                  if (!loanDisbursementDetails.expectedDisbursementDateAsLocalDate().isAfter(transactionDate)
                       && loanDisbursementDetails.actualDisbursementDate() == null) {
                   final String defaultUserMessage = "The loan with undisbrsed tranche before foreclosure cannot be foreclosed.";
                   throw new LoanForeclosureException("loan.with.undisbursed.tranche.before.foreclosure.cannot.be.foreclosured",
                           defaultUserMessage, transactionDate);
               }
           }
           this.loanScheduleHistoryWritePlatformService.createAndSaveLoanScheduleArchive(loan.getRepaymentScheduleInstallments(),
                   loan, loanRescheduleRequest);
           

           final Map<String, Object> modifications = this.loanAccountDomainService.foreCloseLoan(loan, transactionDate, noteText);
           changes.putAll(modifications);

         //Code to send SMS Starts
           if(CommonMethodsUtil.isNotNull(loan.client()) && CommonMethodsUtil.isNotBlank(loan.client().mobileNo())&&CommonMethodsUtil.isNotBlank(loan.client().getDisplayName())&&CommonMethodsUtil.isNotBlank(loan.getClosedOnDate())) {
          	 Date todayDate = loan.getClosedOnDate();
          	 SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MM-yyyy");
          	 String actualClosedDate = dateFormat.format(todayDate);  
          	BigDecimal loanamt = loan.getApprovedPrincipal();
           String loanApproveAmt = loanamt.setScale(0,BigDecimal.ROUND_HALF_UP).toPlainString();
          	//    String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loan.getCurrencyCode()+"&templateParameters[D]="+transactionAmount.toString()+"&templateParameters[E]="+transactionDate.toString("dd/MM/yyyy")+"&templateParameters[F]="+loan.getSummary().getTotalOutstanding().toString()+"&templateParameters[G]="+loan.getCurrencyCode();
          	String templateParamString = "templateParameters[A]="+loan.client().getDisplayName()+"&templateParameters[B]="+loan.getAccountNumber()+"&templateParameters[C]="+loanApproveAmt+"&templateParameters[D]="+actualClosedDate;
//          	String templateParamString = "templateParameters[A]="+loan.getAccountNumber()+"&templateParameters[B]="+loan.getCurrencyCode()+"&templateParameters[C]="+transactionAmount.toString()+"&templateParameters[D]="+transactionDate.toString("dd/MM/yyyy");
          SMSDataVO smsdata = new SMSDataVO("closeLoan", loan.client().mobileNo(), templateParamString);
          this.smsProcessingService.sendSMS(smsdata);
          }
           //Code to send SMS Ends

           
           final CommandProcessingResultBuilder commandProcessingResultBuilder = new CommandProcessingResultBuilder();
           return commandProcessingResultBuilder.withLoanId(loanId) //
                   .with(changes) //
                   .build();
       }

       private void validateIsMultiDisbursalLoanAndDisbursedMoreThanOneTranche(Loan loan) {
           if (!loan.isMultiDisburmentLoan()) {
               final String errorMessage = "loan.product.does.not.support.multiple.disbursals.cannot.undo.last.disbursal";
               throw new LoanMultiDisbursementException(errorMessage);
           }
           Integer trancheDisbursedCount = 0;
           for (LoanDisbursementDetails disbursementDetails : loan.getDisbursementDetails()) {
               if (disbursementDetails.actualDisbursementDate() != null) {
                   trancheDisbursedCount++;
               }
           }
           if (trancheDisbursedCount <= 1) {
               final String errorMessage = "tranches.should.be.disbursed.more.than.one.to.undo.last.disbursal";
               throw new LoanMultiDisbursementException(errorMessage);
           }

       }
       
       private void syncExpectedDateWithActualDisbursementDate(final Loan loan, LocalDate actualDisbursementDate){
   	   	if(!loan.getExpectedDisbursedOnLocalDate().equals(actualDisbursementDate)){
   	   		throw new DateMismatchException(actualDisbursementDate, 
   	   				loan.getExpectedDisbursedOnLocalDate());
   	   	}
   	   	
   	   }
   }