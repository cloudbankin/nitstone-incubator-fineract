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
package org.apache.fineract.portfolio.address.service;

import java.util.Collection;

import org.apache.fineract.portfolio.address.data.AddressData;

public interface AddressReadPlatformService {
	public Collection<AddressData> retrieveAddressFields(long clientid);

	public Collection<AddressData> retrieveAllClientAddress(long clientid);

	public Collection<AddressData> retrieveAddressbyType(long clientid, long typeid);

	Collection<AddressData> retrieveAddressbyTypeAndStatus(long clientid, long typeid, String status);

	Collection<AddressData> retrieveAddressbyStatus(long clientid, String status);

	AddressData retrieveTemplate();

	Collection<AddressData> retrieveByEntityId(long entityId, String entityType);

	Collection<AddressData> retrieveAddressbyId(long typeid);

	// Habile changes retrive address data's based on address type
		public AddressData retrieveByEntityIdAndAddressType(final long entityId, String entityType,
				final String addressType);
		// Habile changes end
}
