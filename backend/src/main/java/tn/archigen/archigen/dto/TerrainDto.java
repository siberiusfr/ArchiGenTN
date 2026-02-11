package tn.archigen.archigen.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Dimensions du terrain")
public class TerrainDto {

    @NotNull
    @DecimalMin("5.0")
    @Schema(description = "Largeur du terrain en metres", example = "15.0")
    private Double width;

    @NotNull
    @DecimalMin("5.0")
    @Schema(description = "Profondeur du terrain en metres", example = "25.0")
    private Double height;
}
