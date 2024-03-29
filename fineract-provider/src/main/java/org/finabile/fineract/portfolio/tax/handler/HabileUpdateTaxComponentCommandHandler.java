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
package org.finabile.fineract.portfolio.tax.handler;

import org.apache.fineract.commands.annotation.CommandType;
import org.apache.fineract.commands.handler.NewCommandSourceHandler;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.finabile.fineract.portfolio.tax.service.HabileTaxWritePlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

@Service
@Primary
@CommandType(entity = "HABILETAXCOMPONENT", action = "UPDATE")
public class HabileUpdateTaxComponentCommandHandler implements NewCommandSourceHandler {

	private final HabileTaxWritePlatformService taxWritePlatformService;

	@Autowired
	public HabileUpdateTaxComponentCommandHandler(final HabileTaxWritePlatformService taxWritePlatformService) {
		this.taxWritePlatformService = taxWritePlatformService;
	}

	@Override
	public CommandProcessingResult processCommand(JsonCommand jsonCommand) {
		return this.taxWritePlatformService.updateTaxComponent(jsonCommand.entityId(), jsonCommand);
	}

}
