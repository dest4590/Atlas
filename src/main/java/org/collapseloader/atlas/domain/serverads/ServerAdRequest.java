package org.collapseloader.atlas.domain.serverads;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerAdRequest {

    @NotBlank(message = "name must not be blank")
    @Size(max = 64, message = "name must be at most 64 characters")
    private String name;

    @NotBlank(message = "ip must not be blank")
    @Size(max = 255, message = "ip must be at most 255 characters")
    private String ip;

    private Boolean active;
}
