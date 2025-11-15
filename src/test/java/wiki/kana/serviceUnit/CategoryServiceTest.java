package wiki.kana.serviceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import wiki.kana.entity.Category;
import wiki.kana.exception.DuplicateResourceException;
import wiki.kana.exception.ResourceNotFoundException;
import wiki.kana.repository.CategoryRepository;
import wiki.kana.service.CategoryService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("findById should return category when exists")
    void findById_success() {
        Category category = Category.builder().id(1L).name("Java").slug("java").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        Category result = categoryService.findById(1L);

        assertThat(result).isEqualTo(category);
        verify(categoryRepository).findById(1L);
    }

    @Test
    @DisplayName("findById should throw when not exists")
    void findById_notFound() {
        when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("createCategory should check duplicate name")
    void createCategory_duplicateName() {
        Category category = Category.builder().name("Java").build();
        when(categoryRepository.findByName("Java")).thenReturn(Optional.of(new Category()));

        assertThatThrownBy(() -> categoryService.createCategory(category, null))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("name");
    }

    @Test
    @DisplayName("createCategory should generate slug and save")
    void createCategory_success() {
        Category category = Category.builder().name("Spring Boot").build();
        when(categoryRepository.findByName("Spring Boot")).thenReturn(Optional.empty());
        when(categoryRepository.findBySlug("spring-boot")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
            Category saved = invocation.getArgument(0);
            saved.setId(10L);
            return saved;
        });

        Category saved = categoryService.createCategory(category, null);

        assertThat(saved.getId()).isEqualTo(10L);
        assertThat(saved.getSlug()).isEqualTo("spring-boot");
        assertThat(saved.getIsActive()).isTrue();
        verify(categoryRepository).save(category);
    }

    @Test
    @DisplayName("updateCategory should throw when slug duplicated")
    void updateCategory_duplicateSlug() {
        Category existing = Category.builder().id(1L).name("Java").slug("java").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.findBySlug("backend")).thenReturn(Optional.of(new Category()));

        Category update = Category.builder().slug("backend").build();

        assertThatThrownBy(() -> categoryService.updateCategory(1L, update, null))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("slug");
    }

    @Test
    @DisplayName("updateCategory should update fields")
    void updateCategory_success() {
        Category existing = Category.builder().id(1L).name("Java").slug("java").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Category update = Category.builder()
                .name("Backend")
                .description("desc")
                .slug("backend")
                .sortOrder(5)
                .isActive(false)
                .build();

        Category result = categoryService.updateCategory(1L, update, null);

        assertThat(result.getName()).isEqualTo("Backend");
        assertThat(result.getDescription()).isEqualTo("desc");
        assertThat(result.getSlug()).isEqualTo("backend");
        assertThat(result.getSortOrder()).isEqualTo(5);
        assertThat(result.getIsActive()).isFalse();
    }

    @Nested
    class StatusManagement {
        @Test
        @DisplayName("activateCategory sets isActive to true")
        void activateCategory() {
            Category category = Category.builder().id(1L).name("Java").isActive(false).build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Category result = categoryService.activateCategory(1L);

            assertThat(result.getIsActive()).isTrue();
            verify(categoryRepository).save(category);
        }

        @Test
        @DisplayName("deactivateCategory sets isActive to false")
        void deactivateCategory() {
            Category category = Category.builder().id(1L).name("Java").isActive(true).build();
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Category result = categoryService.deactivateCategory(1L);

            assertThat(result.getIsActive()).isFalse();
            verify(categoryRepository).save(category);
        }
    }

    @Test
    @DisplayName("deleteCategory should check children and posts")
    void deleteCategory_checksRelations() {
        Category category = Category.builder().id(1L).name("Java").build();
        category.setChildren(List.of(Category.builder().id(2L).build()));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

        assertThatThrownBy(() -> categoryService.deleteCategory(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("child categories");
    }

    @Test
    @DisplayName("countPublishedPosts delegates to repository")
    void countPublishedPosts() {
        Category category = Category.builder().id(1L).name("Java").build();
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(categoryRepository.countPublishedPostsByCategory(category)).thenReturn(3L);

        long count = categoryService.countPublishedPosts(1L);

        assertThat(count).isEqualTo(3L);
        verify(categoryRepository).countPublishedPostsByCategory(category);
    }
}

