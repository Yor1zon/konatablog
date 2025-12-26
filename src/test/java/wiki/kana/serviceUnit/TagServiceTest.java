package wiki.kana.serviceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import wiki.kana.entity.Post;
import wiki.kana.entity.Tag;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.PostRepository;
import wiki.kana.repository.TagRepository;
import wiki.kana.service.TagService;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@DisplayName("TagService 单元测试")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private TagService tagService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("成功创建标签")
    void shouldCreateTagSuccessfully() {
        // Given
        Tag tag = Tag.builder()
                .name("Java")
                .description("Java编程语言")
                .build();

        Tag savedTag = Tag.builder()
                .id(1L)
                .name("Java")
                .slug("java")
                .description("Java编程语言")
                .usageCount(0)
                .build();

        when(tagRepository.findByName("Java")).thenReturn(Optional.empty());
        when(tagRepository.findBySlug("java")).thenReturn(Optional.empty());
        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        // When
        Tag result = tagService.createTag(tag);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getSlug()).isEqualTo("java");
        assertThat(result.getDescription()).isEqualTo("Java编程语言");

        verify(tagRepository).findByName("Java");
        verify(tagRepository).findBySlug("java");
        verify(tagRepository).save(any(Tag.class));
    }

    @Test
    @DisplayName("创建重复标签应该抛出异常")
    void shouldThrowExceptionWhenCreatingDuplicateTag() {
        // Given
        Tag tag = Tag.builder()
                .name("Java")
                .description("Java编程语言")
                .build();

        Tag existingTag = Tag.builder()
                .id(1L)
                .name("Java")
                .build();

        when(tagRepository.findByName("Java")).thenReturn(Optional.of(existingTag));

        // When & Then
        assertThatThrownBy(() -> tagService.createTag(tag))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Tag name already exists");

        verify(tagRepository).findByName("Java");
        verify(tagRepository, never()).save(any(Tag.class));
    }

    @Test
    @DisplayName("根据ID查找标签")
    void shouldFindTagById() {
        // Given
        Tag tag = Tag.builder()
                .id(1L)
                .name("Java")
                .description("Java编程语言")
                .build();

        when(tagRepository.findById(1L)).thenReturn(Optional.of(tag));

        // When
        Tag result = tagService.findById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Java");

        verify(tagRepository).findById(1L);
    }

    @Test
    @DisplayName("查找不存在的标签应该抛出异常")
    void shouldThrowExceptionWhenFindingNonExistentTag() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> tagService.findById(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tag not found");

        verify(tagRepository).findById(1L);
    }

    @Test
    @DisplayName("查找所有标签")
    void shouldFindAllTags() {
        // Given
        List<Tag> tags = List.of(
                Tag.builder().id(1L).name("Java").build(),
                Tag.builder().id(2L).name("Spring").build()
        );

        when(tagRepository.findAll()).thenReturn(tags);

        // When
        List<Tag> result = tagService.findAll();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Java");
        assertThat(result.get(1).getName()).isEqualTo("Spring");

        verify(tagRepository).findAll();
    }

    @Test
    @DisplayName("根据名称查找标签")
    void shouldFindTagByName() {
        // Given
        Tag tag = Tag.builder()
                .id(1L)
                .name("Java")
                .build();

        when(tagRepository.findByName("Java")).thenReturn(Optional.of(tag));

        // When
        Tag result = tagService.findByName("Java");

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");

        verify(tagRepository).findByName("Java");
    }

    @Test
    @DisplayName("统计所有标签数量")
    void shouldCountAllTags() {
        // Given
        when(tagRepository.count()).thenReturn(10L);

        // When
        Long result = tagService.countAllTags();

        // Then
        assertThat(result).isEqualTo(10L);

        verify(tagRepository).count();
    }

    @Test
    @DisplayName("更新标签信息")
    void shouldUpdateTag() {
        // Given
        Tag existingTag = Tag.builder()
                .id(1L)
                .name("Java")
                .description("Java编程语言")
                .build();

        Tag updateData = Tag.builder()
                .name("Java 17")
                .description("Java 17编程语言")
                .build();

        when(tagRepository.findById(1L)).thenReturn(Optional.of(existingTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(existingTag);

        // When
        Tag result = tagService.updateTag(1L, updateData);

        // Then
        assertThat(result.getName()).isEqualTo("Java 17");
        assertThat(result.getDescription()).isEqualTo("Java 17编程语言");

        verify(tagRepository).findById(1L);
        verify(tagRepository).save(existingTag);
    }

    // ==================== 新增功能测试 ====================

    @Test
    @DisplayName("验证标签删除")
    void shouldValidateTagDeletion() {
        // Given
        Tag usedTag = Tag.builder()
                .id(1L)
                .name("Java")
                .usageCount(5)
                .build();

        when(tagRepository.findById(1L)).thenReturn(Optional.of(usedTag));

        // When
        Map<String, Object> result = tagService.validateTagDeletion(1L);

        // Then
        assertThat(result.get("canDelete")).isEqualTo(false);
        assertThat(result.get("usageCount")).isEqualTo(5);
        assertThat(result.get("tagName")).isEqualTo("Java");
        assertThat(result.get("forceDeleteOption")).isEqualTo(true);

        verify(tagRepository).findById(1L);
    }

    @Test
    @DisplayName("清理未使用的标签")
    void shouldCleanupUnusedTags() {
        // Given
        List<Tag> allTags = List.of(
                Tag.builder().id(1L).name("Java").usageCount(0).build(),
                Tag.builder().id(2L).name("Spring").usageCount(3).build(),
                Tag.builder().id(3L).name("MySQL").usageCount(0).build()
        );

        when(tagRepository.findAll()).thenReturn(allTags);
        doNothing().when(tagRepository).delete(any(Tag.class));

        // When
        int result = tagService.cleanupUnusedTags();

        // Then
        assertThat(result).isEqualTo(2);
        verify(tagRepository).findAll();
        verify(tagRepository, times(2)).delete(any(Tag.class));
    }

    @Test
    @DisplayName("验证标签名称有效性 - 有效名称")
    void shouldValidateValidTagName() {
        // Given
        when(tagRepository.findByName("新标签")).thenReturn(Optional.empty());

        // When
        Map<String, Object> result = tagService.validateTagName("新标签");

        // Then
        assertThat(result.get("valid")).isEqualTo(true);
        assertThat(result.get("suggestedSlug")).isEqualTo(""); // Chinese characters are removed

        verify(tagRepository).findByName("新标签");
    }

    @Test
    @DisplayName("验证标签名称有效性 - 无效名称（空）")
    void shouldValidateInvalidTagNameEmpty() {
        // When
        Map<String, Object> result = tagService.validateTagName("");

        // Then
        assertThat(result.get("valid")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("标签名称不能为空");

        verify(tagRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("验证标签名称有效性 - 名称已存在")
    void shouldValidateDuplicateTagName() {
        // Given
        Tag existingTag = Tag.builder().id(1L).name("Java").build();
        when(tagRepository.findByName("Java")).thenReturn(Optional.of(existingTag));

        // When
        Map<String, Object> result = tagService.validateTagName("Java");

        // Then
        assertThat(result.get("valid")).isEqualTo(false);
        assertThat(result.get("error")).isEqualTo("标签名称已存在");
        assertThat(result.get("existingTag")).isEqualTo(existingTag);

        verify(tagRepository).findByName("Java");
    }

    @Test
    @DisplayName("高级搜索标签")
    void shouldAdvancedSearchTags() {
        // Given
        List<Tag> allTags = List.of(
                Tag.builder().id(1L).name("Java").description("编程语言").usageCount(10).color("#FF0000").build(),
                Tag.builder().id(2L).name("Spring").description("框架").usageCount(5).color(null).build(),
                Tag.builder().id(3L).name("MySQL").description("数据库").usageCount(3).color("#00FF00").build()
        );

        when(tagRepository.findAll()).thenReturn(allTags);

        // When
        List<Tag> result = tagService.advancedSearch("Java", "语言", 5, 15, true, "usage", false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Java");

        verify(tagRepository).findAll();
    }

    @Test
    @DisplayName("获取标签云数据")
    void shouldGetTagCloudData() {
        // Given
        List<Tag> usedTags = List.of(
                Tag.builder().id(1L).name("Java").usageCount(10).color("#FF0000").build(),
                Tag.builder().id(2L).name("Spring").usageCount(5).color(null).build(),
                Tag.builder().id(3L).name("MySQL").usageCount(2).color("#00FF00").build()
        );

        when(tagRepository.findUsedTags()).thenReturn(usedTags);

        // When
        List<Map<String, Object>> result = tagService.getTagCloudData(10);

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).get("name")).isEqualTo("Java");
        assertThat(result.get(0).get("weight")).isEqualTo(5); // 最高的权重为5
        assertThat(result.get(2).get("weight")).isEqualTo(1); // 最低的权重为1

        verify(tagRepository).findUsedTags();
    }

    @Test
    @DisplayName("获取标签趋势分析")
    void shouldGetTagTrendAnalysis() {
        // Given
        List<Tag> allTags = List.of(
                Tag.builder().name("Java").usageCount(10).build(),
                Tag.builder().name("Spring").usageCount(0).build(),
                Tag.builder().name("MySQL").usageCount(2).build() // Changed to 2 to be in "rare" category
        );

        when(tagRepository.findAll()).thenReturn(allTags);

        // When
        Map<String, Object> result = tagService.getTagTrendAnalysis();

        // Then
        assertThat(result.get("totalTags")).isEqualTo(3L);
        assertThat(result.get("usedTags")).isEqualTo(2L);
        assertThat(result.get("unusedTags")).isEqualTo(1L);
        assertThat(result.get("usageRate")).isEqualTo(2.0/3.0);

        @SuppressWarnings("unchecked")
        Map<String, Long> distribution = (Map<String, Long>) result.get("usageDistribution");
        assertThat(distribution.get("unused")).isEqualTo(1);
        assertThat(distribution.get("rare")).isEqualTo(1);
        assertThat(distribution.get("popular")).isEqualTo(1);

        verify(tagRepository).findAll();
    }

    @Test
    @DisplayName("获取最近创建的标签")
    void shouldGetRecentlyCreatedTags() {
        // Given
        List<Tag> allTags = List.of(
                Tag.builder().id(1L).name("Java").createdAt(LocalDateTime.now().minusDays(5)).build(),
                Tag.builder().id(2L).name("Spring").createdAt(LocalDateTime.now().minusDays(1)).build(),
                Tag.builder().id(3L).name("MySQL").createdAt(LocalDateTime.now().minusDays(10)).build()
        );

        when(tagRepository.findAll()).thenReturn(allTags);

        // When
        List<Tag> result = tagService.getRecentlyCreatedTags(7, 10);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getName()).isEqualTo("Spring");
        assertThat(result.get(1).getName()).isEqualTo("Java");

        verify(tagRepository).findAll();
    }

    @Test
    @DisplayName("合并标签")
    void shouldMergeTags() {
        // Given
        Post post1 = Post.builder().id(1L).title("Java基础").tags(new ArrayList<>()).build();
        Post post2 = Post.builder().id(2L).title("Spring入门").tags(new ArrayList<>()).build();

        Tag sourceTag = Tag.builder()
                .id(1L)
                .name("Java")
                .usageCount(2)
                .posts(new ArrayList<>(Arrays.asList(post1, post2)))
                .build();

        Tag targetTag = Tag.builder()
                .id(2L)
                .name("Java编程")
                .usageCount(0)
                .posts(new ArrayList<>())
                .build();

        when(tagRepository.findById(1L)).thenReturn(Optional.of(sourceTag));
        when(tagRepository.findById(2L)).thenReturn(Optional.of(targetTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(targetTag);
        when(postRepository.save(any(Post.class))).thenReturn(post1);

        // When
        Tag result = tagService.mergeTags(1L, 2L);

        // Then
        assertThat(result.getName()).isEqualTo("Java编程");
        assertThat(result.getDescription()).contains("Merged from");
        verify(tagRepository).findById(1L);
        verify(tagRepository).findById(2L);
        verify(postRepository, times(2)).save(any(Post.class));
        verify(tagRepository, times(2)).save(any(Tag.class));
        verify(tagRepository).delete(sourceTag);
    }

    @Test
    @DisplayName("合并相同标签应该抛出异常")
    void shouldThrowExceptionWhenMergingSameTag() {
        // Given
        when(tagRepository.findById(1L)).thenReturn(Optional.of(
                Tag.builder().id(1L).name("Java").build()
        ));

        // When & Then
        assertThatThrownBy(() -> tagService.mergeTags(1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot merge tag with itself");
    }

    @Test
    @DisplayName("批量合并标签")
    void shouldBatchMergeTags() {
        // Given
        Post post = Post.builder().id(1L).title("Java基础").tags(new ArrayList<>()).build();

        Tag sourceTag1 = Tag.builder()
                .id(1L)
                .name("Java")
                .usageCount(1)
                .posts(new ArrayList<>(Arrays.asList(post)))
                .build();

        Tag sourceTag2 = Tag.builder()
                .id(2L)
                .name("Java编程")
                .usageCount(0)
                .posts(new ArrayList<>())
                .build();

        Tag targetTag = Tag.builder()
                .id(3L)
                .name("Java语言")
                .usageCount(0)
                .posts(new ArrayList<>())
                .build();

        when(tagRepository.findById(1L)).thenReturn(Optional.of(sourceTag1));
        when(tagRepository.findById(2L)).thenReturn(Optional.of(sourceTag2));
        when(tagRepository.findById(3L)).thenReturn(Optional.of(targetTag));
        when(tagRepository.save(any(Tag.class))).thenReturn(targetTag);
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // When
        Map<String, Object> result = tagService.batchMergeTags(Arrays.asList(1L, 2L), 3L);

        // Then
        assertThat(result.get("mergedCount")).isEqualTo(2);
        assertThat(result.get("targetTag")).isEqualTo("Java语言");
        assertThat(result.get("success")).isEqualTo(true);

        @SuppressWarnings("unchecked")
        List<String> mergedTags = (List<String>) result.get("mergedTags");
        assertThat(mergedTags).contains("Java", "Java编程");
    }

    @Test
    @DisplayName("建议标签合并")
    void shouldSuggestTagMerges() {
        // Given
        List<Tag> allTags = List.of(
                Tag.builder().id(1L).name("Java").usageCount(5).build(),
                Tag.builder().id(2L).name("java").usageCount(3).build(),
                Tag.builder().id(3L).name("Spring").usageCount(10).build()
        );

        when(tagRepository.findAll()).thenReturn(allTags);

        // When
        List<Map<String, Object>> result = tagService.suggestTagMerges(0.5);

        // Then
        assertThat(result).hasSize(1);
        Map<String, Object> suggestion = result.get(0);
        Double similarity = (Double) suggestion.get("similarity");
        assertThat(similarity).isGreaterThan(0.5);

        @SuppressWarnings("unchecked")
        Map<String, Object> tag1 = (Map<String, Object>) suggestion.get("tag1");
        assertThat(tag1.get("name")).isEqualTo("Java");

        verify(tagRepository).findAll();
    }
}