package com.zhli.baymd.rag.dao.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserEpisodeVectorMapper {

    @Insert("INSERT INTO t_user_episode_vector (id, embedding) VALUES (#{id}, #{embedding}::vector)")
    void insert(@Param("id") String id, @Param("embedding") String embedding);

    @Select("SELECT v.id, e.title, e.summary, 1 - (v.embedding <=> #{embedding}::vector) AS similarity "
            + "FROM t_user_episode_vector v JOIN t_user_episode e ON v.id = e.id "
            + "WHERE e.user_id = #{userId} "
            + "ORDER BY v.embedding <=> #{embedding}::vector "
            + "LIMIT #{limit}")
    List<EpisodeVectorResult> searchSimilar(@Param("userId") String userId,
                                             @Param("embedding") String embedding,
                                             @Param("limit") int limit);

    @Delete("DELETE FROM t_user_episode_vector WHERE id = #{id}")
    void deleteById(@Param("id") String id);

    @Delete("DELETE FROM t_user_episode_vector WHERE id IN "
            + "(SELECT id FROM t_user_episode WHERE user_id = #{userId})")
    void deleteByUserId(@Param("userId") String userId);

    record EpisodeVectorResult(String id, String title, String summary, Double similarity) {}
}
