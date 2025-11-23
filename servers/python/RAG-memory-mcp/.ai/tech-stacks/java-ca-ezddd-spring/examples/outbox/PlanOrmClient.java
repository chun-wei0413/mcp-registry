package tw.teddysoft.example.io.springboot.config.orm;

import tw.teddysoft.example.plan.usecase.port.out.PlanData;
import tw.teddysoft.ezddd.data.io.ezoutbox.SpringJpaClient;

/**
 * Interface to generate bean for JPA CRUDRepository
 * This will be used by PostgresOutboxStoreClient for Plan aggregate
 */
public interface PlanOrmClient extends SpringJpaClient<PlanData, String> {
}