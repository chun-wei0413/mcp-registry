package tw.teddysoft.example.tag.usecase.port.out.projection;

import tw.teddysoft.example.tag.usecase.port.TagDto;
import java.util.List;

public interface AllTagsProjection {
    
    List<TagDto> query(AllTagsProjectionInput input);
    
    class AllTagsProjectionInput {
        public String planId;
        
        public AllTagsProjectionInput(String planId) {
            this.planId = planId;
        }
    }
}