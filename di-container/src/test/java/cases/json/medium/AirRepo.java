package cases.json.medium;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@Named("airRepo")
@NoArgsConstructor
public class AirRepo {
    private String data;
}