package tw.teddysoft.aiscrum.product.adapter.out.repository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import tw.teddysoft.aiscrum.product.entity.*;
import tw.teddysoft.ezddd.usecase.port.out.repository.Repository;
import tw.teddysoft.ezspec.EzFeature;
import tw.teddysoft.ezspec.EzFeatureReport;
import tw.teddysoft.ezspec.extension.junit5.EzScenario;
import tw.teddysoft.ezspec.keyword.Feature;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * OutboxRepository 整合測試範例
 * 
 * 這是每個 Aggregate 的 OutboxRepository 都應該包含的標準測試案例。
 * 測試 OutboxRepository 實作與 PostgreSQL 資料庫的整合。
 * 
 * 測試涵蓋範圍：
 * 1. 資料持久化 - 驗證所有欄位正確儲存到資料庫
 * 2. 資料讀取 - 驗證從資料庫讀取的完整性
 * 3. 軟刪除 - 驗證 isDeleted 標記而非實體刪除
 * 4. 版本控制 - 驗證樂觀鎖機制
 * 
 * 配置說明：
 * - Profile: test-outbox (載入 application-test-outbox.yml)
 * - Database: PostgreSQL on localhost:5800, schema: message_store
 * - Transaction: 每個測試後自動 rollback
 * 
 * 注意：需要本地運行 PostgreSQL on port 5800 (測試資料庫)
 */
@SpringBootTest(classes = tw.teddysoft.aiscrum.io.springboot.AiScrumApp.class)
@Transactional
@ActiveProfiles("test-outbox")
@EzFeature
@EzFeatureReport
public class ProductOutboxRepositoryTest {
    
    static String FEATURE_NAME = "Product Outbox Repository";
    static Feature feature;
    static String PERSIST_RULE = "Product data should be correctly persisted to database";
    static String RETRIEVE_RULE = "Product data should be completely retrieved from database";
    static String SOFT_DELETE_RULE = "Product should be soft deleted with isDeleted flag";
    static String VERSION_CONTROL_RULE = "Optimistic locking should work with version control";
    
    private final Repository<Product, ProductId> productRepository;
    private final EntityManager entityManager;
    
    @Autowired
    public ProductOutboxRepositoryTest(
            Repository<Product, ProductId> productRepository,
            EntityManager entityManager) {
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }
    
    @BeforeAll
    static void beforeAll() {
        feature = Feature.New(FEATURE_NAME);
        feature.initialize();
        feature.NewRule(PERSIST_RULE);
        feature.NewRule(RETRIEVE_RULE);
        feature.NewRule(SOFT_DELETE_RULE);
        feature.NewRule(VERSION_CONTROL_RULE);
    }
    
    @BeforeEach
    void setUp() {
        // 初始化 BootstrapConfig 確保 DomainEventMapper 正確配置
        tw.teddysoft.aiscrum.io.springboot.config.BootstrapConfig.initialize();
    }
    
    @AfterAll
    static void afterAll() {
    }
    
    /**
     * 測試案例 1：資料持久化測試
     * 驗證 Aggregate 的所有欄位都正確儲存到資料庫
     */
    @EzScenario
    public void should_persist_product_to_database_with_all_fields() {
        feature.newScenario("Persist product to database and verify all fields are correctly stored")
            .withRule(PERSIST_RULE)
            .Given("a Product aggregate with complete data", env -> {
                ProductId productId = ProductId.valueOf("test-product-" + System.currentTimeMillis());
                Product product = new Product(productId, ProductName.valueOf("測試產品"));
                
                // 設定 Goal
                product.setGoal(
                    ProductGoalId.valueOf("goal-001"),
                    "交付價值",
                    "為客戶交付最大價值",
                    ProductGoalState.ACTIVE
                );
                
                // 設定 Definition of Done
                LinkedHashSet<DoneCriterion> criteria = new LinkedHashSet<>();
                criteria.add(new DoneCriterion("程式碼已審查"));
                criteria.add(new DoneCriterion("測試已撰寫"));
                criteria.add(new DoneCriterion("文件已更新"));
                
                product.defineDefinitionOfDone(
                    "Sprint DoD",
                    criteria,
                    "test-user",
                    Instant.now()
                );
                
                env.put("product", product);
                env.put("productId", productId);
            })
            .When("save the product using OutboxRepository", env -> {
                Product product = env.get("product", Product.class);
                productRepository.save(product);
                entityManager.flush();
                env.put("savedVersion", product.getVersion());
            })
            .Then("the product should be persisted in database", env -> {
                ProductId productId = env.get("productId", ProductId.class);
                
                // 驗證可以從 Repository 讀取
                Optional<Product> savedProduct = productRepository.findById(productId);
                assertThat(savedProduct).isPresent();
                assertThat(savedProduct.get().getName().value()).isEqualTo("測試產品");
                
                // 直接查詢資料庫驗證所有欄位
                Query query = entityManager.createNativeQuery(
                    "SELECT name, goal_title, goal_description, goal_state, definition_of_done, version, is_deleted " +
                    "FROM message_store.product WHERE id = ?1"
                );
                query.setParameter(1, productId.value());
                
                Object[] result = (Object[]) query.getSingleResult();
                assertThat(result).isNotNull();
                assertThat(result[0]).isEqualTo("測試產品");           // name
                assertThat(result[1]).isEqualTo("交付價值");           // goal_title
                assertThat(result[2]).isEqualTo("為客戶交付最大價值");  // goal_description
                assertThat(result[3]).isEqualTo("ACTIVE");            // goal_state
                assertThat(result[4]).isNotNull();                    // definition_of_done (JSON)
                assertThat(((Long) result[5])).isGreaterThanOrEqualTo(0L); // version
                assertThat(result[6]).isEqualTo(false);               // is_deleted
            }).Execute();
    }
    
    /**
     * 測試案例 2：資料讀取測試
     * 驗證從資料庫讀取的資料完整性
     */
    @EzScenario
    public void should_retrieve_product_with_complete_data() {
        feature.newScenario("Retrieve product from OutboxRepository with complete data")
            .withRule(RETRIEVE_RULE)
            .Given("a product exists in database", env -> {
                ProductId productId = ProductId.valueOf("test-product-" + System.currentTimeMillis());
                Product product = new Product(productId, ProductName.valueOf("已存在的產品"));
                
                product.setGoal(
                    ProductGoalId.valueOf("goal-002"),
                    "提高品質",
                    "減少 50% 的缺陷",
                    ProductGoalState.ACTIVE
                );
                
                productRepository.save(product);
                entityManager.flush();
                entityManager.clear();  // 清除快取，強制從資料庫讀取
                
                env.put("productId", productId);
                env.put("originalVersion", product.getVersion());
            })
            .When("retrieve the product from repository", env -> {
                ProductId productId = env.get("productId", ProductId.class);
                Optional<Product> result = productRepository.findById(productId);
                env.put("retrievedProduct", result.orElse(null));
            })
            .Then("all product data should be completely loaded", env -> {
                Product retrievedProduct = env.get("retrievedProduct", Product.class);
                
                assertThat(retrievedProduct).isNotNull();
                assertThat(retrievedProduct.getName().value()).isEqualTo("已存在的產品");
                
                // 驗證 Goal 資料
                ProductGoal goal = retrievedProduct.getGoal();
                assertThat(goal).isNotNull();
                assertThat(goal.getId().value()).isEqualTo("goal-002");
                assertThat(goal.getTitle()).isEqualTo("提高品質");
                assertThat(goal.getDescription()).isEqualTo("減少 50% 的缺陷");
                
                // 驗證版本號
                assertThat(retrievedProduct.getVersion()).isGreaterThanOrEqualTo(0);
            }).Execute();
    }
    
    /**
     * 測試案例 3：軟刪除測試
     * 驗證軟刪除功能（標記 isDeleted 而非實體刪除）
     * 
     * 重要：軟刪除應該使用 save() 而非 delete()
     */
    @EzScenario
    public void should_soft_delete_product() {
        feature.newScenario("Soft delete product using OutboxRepository")
            .withRule(SOFT_DELETE_RULE)
            .Given("a product exists in database", env -> {
                ProductId productId = ProductId.valueOf("test-product-delete-" + System.currentTimeMillis());
                Product product = new Product(productId, ProductName.valueOf("待刪除的產品"));
                
                productRepository.save(product);
                entityManager.flush();
                
                env.put("productId", productId);
                env.put("product", product);
            })
            .When("soft delete the product using repository", env -> {
                Product product = env.get("product", Product.class);
                product.markAsDelete("u001");
                productRepository.save(product);  // 使用 save 而不是 delete 來執行軟刪除
                entityManager.flush();
            })
            .Then("product should be marked as deleted but remain in database", env -> {
                ProductId productId = env.get("productId", ProductId.class);
                
                // 驗證資料仍在資料庫中
                Query countQuery = entityManager.createNativeQuery(
                    "SELECT COUNT(*) FROM message_store.product WHERE id = ?1"
                );
                countQuery.setParameter(1, productId.value());
                
                Long count = (Long) countQuery.getSingleResult();
                assertThat(count).isEqualTo(1L);
                
                // 驗證 isDeleted 標記
                Query deleteQuery = entityManager.createNativeQuery(
                    "SELECT is_deleted FROM message_store.product WHERE id = ?1"
                );
                deleteQuery.setParameter(1, productId.value());
                
                Boolean isDeleted = (Boolean) deleteQuery.getSingleResult();
                assertThat(isDeleted).isTrue();
            }).Execute();
    }
    
    /**
     * 測試案例 4：版本控制測試
     * 驗證樂觀鎖機制
     */
    @EzScenario
    public void should_handle_version_control_for_optimistic_locking() {
        feature.newScenario("Update product and verify version control")
            .withRule(VERSION_CONTROL_RULE)
            .Given("a product exists in database", env -> {
                ProductId productId = ProductId.valueOf("test-product-version-" + System.currentTimeMillis());
                Product product = new Product(productId, ProductName.valueOf("版本控制測試"));
                
                productRepository.save(product);
                entityManager.flush();
                
                env.put("productId", productId);
                env.put("product", product);
                env.put("initialVersion", product.getVersion());
            })
            .When("update and save the product", env -> {
                Product product = env.get("product", Product.class);
                
                product.setGoal(
                    ProductGoalId.valueOf("updated-goal"),
                    "更新的目標",
                    "更新的描述",
                    ProductGoalState.ACTIVE
                );
                
                productRepository.save(product);
                entityManager.flush();
                
                env.put("updatedVersion", product.getVersion());
            })
            .Then("version number should be incremented", env -> {
                Long initialVersion = env.get("initialVersion", Long.class);
                Long updatedVersion = env.get("updatedVersion", Long.class);
                ProductId productId = env.get("productId", ProductId.class);
                
                // 驗證版本號增加
                assertThat(updatedVersion).isGreaterThan(initialVersion);
                
                // 驗證資料庫中的版本號
                Query query = entityManager.createNativeQuery(
                    "SELECT version FROM message_store.product WHERE id = ?1"
                );
                query.setParameter(1, productId.value());
                
                Long dbVersion = (Long) query.getSingleResult();
                assertThat(dbVersion).isEqualTo(updatedVersion);
            }).Execute();
    }
}