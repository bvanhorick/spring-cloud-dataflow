/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.dataflow.server.audit.service;


import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import org.springframework.cloud.dataflow.audit.repository.AuditRecordRepository;
import org.springframework.cloud.dataflow.audit.service.AuditRecordService;
import org.springframework.cloud.dataflow.audit.service.DefaultAuditRecordService;
import org.springframework.cloud.dataflow.core.AuditActionType;
import org.springframework.cloud.dataflow.core.AuditOperationType;
import org.springframework.cloud.dataflow.core.AuditRecord;
import org.springframework.data.domain.PageRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * @author Gunnar Hillert
 */
public class DefaultAuditRecordServiceTests {

	private AuditRecordRepository auditRecordRepository;

	@Before
	public void setupMock() {
		this.auditRecordRepository = mock(AuditRecordRepository.class);
	}

	@Test
	public void testInitializationWithNullParameters() {
		try {
			new DefaultAuditRecordService(null);
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditRecordRepository must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecord() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);
		auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", "my data");

		final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
		verify(this.auditRecordRepository, times(1)).save(argument.capture());
		verifyNoMoreInteractions(this.auditRecordRepository);

		AuditRecord auditRecord = argument.getValue();

		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals("1234", auditRecord.getCorrelationId());
		assertEquals("my data", auditRecord.getAuditData());
	}


	@Test
	public void testPopulateAndSaveAuditRecordWithNullAuditActionType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

		try {
			auditRecordService.populateAndSaveAuditRecord(AuditOperationType.SCHEDULE, null, "1234", "my audit data");
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditActionType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithNullAuditOperationType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

		try {
			auditRecordService.populateAndSaveAuditRecord(null, AuditActionType.CREATE, "1234", "my audit data");
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditOperationType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordWithMapData() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo1", "bar1");
		mapAuditData.put("foofoo", "barbar");

		auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", mapAuditData);

		final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
		verify(this.auditRecordRepository, times(1)).save(argument.capture());
		verifyNoMoreInteractions(this.auditRecordRepository);

		final AuditRecord auditRecord = argument.getValue();

		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals("1234", auditRecord.getCorrelationId());
		assertEquals("{\"foofoo\":\"barbar\",\"foo1\":\"bar1\"}", auditRecord.getAuditData());
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithNullAuditActionType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, null, "1234", mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditActionType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataWithNullAuditOperationType() {
		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository);

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		try {
			auditRecordService.populateAndSaveAuditRecordUsingMapData(null, AuditActionType.CREATE, "1234", mapAuditData);
		}
		catch (IllegalArgumentException e) {
			assertEquals("auditOperationType must not be null.", e.getMessage());
			return;
		}
		fail("Expected an Exception to be thrown.");
	}

	@Test
	public void testPopulateAndSaveAuditRecordUsingMapDataThrowingJsonProcessingException() throws JsonProcessingException {
		final ObjectMapper objectMapper = mock(ObjectMapper.class);
		when(objectMapper.writeValueAsString(any(Object.class))).thenThrow(new JsonProcessingException("Error"){
			private static final long serialVersionUID = 1L;
		});

		final AuditRecordService auditRecordService = new DefaultAuditRecordService(this.auditRecordRepository, objectMapper);

		final Map<String, Object> mapAuditData = new HashMap<>(2);
		mapAuditData.put("foo", "bar");

		auditRecordService.populateAndSaveAuditRecordUsingMapData(AuditOperationType.SCHEDULE, AuditActionType.CREATE, "1234", mapAuditData);

		final ArgumentCaptor<AuditRecord> argument = ArgumentCaptor.forClass(AuditRecord.class);
		verify(this.auditRecordRepository, times(1)).save(argument.capture());
		verifyNoMoreInteractions(this.auditRecordRepository);

		AuditRecord auditRecord = argument.getValue();

		assertEquals(AuditActionType.CREATE, auditRecord.getAuditAction());
		assertEquals(AuditOperationType.SCHEDULE, auditRecord.getAuditOperation());
		assertEquals("1234", auditRecord.getCorrelationId());
		assertEquals("Error serializing audit record data.  Data = {foo=bar}", auditRecord.getAuditData());
	}

	@Test
	public void testFindAuditRecordByAuditOperationTypeAndAuditActionType() {
		AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

		AuditActionType[] auditActionTypes = {AuditActionType.CREATE};
		AuditOperationType[] auditOperationTypes = {AuditOperationType.STREAM};
		PageRequest pageRequest = PageRequest.of(0, 1);
		auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionType(pageRequest, auditActionTypes, auditOperationTypes);

		verify(this.auditRecordRepository, times(1)).findByAuditOperationInAndAuditActionIn(eq(auditOperationTypes),eq(auditActionTypes),eq(pageRequest));
		verifyNoMoreInteractions(this.auditRecordRepository);
	}

	@Test
	public void testFindAuditRecordByAuditOperationTypeAndAuditActionTypeWithNullAuditActionType() {
		AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

		AuditOperationType[] auditOperationTypes = {AuditOperationType.STREAM};
		PageRequest pageRequest = PageRequest.of(0, 1);
		auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionType(pageRequest, null, auditOperationTypes);

		verify(this.auditRecordRepository, times(1)).findByAuditOperationIn(eq(auditOperationTypes), eq(pageRequest));
		verifyNoMoreInteractions(this.auditRecordRepository);
	}

	@Test
	public void testFindAuditRecordByAuditOperationTypeAndAuditActionTypeWithNullOperationType() {
		AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

		AuditActionType[] auditActionTypes = {AuditActionType.CREATE};
		PageRequest pageRequest = PageRequest.of(0, 1);
		auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionType(pageRequest, auditActionTypes, null);

		verify(this.auditRecordRepository, times(1)).findByAuditActionIn(eq(auditActionTypes), eq(pageRequest));
		verifyNoMoreInteractions(this.auditRecordRepository);
	}

	@Test
	public void testFindAuditRecordByAuditOperationTypeAndAuditActionTypeWithNullActionAndOperationType() {
		AuditRecordService auditRecordService = new DefaultAuditRecordService(auditRecordRepository);

		PageRequest pageRequest = PageRequest.of(0, 1);
		auditRecordService.findAuditRecordByAuditOperationTypeAndAuditActionType(pageRequest, null, null);

		verify(this.auditRecordRepository, times(1)).findAll(eq(pageRequest));
		verifyNoMoreInteractions(this.auditRecordRepository);
	}
}