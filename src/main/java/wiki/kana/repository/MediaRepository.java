package wiki.kana.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import wiki.kana.entity.Media;

import java.util.List;
import java.util.Optional;

/**
 * 媒体资源数据访问层
 */
@Repository
public interface MediaRepository extends JpaRepository<Media, Long> {

    /**
     * 根据文件类型查找媒体
     */
    List<Media> findByType(Media.MediaType type);

    /**
     * 根据上传者查找媒体
     */
    List<Media> findByUploadedById(Long userId);

    /**
     * 根据文件名查找媒体
     */
    Optional<Media> findByFileName(String fileName);

    /**
     * 统计指定类型的媒体数量
     */
    @Query("SELECT COUNT(m) FROM Media m WHERE m.type = :type")
    long countByType(@Param("type") Media.MediaType type);

    /**
     * 查找最近的媒体
     */
    @Query("SELECT m FROM Media m ORDER BY m.createdAt DESC")
    List<Media> findRecentMedia(int limit);
}