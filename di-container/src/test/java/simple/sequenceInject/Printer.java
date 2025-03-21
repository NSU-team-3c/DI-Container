package simple.sequenceInject;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("printer")
public class Printer {
    @Inject
    private Reader reader;

    public void print() {
        System.out.println(this);
    }

}
