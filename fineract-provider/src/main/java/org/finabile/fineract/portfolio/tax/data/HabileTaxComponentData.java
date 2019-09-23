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
package org.finabile.fineract.portfolio.tax.data;

import java.util.List;

import org.apache.fineract.infrastructure.core.data.EnumOptionData;
import org.apache.fineract.portfolio.tax.data.TaxComponentData;

public class HabileTaxComponentData {

	private List<EnumOptionData> taxComponentTypes;
	private EnumOptionData taxComponentType;
	private TaxComponentData taxComponentData;

	public TaxComponentData getTaxComponentData() {
		return taxComponentData;
	}

	public void setTaxComponentData(TaxComponentData taxComponentData) {
		this.taxComponentData = taxComponentData;
	}

	public EnumOptionData getTaxComponentType() {
		return taxComponentType;
	}

	public void setTaxComponentType(EnumOptionData taxComponentType) {
		this.taxComponentType = taxComponentType;
	}

	public List<EnumOptionData> getTaxComponentTypes() {
		return taxComponentTypes;
	}

	public void setTaxComponentTypes(List<EnumOptionData> taxComponentTypes) {
		this.taxComponentTypes = taxComponentTypes;
	}

	public HabileTaxComponentData(TaxComponentData taxComponentData, EnumOptionData taxComponentType) {
		this.taxComponentData = taxComponentData;
		this.taxComponentType = taxComponentType;
	}

	public HabileTaxComponentData() {

	}

}
