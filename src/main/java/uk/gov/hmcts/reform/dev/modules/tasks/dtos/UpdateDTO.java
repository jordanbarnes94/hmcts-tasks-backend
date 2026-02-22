package uk.gov.hmcts.reform.dev.modules.tasks.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import uk.gov.hmcts.reform.dev.modules.global.validators.ValidDateTime;
import uk.gov.hmcts.reform.dev.modules.tasks.models.TaskStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class UpdateDTO {

    @NotBlank(message = "Title is required")
    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Due date is required")
    @ValidDateTime
    private String dueDate;

    @NotNull(message = "Status is required")
    private TaskStatus status;
}
