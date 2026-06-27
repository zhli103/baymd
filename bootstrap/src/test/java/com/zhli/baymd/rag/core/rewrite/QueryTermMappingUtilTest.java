package com.zhli.baymd.rag.core.rewrite;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("查询词映射工具")
class QueryTermMappingUtilTest {

    @Test
    @DisplayName("null / 空文本 → 原样返回")
    void nullOrEmptyInput() {
        assertThat(QueryTermMappingUtil.applyMapping(null, "旧", "新")).isNull();
        assertThat(QueryTermMappingUtil.applyMapping("", "旧", "新")).isEqualTo("");
    }

    @Test
    @DisplayName("sourceTerm 为 null/空 → 原样返回")
    void nullOrEmptySource() {
        assertThat(QueryTermMappingUtil.applyMapping("hello", null, "新")).isEqualTo("hello");
        assertThat(QueryTermMappingUtil.applyMapping("hello", "", "新")).isEqualTo("hello");
    }

    @Test
    @DisplayName("简单替换：平安 → 平安保司")
    void simpleReplace() {
        assertThat(QueryTermMappingUtil.applyMapping("平安保险", "平安", "平安保司"))
                .isEqualTo("平安保司保险");
    }

    @Test
    @DisplayName("多处出现 → 全部替换")
    void replaceAllOccurrences() {
        assertThat(QueryTermMappingUtil.applyMapping("平安理赔平安客服", "平安", "平安保司"))
                .isEqualTo("平安保司理赔平安保司客服");
    }

    @Test
    @DisplayName("已是目标词 → 不重复替换（防止平安保司 → 平安保司保司）")
    void skipAlreadyTarget() {
        assertThat(QueryTermMappingUtil.applyMapping("平安保司保险", "平安", "平安保司"))
                .isEqualTo("平安保司保险"); // 不变！
    }

    @Test
    @DisplayName("部分已替换 + 部分未替换")
    void partialAlreadyTarget() {
        assertThat(QueryTermMappingUtil.applyMapping("平安保司理赔平安客服", "平安", "平安保司"))
                .isEqualTo("平安保司理赔平安保司客服");
    }

    @Test
    @DisplayName("不出现 sourceTerm → 原样返回")
    void noMatch() {
        assertThat(QueryTermMappingUtil.applyMapping("健康饮食", "平安", "平安保司"))
                .isEqualTo("健康饮食");
    }
}
