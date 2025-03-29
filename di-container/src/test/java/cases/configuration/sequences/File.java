package cases.configuration.sequences;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.inject.Named;

@Data
@NoArgsConstructor
@Named("file")
public class File {
    private String file;
}
