package wiki.kana.serviceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import wiki.kana.entity.Tag;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.TagRepository;
import wiki.kana.service.TagService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("TagService 单元测试")
class TagServiceTest {

    @Mock
    private TagRepository tagRepository;

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
        when(tagRepository.save(any(Tag.class))).thenReturn(savedTag);

        // When
        Tag result = tagService.createTag(tag);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Java");
        assertThat(result.getSlug()).isEqualTo("java");
        assertThat(result.getDescription()).isEqualTo("Java编程语言");

        verify(tagRepository).findByName("Java");
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
}