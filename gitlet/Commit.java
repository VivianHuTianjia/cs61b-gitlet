package gitlet;

import java.io.Serializable;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.HashMap;

/** Represents a gitlet commit object.
 *  It contains commit message, branch of the commit, its parent(s),
 *  files tracked by its parents, and files tracked by itself.
 *  @author Viviana Hu
 */
public class Commit implements Serializable {

    /** The time when the Commit is created, and format for the time. */
    SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z");
    Date date;
    String formattedDate;
    /** The message of this Commit, its unique sha-1 string. */
    String message, sha1;
    /** The branch it belongs to, its Parent Commit's sha1, and its second Parent's sha1 (if any).*/
    String branch, parentSha1, secondBranch, parent2Sha1;
    /** The map of all files tracked in this Commit (name, blob), and in its Parent Commit.*/
    HashMap<String, Blob> files;
    HashMap<String, Blob> parentFiles;

    public Commit(String message, String branch, String parentSha1,
                   HashMap<String, Blob> parentFiles) {
        this.date = new Date();
        this.message = message;
        this.branch = branch;
        this.parentSha1 = parentSha1;
        this.parent2Sha1 = null;
        this.secondBranch = null;
        if (parentSha1 == null) {
            this.date = new Date(0); //unixDate
            this.parentFiles = new HashMap<>();
            this.files = new HashMap<>();
        } else {
            this.parentFiles = parentFiles;
            this.files = (HashMap<String, Blob>) this.parentFiles.clone();
        }
        this.formattedDate = dateFormat.format(date);
        this.sha1 = Utils.sha1(message, formattedDate);
    }

    public void commitFromStaging(HashMap<String, Blob> filesMap) {
        this.files.putAll(filesMap);
    }

    public void commitFromRM(String blobName) {
        this.files.remove(blobName);
    }

}
