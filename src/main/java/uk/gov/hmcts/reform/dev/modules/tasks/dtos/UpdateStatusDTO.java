package uk.gov.hmcts.reform.dev.modules.tasks.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateStatusDTO {

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
