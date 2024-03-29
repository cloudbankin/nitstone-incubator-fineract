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
package org.finabile.fineract.portfolio.tax.service;

import java.util.Arrays;
import java.util.List;

import org.apache.fineract.accounting.common.AccountingDropdownReadPlatformService;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.finabile.fineract.portfolio.tax.domain.TaxComponentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class HabileTaxReadPlatformServiceImpl implements HabileTaxReadPlatformService {

	private final JdbcTemplate jdbcTemplate;
	private final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService;

	@Autowired
	public HabileTaxReadPlatformServiceImpl(final RoutingDataSource dataSource,
			final AccountingDropdownReadPlatformService accountingDropdownReadPlatformService) {
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		this.accountingDropdownReadPlatformService = accountingDropdownReadPlatformService;
	}

	@Override
	public List<EnumOptionData> retrieveTaxComponentTypeTemplate() {
		final List<EnumOptionData> allowedTaxComponentTypes = Arrays.asList(
				TaxComponentTypeEnumerations.taxComponentType(TaxComponentType.INTER_STATE),
				TaxComponentTypeEnumerations.taxComponentType(TaxComponentType.INTRA_STATE));
		return allowedTaxComponentTypes;
	}

}
