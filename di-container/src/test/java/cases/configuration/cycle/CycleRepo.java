package cases.configuration.cycle;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("cycleRepo")
public class CycleRepo {
    @Inject
    @Named("cycleService")
    private CycleService cycleService;
}