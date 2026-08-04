package com.chachamaru.harness.workflow.agent.framework;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("AgentType 测试")
public class AgentTypeTest {

    @Test
    @DisplayName("应该有 WORKER 类型")
    public void testWorkerType() {
        assertEquals("WORKER", AgentType.WORKER.name());
        assertEquals("工作代理", AgentType.WORKER.getDisplayName());
        assertEquals("执行具体任务的代理", AgentType.WORKER.getDescription());
    }

    @Test
    @DisplayName("应该有 REVIEWER 类型")
    public void testReviewerType() {
        assertEquals("REVIEWER", AgentType.REVIEWER.name());
        assertEquals("审查代理", AgentType.REVIEWER.getDisplayName());
    }

    @Test
    @DisplayName("应该有 ADVISOR 类型")
    public void testAdvisorType() {
        assertEquals("ADVISOR", AgentType.ADVISOR.name());
        assertEquals("顾问代理", AgentType.ADVISOR.getDisplayName());
    }

    @Test
    @DisplayName("应该有 PLANNER 和 CRITIC 类型（为阶段2+预留）")
    public void testFutureTypes() {
        assertEquals("PLANNER", AgentType.PLANNER.name());
        assertEquals("CRITIC", AgentType.CRITIC.name());
    }
}