package simple.sequenceInject;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("reader")
public class Reader {
    @Inject
    private File file;
}
