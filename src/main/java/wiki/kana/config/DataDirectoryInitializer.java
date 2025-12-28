package wiki.kana.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 确保本地 SQLite 数据库目录存在（避免 sqlite 无法创建 db 文件导致启动/构建失败）。
 */
@Slf4j
@Component
public class DataDirectoryInitializer implements BeanFactoryPostProcessor {

    private static final String DATA_DIR = "data";

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
        ensureDataDirExists();
    }

    private void ensureDataDirExists() {
        try {
            Path dataDir = Paths.get(DATA_DIR);
            if (Files.notExists(dataDir)) {
                Files.createDirectories(dataDir);
                log.info("✅ 已创建数据目录: {}", dataDir.toAbsolutePath());
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create data directory for SQLite DB", e);
        }
    }
}

