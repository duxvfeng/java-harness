package com.chachamaru.harness.service;

import com.chachamaru.harness.service.domain.Session;
import com.chachamaru.harness.service.domain.WorkState;
import com.chachamaru.harness.service.service.StateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class StateServiceTest {

    @Autowired
    private StateService stateService;

    @Test
    void testCreateAndRetrieveSession() {
        Session session = stateService.createSession("/test/project");
        
        assertNotNull(session.getId());
        assertEquals("/test/project", session.getProjectRoot());
        
        Optional<Session> retrieved = stateService.getSession(session.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(session.getId(), retrieved.get().getId());
    }

    @Test
    void testCreateAndRetrieveWorkState() {
        Session session = stateService.createSession("/test/project");
        WorkState workState = stateService.createWorkState(session.getId(), "pending");
        
        assertNotNull(workState.getId());
        assertEquals(session.getId(), workState.getSessionId());
        assertEquals("pending", workState.getStatus());
        
        Optional<WorkState> retrieved = stateService.getWorkState(workState.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(workState.getId(), retrieved.get().getId());
    }

    @Test
    void testGetWorkStatesBySession() {
        Session session = stateService.createSession("/test/project");
        
        stateService.createWorkState(session.getId(), "pending");
        stateService.createWorkState(session.getId(), "running");
        
        var workStates = stateService.getWorkStatesBySession(session.getId());
        assertEquals(2, workStates.size());
    }
}
