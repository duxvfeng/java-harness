package com.chachamaru.harness.service.mapper;

import com.chachamaru.harness.service.domain.Session;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Optional;

/**
 * MyBatis Mapper for Session operations
 */
@Mapper
public interface SessionMapper {

    @Insert("INSERT INTO sessions (id, project_root, created_at, updated_at, metadata) " +
            "VALUES (#{id}, #{projectRoot}, #{createdAt}, #{updatedAt}, #{metadata})")
    int insert(Session session);

    @Select("SELECT * FROM sessions WHERE id = #{id}")
    Optional<Session> findById(String id);

    @Select("SELECT * FROM sessions WHERE project_root = #{projectRoot}")
    List<Session> findByProjectRoot(String projectRoot);

    @Update("UPDATE sessions SET updated_at = #{updatedAt}, metadata = #{metadata} WHERE id = #{id}")
    int update(Session session);

    @Delete("DELETE FROM sessions WHERE id = #{id}")
    int deleteById(String id);
}
