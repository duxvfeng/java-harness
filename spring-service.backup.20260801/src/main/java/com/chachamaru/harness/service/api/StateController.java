package com.chachamaru.harness.service.api;

import com.chachamaru.harness.service.dto.StateQueryRequest;
import com.chachamaru.harness.service.dto.StateQueryResponse;
import com.chachamaru.harness.service.domain.Session;
import com.chachamaru.harness.service.domain.WorkState;
import com.chachamaru.harness.service.service.StateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for state management
 */
@RestController
@RequestMapping("/api/state")
public class StateController {
    private static final Logger log = LoggerFactory.getLogger(StateController.class);

    private final StateService stateService;

    public StateController(StateService stateService) {
        this.stateService = stateService;
    }

    @PostMapping("/query")
    public ResponseEntity<StateQueryResponse> queryState(@RequestBody StateQueryRequest request) {
        String requestId = UUID.randomUUID().toString();
        
        try {
            log.debug("Received state query: sessionId={}, queryType={}", 
                request.sessionId(), request.queryType());
            
            Object data = switch (request.queryType()) {
                case "SESSION" -> stateService.getSession(request.sessionId())
                    .orElse(null);
                case "WORK_STATES" -> stateService.getWorkStatesBySession(request.sessionId());
                default -> throw new IllegalArgumentException("Unknown query type: " + request.queryType());
            };
            
            return ResponseEntity.ok(StateQueryResponse.success(requestId, data));
        } catch (Exception e) {
            log.error("State query failed: {}", e.getMessage(), e);
            return ResponseEntity.ok(StateQueryResponse.error(requestId, e.getMessage()));
        }
    }

    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(@RequestBody Map<String, String> request) {
        String projectRoot = request.get("projectRoot");
        Session session = stateService.createSession(projectRoot);
        return ResponseEntity.ok(session);
    }

    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<Session> getSession(@PathVariable String sessionId) {
        return stateService.getSession(sessionId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/work-states")
    public ResponseEntity<WorkState> createWorkState(@RequestBody Map<String, String> request) {
        String sessionId = request.get("sessionId");
        String status = request.get("status");
        WorkState workState = stateService.createWorkState(sessionId, status);
        return ResponseEntity.ok(workState);
    }

    @GetMapping("/work-states/{sessionId}")
    public ResponseEntity<List<WorkState>> getWorkStates(@PathVariable String sessionId) {
        List<WorkState> workStates = stateService.getWorkStatesBySession(sessionId);
        return ResponseEntity.ok(workStates);
    }
}
