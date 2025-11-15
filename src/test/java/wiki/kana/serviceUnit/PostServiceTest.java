package wiki.kana.serviceUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import wiki.kana.entity.Post;
import wiki.kana.entity.User;
import wiki.kana.repository.PostRepository;
import wiki.kana.repository.UserRepository;
import wiki.kana.service.PostService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class PostServiceTest {

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    private Long authorId;

    @BeforeEach
    void setUp() {
        User user = User.builder()
                .username("testuser")
                .password("password")
                .email("test@example.com")
                .role(User.UserRole.ADMIN)
                .build();
        user = userRepository.save(user);
        authorId = user.getId();
    }

    @Test
    @DisplayName("创建文章应有默认草稿状态")
    void shouldCreatePostWithDefaultDraftStatus() {
        Post post = new Post();
        post.setTitle("Test Post");
        post.setContent("This is a test");

        Post saved = postService.createPost(post, authorId);

        assertNotNull(saved.getId());
        assertEquals(Post.PostStatus.DRAFT, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
    }

    @Test
    @DisplayName("发布文章应设置发布时间")
    void publishPostShouldSetPublishedAt() {
        Post post = new Post();
        post.setTitle("Published Post");
        post.setContent("This will be published");
        post = postService.createPost(post, authorId);

        Post published = postService.publishPost(post.getId());

        assertEquals(Post.PostStatus.PUBLISHED, published.getStatus());
        assertNotNull(published.getPublishedAt());
    }

    @Test
    @DisplayName("按作者ID查找文章")
    void findByAuthorId() {
        for (int i = 0; i < 3; i++) {
            Post post = new Post();
            post.setTitle("Post " + i);
            post.setContent("Content " + i);
            postService.createPost(post, authorId);
        }

        User author = authorId != null
            ? userRepository.findById(authorId).orElseThrow()
            : postRepository.findAll().get(0).getAuthor();

        assertNotNull(author);
        assertEquals(3, postRepository.findByAuthor(author).size());
    }
}