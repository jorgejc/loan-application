package co.com.bancolombia.model.status;
import lombok.*;

import java.math.BigDecimal;
import lombok.NoArgsConstructor;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Status {

    private Long statusId;
    private String name;
    private String description;
}
