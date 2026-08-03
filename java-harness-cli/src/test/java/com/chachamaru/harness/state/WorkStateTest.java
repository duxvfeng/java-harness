package com.chachamaru.harness.state;

import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class WorkStateTest {
    @Test
    void testCreateWorkState() {
        WorkState state = new WorkState();
        assertNotNull(state);
        assertNotNull(state.getWorkId());
        assertNotNull(state.getStartTime());
    }

    @Test
    void testSetWorkStatus() {
        WorkState state = new WorkState();
        state.setStatus(WorkState.Status.IN_PROGRESS);
        assertEquals(WorkState.Status.IN_PROGRESS, state.getStatus());

        state.setStatus(WorkState.Status.COMPLETED);
        assertEquals(WorkState.Status.COMPLETED, state.getStatus());
    }

    @Test
    void testAddWorkItem() {
        WorkState state = new WorkState();
        state.addWorkItem("task-1", "Implement feature X", "TODO");

        Map<String, WorkState.WorkItem> items = state.getWorkItems();
        assertEquals(1, items.size());
        assertTrue(items.containsKey("task-1"));
        assertEquals("Implement feature X", items.get("task-1").getDescription());
    }

    @Test
    void testUpdateWorkItem() {
        WorkState state = new WorkState();
        state.addWorkItem("task-1", "Implement feature X", "TODO");
        state.updateWorkItemStatus("task-1", "DONE");

        Map<String, WorkState.WorkItem> items = state.getWorkItems();
        assertEquals("DONE", items.get("task-1").getStatus());
    }

    @Test
    void testToJson() {
        WorkState state = new WorkState();
        state.setStatus(WorkState.Status.IN_PROGRESS);
        state.addWorkItem("task-1", "Test task", "TODO");

        String json = state.toJson();
        assertNotNull(json);
        assertTrue(json.contains("task-1"));
        assertTrue(json.contains("Test task"));
    }

    @Test
    void testFromJson() {
        WorkState original = new WorkState();
        original.setStatus(WorkState.Status.IN_PROGRESS);
        original.addWorkItem("task-1", "Test task", "TODO");

        String json = original.toJson();
        WorkState restored = WorkState.fromJson(json);

        assertEquals(original.getWorkId(), restored.getWorkId());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(1, restored.getWorkItems().size());
    }

    @Test
    void testGetWorkDuration() {
        WorkState state = new WorkState();
        // Just test that duration can be calculated
        long duration = state.getDuration();
        assertTrue(duration >= 0);
    }

    @Test
    void testGetProgress() {
        WorkState state = new WorkState();
        state.addWorkItem("task-1", "Task 1", "DONE");
        state.addWorkItem("task-2", "Task 2", "TODO");
        state.addWorkItem("task-3", "Task 3", "DONE");

        double progress = state.getProgress();
        assertEquals(2.0 / 3.0, progress, 0.001);
    }
}
