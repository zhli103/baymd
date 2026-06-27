package com.zhli.baymd.rag.core.intent;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.zhli.baymd.rag.enums.IntentLevel;

import java.util.List;

import static com.zhli.baymd.rag.enums.IntentLevel.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * IntentTreeFactory 单元测试（纯逻辑）
 * <p>
 * 测试默认意图树的构建结果：节点数量、层级关系、父子关联
 */
@DisplayName("意图树工厂 — IntentTreeFactory")
class IntentTreeFactoryTest {

    private final List<IntentNode> tree = IntentTreeFactory.buildIntentTree();

    // ==================== 顶层结构 ====================

    @Test
    @DisplayName("构建出 2 个根节点：医学健康 + 系统交互")
    void twoRootDomains() {
        assertThat(tree).hasSize(2);
        assertThat(tree).extracting(IntentNode::getName)
                .containsExactly("医学健康", "系统交互");
    }

    @Test
    @DisplayName("根节点层级为 DOMAIN")
    void rootsAreDomainLevel() {
        assertThat(tree).allMatch(n -> n.getLevel() == DOMAIN);
    }

    // ==================== 医学健康 → 7 个分类 ====================

    @Test
    @DisplayName("医学健康下挂 7 个 CATEGORY")
    void medicalHasSevenCategories() {
        IntentNode medical = findBy(tree, "医学健康");
        assertThat(medical.getChildren()).hasSize(7);
        assertThat(medical.getChildren()).allMatch(c -> c.getLevel() == CATEGORY);
    }

    @Test
    @DisplayName("7 个分类名称齐全")
    void categoryNames() {
        IntentNode medical = findBy(tree, "医学健康");
        assertThat(medical.getChildren()).extracting(IntentNode::getName)
                .containsExactly(
                        "科室推荐", "症状自查", "药物查询", "饮食建议",
                        "中医辨证", "报告解读", "医院推荐"
                );
    }

    // ==================== 叶子节点 ====================

    @Test
    @DisplayName("每个 TOPIC 节点都是叶子节点（isLeaf = true）")
    void allTopicsAreLeaves() {
        List<IntentNode> topics = collectByLevel(tree, TOPIC);
        assertThat(topics).isNotEmpty();
        assertThat(topics).allMatch(IntentNode::isLeaf);
    }

    @Test
    @DisplayName("DOMAIN 节点不是叶子，有子节点的 CATEGORY 也不是叶子")
    void domainsAndCategoriesWithChildrenNotLeaves() {
        // 两个 DOMAIN 都不是叶子
        for (IntentNode domain : tree) {
            assertThat(domain.isLeaf()).isFalse();
        }
        // 医学健康的 7 个 CATEGORY 都有子 TOPIC → 不是叶子
        IntentNode medical = findBy(tree, "医学健康");
        assertThat(medical.getChildren()).allMatch(c -> !c.isLeaf());
    }

    // ==================== fullPath ====================

    @Test
    @DisplayName("fullPath 包含完整层级路径")
    void fullPathContainsHierarchy() {
        IntentNode node = findByDeep(tree, "西药查询");
        assertThat(node).isNotNull();
        assertThat(node.getFullPath()).isEqualTo("医学健康 > 药物查询 > 西药查询");
    }

    // ==================== IntentKind ====================

    @Test
    @DisplayName("医学健康节点均为 KB 类型")
    void medicalNodesAreKB() {
        IntentNode medical = findBy(tree, "医学健康");
        assertThat(medical.isKB()).isTrue();
        for (IntentNode cat : medical.getChildren()) {
            assertThat(cat.isKB()).isTrue();
            for (IntentNode topic : cat.getChildren()) {
                assertThat(topic.isKB()).isTrue();
            }
        }
    }

    @Test
    @DisplayName("系统交互节点为 SYSTEM 类型")
    void systemNodesAreSystem() {
        IntentNode sys = findBy(tree, "系统交互");
        assertThat(sys.isSystem()).isTrue();
        assertThat(sys.getChildren()).allMatch(IntentNode::isSystem);
    }

    // ==================== KB ID 关联 ====================

    @Test
    @DisplayName("医学健康及其子节点挂载了 KB ID")
    void medicalNodesHaveKbId() {
        IntentNode medical = findBy(tree, "医学健康");
        assertThat(medical.getKbId()).isNotEmpty();
        for (IntentNode cat : medical.getChildren()) {
            assertThat(cat.getKbId()).isNotEmpty();
        }
    }

    @Test
    @DisplayName("系统交互节点无 KB ID")
    void systemNodesNoKbId() {
        IntentNode sys = findBy(tree, "系统交互");
        assertThat(sys.getKbId()).isNull();
    }

    // ==================== 示例问题 ====================

    @Test
    @DisplayName("每个 CATEGORY 节点都有示例问题")
    void categoriesHaveExamples() {
        IntentNode medical = findBy(tree, "医学健康");
        assertThat(medical.getChildren()).allMatch(c -> !c.getExamples().isEmpty());
    }

    // ==================== 工具方法 ====================

    private IntentNode findBy(List<IntentNode> nodes, String name) {
        return nodes.stream().filter(n -> n.getName().equals(name)).findFirst().orElseThrow();
    }

    private IntentNode findByDeep(List<IntentNode> nodes, String name) {
        for (IntentNode n : nodes) {
            if (n.getName().equals(name)) return n;
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                IntentNode found = findByDeep(n.getChildren(), name);
                if (found != null) return found;
            }
        }
        return null;
    }

    private List<IntentNode> collectByLevel(List<IntentNode> nodes, IntentLevel level) {
        List<IntentNode> result = new java.util.ArrayList<>();
        for (IntentNode n : nodes) {
            if (n.getLevel() == level) result.add(n);
            if (n.getChildren() != null) result.addAll(collectByLevel(n.getChildren(), level));
        }
        return result;
    }
}
