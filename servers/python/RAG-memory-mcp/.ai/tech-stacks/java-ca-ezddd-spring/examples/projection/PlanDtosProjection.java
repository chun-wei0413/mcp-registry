package tw.teddysoft.example.plan.usecase.port.out.projection;

import tw.teddysoft.ezddd.cqrs.usecase.query.Projection;
import tw.teddysoft.ezddd.cqrs.usecase.query.ProjectionInput;
import tw.teddysoft.example.plan.usecase.port.PlanDto;

import java.util.List;

public interface PlanDtosProjection extends Projection<PlanDtosProjection.PlanDtosProjectionInput, List<PlanDto>> {

    enum SortBy {
        NAME,
        LAST_MODIFIED
    }

    enum SortOrder {
        ASC,
        DESC
    }

    class PlanDtosProjectionInput implements ProjectionInput {
        public String userId;
        public SortBy sortBy = SortBy.NAME;
        public SortOrder sortOrder = SortOrder.ASC;
    }
}