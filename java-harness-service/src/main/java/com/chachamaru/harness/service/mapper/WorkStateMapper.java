package com.chachamaru.harness.service.mapper;

import com.chachamaru.harness.service.domain.WorkState;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper for WorkState operations
 */
@Mapper
public interface WorkStateMapper {

    @Insert("INSERT INTO work_states (id, session_id, status, created_at, updated_at, expires_at, metadata) " +
            "VALUES (#{id}, #{sessionId}, #{status}, #{createdAt}, #{updatedAt}, #{expiresAt}, #{metadata})")
    int insert(WorkState workState);

    @Select("SELECT * FROM work_states WHERE id = #{id}")
    Optional<WorkState> findById(String id);

    @Select("SELECT * FROM work_states WHERE session_id = #{sessionId}")
    List<WorkState> findBySessionId(String sessionId);

    @Select("SELECT * FROM work_states WHERE session_id = #{sessionId} AND status = #{status}")
    List<WorkState> findBySessionIdAndStatus(@Param("sessionId") String sessionId, @Param("status") String status);

    @Update("UPDATE work_states SET updated_at = #{updatedAt}, status = #{status}, metadata = #{metadata} WHERE id = #{id}")
    int update(WorkState workState);

    @Delete("DELETE FROM work_states WHERE id = #{id}")
    int deleteById(String id);
}
