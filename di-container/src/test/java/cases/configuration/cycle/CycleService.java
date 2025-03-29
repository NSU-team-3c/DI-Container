package cases.configuration.cycle;

import lombok.Data;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@Named("cycleService")
public class CycleService {
    private final CycleRepo cycleRepo;

    @Inject
    public CycleService(@Named("cycleRepo") CycleRepo cycleRepo) {
        this.cycleRepo = cycleRepo;
    }
}