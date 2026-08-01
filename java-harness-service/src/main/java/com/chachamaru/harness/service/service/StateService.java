package com.chachamaru.harness.service.service;

import com.chachamaru.harness.service.domain.Session;
import com.chachamaru.harness.service.domain.WorkState;
import com.chachamaru.harness.service.mapper.SessionMapper;
import com.chachamaru.harness.service.mapper.WorkStateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * State management service
 */
@Service
public class StateService {
    private static final Logger log = LoggerFactory.getLogger(StateService.class);

    private final SessionMapper sessionMapper;
    private final WorkStateMapper workStateMapper;

    public StateService(SessionMapper sessionMapper, WorkStateMapper workStateMapper) {
        this.sessionMapper = sessionMapper;
        this.workStateMapper = workStateMapper;
    }

    // Session operations
    @Transactional
    public Session createSession(String projectRoot) {
        String sessionId = UUID.randomUUID().toString();
        Session session = new Session(sessionId, projectRoot);
        sessionMapper.insert(session);
        log.info("Created session: {} for project: {}", sessionId, projectRoot);
        return session;
    }

    public Optional<Session> getSession(String sessionId) {
        return sessionMapper.findById(sessionId);
    }

    @Transactional
    public Session updateSession(String sessionId, String metadata) {
        Session session = sessionMapper.findById(sessionId)
            .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        session.setUpdatedAt(LocalDateTime.now());
        session.setMetadata(metadata);
        sessionMapper.update(session);
        return session;
    }

    @Transactional
    public void deleteSession(String sessionId) {
        sessionMapper.deleteById(sessionId);
        log.info("Deleted session: {}", sessionId);
    }

    // WorkState operations
    @Transactional
    public WorkState createWorkState(String sessionId, String status) {
        String workStateId = UUID.randomUUID().toString();
        WorkState workState = new WorkState(workStateId, sessionId, status);
        workStateMapper.insert(workState);
        log.info("Created work state: {} for session: {}", workStateId, sessionId);
        return workState;
    }

    public Optional<WorkState> getWorkState(String workStateId) {
        return workStateMapper.findById(workStateId);
    }

    public List<WorkState> getWorkStatesBySession(String sessionId) {
        return workStateMapper.findBySessionId(sessionId);
    }

    @Transactional
    public WorkState updateWorkState(String workStateId, String status, String metadata) {
        WorkState workState = workStateMapper.findById(workStateId)
            .orElseThrow(() -> new IllegalArgumentException("WorkState not found: " + workStateId));
        workState.setUpdatedAt(LocalDateTime.now());
        workState.setStatus(status);
        workState.setMetadata(metadata);
        workStateMapper.update(workState);
        return workState;
    }

    @Transactional
    public void deleteWorkState(String workStateId) {
        workStateMapper.deleteById(workStateId);
        log.info("Deleted work state: {}", workStateId);
    }
}
