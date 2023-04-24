package gitlet;

import java.io.Serializable;
import java.io.File;

public class Blob implements Serializable {

    /** Name of Blob's file. */
    String name;
    /** File object of the Blob. */
    File file;
    /** Sha1 value of Blob. */
    String sha1;
    /** Content of Blob's file. */
    byte[] contents;

    public Blob(File directory, String fileName) {
        this.file = Utils.join(directory, fileName);
        this.name = fileName;
        if (!this.file.exists()) {
            this.contents = null;
        } else {
            this.contents = Utils.readContents(this.file);
        }
        this.sha1 = Utils.sha1(contents);
    }


}
