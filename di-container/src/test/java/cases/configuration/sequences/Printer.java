package cases.configuration.sequences;

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
        System.out.println(reader.getFile().getFile());
    }

    public String getFileInfo() {
        return reader.getFile().getFile();
    }
}
