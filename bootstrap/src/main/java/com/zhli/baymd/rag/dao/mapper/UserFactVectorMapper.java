package com.zhli.baymd.rag.dao.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户事实向量 Mapper — pgvector 原生 SQL 操作。
 */
@Mapper
public interface UserFactVectorMapper {

    /** 插入向量（使用 pgvector ?::vector 语法） */
    @Insert("INSERT INTO t_user_fact_vector (id, embedding) VALUES (#{id}, #{embedding}::vector)")
    void insert(@Param("id") String id, @Param("embedding") String embedding);

    /** 按余弦距离检索 topK 条（按 user_id 过滤） */
    @Select("SELECT v.id, f.fact_text, f.fact_type, f.confidence, 1 - (v.embedding <=> #{embedding}::vector) AS similarity "
            + "FROM t_user_fact_vector v JOIN t_user_fact f ON v.id = f.id "
            + "WHERE f.user_id = #{userId} "
            + "ORDER BY v.embedding <=> #{embedding}::vector "
            + "LIMIT #{limit}")
    List<FactVectorResult> searchSimilar(@Param("userId") String userId,
                                          @Param("embedding") String embedding,
                                          @Param("limit") int limit);

    /** 删除指定 user_id 的所有向量 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM t_user_fact_vector WHERE id = #{id}")
    void deleteById(@Param("id") String id);

    @org.apache.ibatis.annotations.Delete(
            "DELETE FROM t_user_fact_vector WHERE id IN (SELECT id FROM t_user_fact WHERE user_id = #{userId})")
    void deleteByUserId(@Param("userId") String userId);

    record FactVectorResult(String id, String factText, String factType,
                            Float confidence, Double similarity) {}
}
