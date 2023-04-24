package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.nio.file.Paths;

public class Gitlet implements Serializable {

    /** Keep track of the head and current branch. */
    private static Commit head;
    private static String currentBranch;
    /** Keep track of whether "branch [branchName] has been called. */
    private boolean switchBranch;
    /** Keep track of files staged to be added / removed in the CWD (blob name, blob). */
    private static HashMap<String, Blob> filesToAdd;
    private static HashMap<String, Blob> filesToRM;
    /** Keep track of files in the CWD. */
    private static HashMap<String, Blob> filesInCWD;
    /** The map of all branches (branch name, commit object). */
    private static HashMap<String, Commit> branches;
    /** The map of all commits (sha1 value, commit object). */
    private static HashMap<String, Commit> allCommits;
    /** The map of all remotes (remote name, remote path). */
    private static HashMap<String, String> allRemotes;
    /** The current working directory. */
    private static File CWD;
    /** The .gitlet directory. */
    private static File GITLET_DIR;

    public Gitlet() {
        this.head = null;
        this.currentBranch = "master";
        this.switchBranch = false;
        this.filesToAdd = new HashMap<>();
        this.filesToRM = new HashMap<>();
        this.filesInCWD = new HashMap<>();
        this.branches = new HashMap<>();
        this.allCommits = new HashMap<>();
        this.allRemotes = new HashMap<>();
        this.CWD = new File(System.getProperty("user.dir"));
        this.GITLET_DIR = Utils.join(CWD, ".gitlet");
    }

    public void serialize(File cwd) {
        if (cwd == null) {
            cwd = this.CWD;
        }
        File gitDir = Utils.join(cwd, ".gitlet"); //GITLET_DIR;
        Utils.writeObject(Utils.join(gitDir, "data"), this);
        Utils.writeObject(Utils.join(gitDir, "CWD"), this.CWD);
        Utils.writeObject(Utils.join(gitDir, "GITDIR"), this.GITLET_DIR);
        Utils.writeObject(Utils.join(gitDir, "head"), this.head);
        Utils.writeObject(Utils.join(gitDir, "branches"), this.branches);
        Utils.writeObject(Utils.join(gitDir, "commits"), this.allCommits);
        Utils.writeObject(Utils.join(gitDir, "remotes"), this.allRemotes);
        Utils.writeObject(Utils.join(gitDir, "staging"), this.filesToAdd);
        Utils.writeObject(Utils.join(gitDir, "stagingRM"), this.filesToRM);
        Utils.writeObject(Utils.join(gitDir, "currentBranch"), this.currentBranch);
        Utils.writeObject(Utils.join(gitDir, "switchBranch"), this.switchBranch);
    }

    @SuppressWarnings("unchecked")
    public void deserialize(File cwd) {
        if (cwd == null) {
            cwd = this.CWD;
        }
        File gitDir = Utils.join(cwd, ".gitlet"); //repository.GITLET_DIR;
        Gitlet git = (Gitlet) Utils.readObject(Utils.join(gitDir, "data"));
        this.head = (Commit) Utils.readObject(Utils.join(gitDir, "head"));
        this.CWD = (File) Utils.readObject(Utils.join(gitDir, "CWD"));
        this.GITLET_DIR = (File) Utils.readObject(Utils.join(gitDir, "GITDIR"));
        this.branches = (HashMap<String, Commit>) Utils.readObject(Utils.join(gitDir, "branches"));
        this.allCommits = (HashMap<String, Commit>) Utils.readObject(Utils.join(gitDir, "commits"));
        this.allRemotes = (HashMap<String, String>) Utils.readObject(Utils.join(gitDir, "remotes"));
        this.filesToAdd = (HashMap<String, Blob>) Utils.readObject(Utils.join(gitDir, "staging"));
        this.filesToRM = (HashMap<String, Blob>) Utils.readObject(Utils.join(gitDir, "stagingRM"));
        this.currentBranch = (String) Utils.readObject(Utils.join(gitDir, "currentBranch"));
        this.switchBranch = (Boolean) Utils.readObject(Utils.join(gitDir, "switchBranch"));
    }

    /** Private helper method that keeps track of the files in CWD
     * and updates them as needed. */
    private void updateCWD(File cwd) {
        if (cwd == null) {
            cwd = this.CWD;
        }
        filesInCWD.clear();
        List<String> files = Utils.plainFilenamesIn(cwd);
        for (String name : files) {
            Blob newBlob = new Blob(cwd, name);
            filesInCWD.put(name, newBlob);
        }
    }

    public void init() {
        //this.repository = new Repository();
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();
            Commit first = new Commit("initial commit", currentBranch,
                    null, null);
            head = first;
            allCommits.put(head.sha1, head);
            branches.put(currentBranch, head);
        } else {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
        }
    }

    /** Private helper method that determines whether the Gitlet directory exists.*/
    public static boolean hasInited() {
        return GITLET_DIR != null && GITLET_DIR.exists();
    }

    public void add(String fileName) {
        updateCWD(null);
        Blob newBlob = filesInCWD.get(fileName);
        if (newBlob != null && newBlob.file.exists()) {
            Blob headBlob = head.files.get(fileName);
            boolean add = true;
            if (same(newBlob, headBlob)) {
                //if current working version of file is identical, don't add
                add = false;
            }
            if (!filesToRM.isEmpty() && filesToRM.containsKey(fileName)) {
                filesToRM.remove(fileName); //no longer staged for removal
                add = false;
            }
            if (add) {
                filesToAdd.put(fileName, newBlob); //stage it for addition
            } else if (filesToAdd.containsKey(fileName)) {
                //if identical, no longer staged for addition
                filesToAdd.remove(fileName);
            }
        } else {
            System.out.println("File does not exist.");
        }
    }

    public void commit(String message, boolean merged, String givenBranch) {
        if (!merged && filesToAdd.isEmpty() && filesToRM.isEmpty()) {
            System.out.println("No changes added to the commit.");
        } else if (message.length() == 0 || message.equals("")) {
            System.out.println("Please enter a commit message.");
        } else {
            Commit newCommit = new Commit(message, currentBranch, head.sha1, head.files);
            if (!filesToAdd.isEmpty()) {
                newCommit.commitFromStaging(filesToAdd);
            }
            if (!filesToRM.isEmpty()) {
                for (String blobname : filesToRM.keySet()) {
                    newCommit.commitFromRM(blobname);
                }
            }
            if (merged) { //if is a merged commit, set its second parent Sha1
                newCommit.secondBranch = givenBranch;
                newCommit.parent2Sha1 = branches.get(givenBranch).sha1;
            }
            head = newCommit;
            branches.put(currentBranch, newCommit);
            allCommits.put(newCommit.sha1, newCommit);
            filesToAdd.clear();
            filesToRM.clear();
        }
    }

    public void rm(String nameFile) {
        boolean reason = false;
        Blob newBlob = head.files.get(nameFile);
        if (head.files.containsKey(nameFile) && newBlob != null) {
            //if tracked in current commit
            filesToRM.put(nameFile, newBlob);
            Utils.restrictedDelete(nameFile);
            reason = true;
        }
        if (filesToAdd.containsKey(nameFile)) {
            //if staged for removal
            filesToAdd.remove(nameFile);
            reason = true;
        }
        if (!reason) {
            System.out.println("No reason to remove the file.");
        }
    }

    public void log() {
        Commit commit = head;
        while (commit != null) {
            logPrinter(commit);
            commit = allCommits.get(commit.parentSha1);
        }
    }

    public void globalLog() {
        for (Commit commit : allCommits.values()) {
            logPrinter(commit);
        }
    }

    /** Private helper function that formats and prints log information*/
    private void logPrinter(Commit commit) {
        System.out.println("===\n" + "commit " + commit.sha1);
        if (commit.secondBranch != null) { //if merge
            System.out.println("Merge: " + commit.parentSha1.substring(0, 7)
                    + " " + commit.parent2Sha1.substring(0, 7));
            System.out.println("Date: " + commit.formattedDate);
            System.out.println("Merged " + commit.secondBranch
                    + " into " + commit.branch + ".\n");
        } else {
            System.out.println("Date: " + commit.formattedDate);
            System.out.println(commit.message + "\n");
        }
    }

    public void find(String commitMes) {
        boolean found = false;
        for (Commit commit : allCommits.values()) {
            if (commit.message.equals(commitMes)) {
                System.out.println(commit.sha1);
                found = true;
            }
        }
        if (!found) {
            System.out.println("Found no commit with that message.");
        }
    }

    public void status() {
        List<String> sortBranches = new ArrayList<>(branches.keySet());
        Collections.sort(sortBranches);
        System.out.println("=== Branches ===");
        for (String branch : sortBranches) {
            if (branch.equals(currentBranch)) {
                System.out.print("*");
            }
            System.out.println(branch);
        }

        List<String> sortAdd = new ArrayList<>(filesToAdd.keySet());
        Collections.sort(sortAdd);
        List<String> sortRM = new ArrayList<>(filesToRM.keySet());
        Collections.sort(sortRM);
        System.out.println("\n=== Staged Files ===");
        for (String name : sortAdd) {
            System.out.println(name);
        }
        System.out.println("\n=== Removed Files ===");
        for (String name : sortRM) {
            System.out.println(name);
        }

        List<String> sortTracked = new ArrayList<>(head.files.keySet());
        Collections.sort(sortTracked);
        updateCWD(null);
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        for (String nameAdd : sortAdd) { //for all those added (staged)
            Blob blobCWD = filesInCWD.get(nameAdd);
            Blob blobAdd = filesToAdd.get(nameAdd);
            if (!filesInCWD.containsKey(nameAdd)) {
                //if deleted in CWD
                System.out.println(nameAdd + " (deleted)");
            } else if (!same(blobCWD, blobAdd)) {
                // if has different content in CWD
                System.out.println(nameAdd + " (modified)");
            }
        }
        for (String nameTrack : sortTracked) { //for all those tracked in head commit
            Blob blobCWD = filesInCWD.get(nameTrack);
            Blob blobTracked = head.files.get(nameTrack);
            if (!filesInCWD.containsKey(nameTrack)
                    && !sortRM.contains(nameTrack)) {
                //if deleted in CWD but not staged for removal
                System.out.println(nameTrack + " (deleted)");
            } else if (!same(blobCWD, blobTracked)
                    && !sortAdd.contains(nameTrack) && !sortRM.contains((nameTrack))) {
                //if has different content in CWD but not staged
                System.out.println(nameTrack + " (modified)");
            }
        }

        List<String> sortCWD = new ArrayList<>(filesInCWD.keySet());
        Collections.sort(sortCWD);
        System.out.println("\n=== Untracked Files ===");
        for (String name : sortCWD) {
            if (!filesToAdd.containsKey(name) && !head.files.containsKey(name)) {
                //file in CWD neither added or tracked
                System.out.println(name);
            }
        }
        System.out.println();
    }

    public void fileCheckout(String dashes, String filename) {
        if (!dashes.equals("--")) {
            System.out.println("Incorrect operands.");
        } else {
            checkoutHelper(head, filename, null);
        }
    }

    public void fileIDCheckout(String commitID, String dashes, String filename) {
        commitID = abbreviated(commitID);
        if (!dashes.equals("--")) {
            System.out.println("Incorrect operands.");
            return;
        } else if (commitID == null) {
            System.out.println("No commit with that id exists.");
            return;
        } else { //if commitID is valid
            Commit checkCommit = allCommits.get(commitID);
            checkoutHelper(checkCommit, filename, null);
        }
    }

    /** Private helper method that returns
     * whether a short sha1-strings is validly abbreviated. */
    private String abbreviated(String commitID) {
        if (commitID.length() < 40) {
            for (String sha1 : allCommits.keySet()) {
                if (sha1.startsWith(commitID)) {
                    return sha1;
                }
            }
        } else if (allCommits.containsKey(commitID)) {
            return commitID;
        }
        return null;
    }

    /** Private helper method that checks out
     * a file in a given commit to the CWD. */
    private void checkoutHelper(Commit commit, String filename, File cwd) {
        if (cwd == null) {
            cwd = this.CWD;
        }
        Blob checkBlob = commit.files.get(filename);
        if (checkBlob != null) { //(checkBlob.file.isFile()) (newFile.exists())
            File newFile = Utils.join(cwd, filename);
            byte[] contents = checkBlob.contents;
            Utils.writeContents(newFile, contents);
        } else {
            System.out.println("File does not exist in that commit.");
        }
    }

    /** Some resets may check out a specific commit on the current branch,
     * so givenCommit and checkBranch accounts for this edge case. */
    public void branchCheckout(String branchName,
                               Commit givenCommit, boolean checkBranch, File cwd) {
        if (givenCommit == null) {
            givenCommit = branches.get(branchName);
        }
        if (givenCommit == null && !branches.containsKey(branchName)) {
            System.out.println("No such branch exists");
        } else if (checkBranch && branchName.equals(currentBranch)) {
            System.out.println("No need to checkout the current branch.");
        } else {
            updateCWD(cwd);
            for (String name : filesInCWD.keySet()) {
                if (overwrite(givenCommit, name)) {
                    return;
                }
            }
            for (String name : givenCommit.files.keySet()) {
                //checkout all files tracked in new commit
                checkoutHelper(givenCommit, name, cwd);
            }
            for (String name : head.files.keySet()) {
                //if tracked in head branch but not new branch
                if (!givenCommit.files.containsKey(name)) {
                    Utils.restrictedDelete(head.files.get(name).file);
                }
            }
            if (switchBranch || givenCommit.equals(head)) {
                head = branches.get(branchName);
                currentBranch = branchName;
                switchBranch = false;
            } else {
                currentBranch = branchName;
                head = givenCommit;
                branches.put(currentBranch, head);
                filesToAdd.clear(); //clear staging area
                filesToRM.clear();
            }
        }
    }

    /** Private helper method that returns true if a file is not tracked
     * in current branch but is in new branch (will cause overwrite).*/
    private boolean overwrite(Commit commit, String filename) {
        Blob fileInCWD = filesInCWD.get(filename);
        Blob fileInCommit = commit.files.get(filename);
        Blob fileInHead = head.files.get(filename);
        String msg = "There is an untracked file in the way; "
                + "delete it, or add and commit it first.";
        if (fileInCWD != null && fileInCWD.file.isFile()
                && fileInCommit != null && !same(fileInCommit, fileInCWD)) {
            //if file in CWD differs from that in given commit
            if (!same(fileInHead, fileInCWD) && !filesToAdd.containsKey(filename)) {
                //if not tracked in head, or modified but and not staged for add
                System.out.println(msg);
                return true;
            }
        }
        return false;
    }

    public void branch(String branchName) {
        if (branches.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
        } else {
            branches.put(branchName, head);
            switchBranch = true;
        }
    }

    public void rmBranch(String branchName) {
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
        } else if (branchName.equals(currentBranch)) {
            System.out.println("Cannot remove the current branch.");
        } else {
            branches.remove(branchName);
        }
    }

    public void reset(String commitID, File cwd) {
        commitID = abbreviated(commitID);
        if (commitID == null) {
            System.out.println("No commit with that id exists.");
        } else {
            Commit commit = allCommits.get(commitID);
            String branch = commit.branch;
            branchCheckout(branch, commit, false, cwd);
        }
    }

    /** Private helper method that returns the latest common ancestor
     * (split point) of two commits. */
    private Commit getSP(Commit c1, Commit c2) {
        if (c1.equals(c2)) {
            return c1;
        }
        ArrayList<String> allParents1 = allParents(c1);
        ArrayList<String> allParents2 = allParents(c2);
        for (String sha1 : allParents2) {
            if (allParents1.contains(sha1)) {
                return allCommits.get(sha1);
            }
        }
        return null;
    }

    /** Private helper method that finds all ancestors of a commit. */
    private ArrayList<String> allParents(Commit c) {
        ArrayList<String> allP = new ArrayList<>();
        if (c.parentSha1 == null) {
            allP.add(c.sha1);
            return allP;
        } else {
            allP.add(c.sha1);
            allP.addAll(allParents(allCommits.get(c.parentSha1)));
            if (c.parent2Sha1 != null) {
                allP.addAll(allParents(allCommits.get(c.parent2Sha1)));
            }
        }
        return allP;
    }

    public void merge(String givenBranch) {
        if (!filesToAdd.isEmpty() || !filesToRM.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            return;
        }
        if (!branches.containsKey(givenBranch)) {
            System.out.println("A branch with that name does not exist.");
            return;
        }
        if (givenBranch.equals(currentBranch)) {
            System.out.println("Cannot merge a branch with itself.");
            return;
        }
        updateCWD(null);
        Commit givenCommit = branches.get(givenBranch);
        Commit splitPoint = getSP(head, givenCommit);
        for (String name : filesInCWD.keySet()) {
            if (overwrite(givenCommit, name)) {
                return;
            }
        }
        if (same(givenCommit, splitPoint)) {
            // if split point is the same commit as the given branch
            System.out.println("Given branch is an ancestor of the current branch.");
        } else if (same(head, splitPoint)) {
            // if split point is the same commit as the current branch
            branchCheckout(givenBranch, givenCommit, false, null);
            System.out.println("Current branch fast-forwarded.");
        } else {
            mergeHelper(givenBranch, givenCommit, splitPoint);
        }
    }

    private void mergeHelper(String givenBranch, Commit givenCommit, Commit splitPoint) {
        boolean conflict = false;
        for (Blob splitBlob : splitPoint.files.values()) { //files in split point
            Blob givenBlob = givenCommit.files.get(splitBlob.name);
            Blob currBlob = head.files.get(splitBlob.name);
            if (givenBlob == null && currBlob == null) {
                //3.2.both deleted
                continue;
            } else if (givenBlob == null && same(splitBlob, currBlob)) {
                //6.deleted in given commit, not modified in current commit
                rm(currBlob.name);
            } else if (givenBlob == null && currBlob != null) {
                //8.2.deleted in given commit, modified in current commit
                writeConflict(currBlob, givenBlob, splitBlob.name);
                conflict = true;
            } else if (currBlob == null && givenBlob != null && !same(splitBlob, givenBlob)) {
                //8.2.deleted in current commit, modified in given commit
                writeConflict(currBlob, givenBlob, splitBlob.name);
                conflict = true;
            } else if (!same(splitBlob, givenBlob) && same(splitBlob, currBlob)) {
                //1.modified in given commit, not modified in current commit
                fileIDCheckout(givenCommit.sha1, "--", givenBlob.name);
                add(givenBlob.name);
            } else if (same(splitBlob, givenBlob) && !same(splitBlob, currBlob)) {
                //2.modified in current commit, not modified in given commit
                continue;
            } else if (!same(splitBlob, givenBlob) && !same(splitBlob, currBlob)
                    && !same(currBlob, givenBlob)) {
                //8.3.contents both changed, differ in given & current commits
                writeConflict(currBlob, givenBlob, splitBlob.name);
                conflict = true;
            }
        }
        for (Blob givenBlob : givenCommit.files.values()) { //files in given commit
            Blob splitBlob = splitPoint.files.get(givenBlob.name);
            Blob currBlob = head.files.get(givenBlob.name);
            if (splitBlob == null) { //if not in split point
                if (currBlob == null) {
                    //5.only present in given commit
                    fileIDCheckout(givenCommit.sha1, "--", givenBlob.name);
                    add(givenBlob.name);
                } else if (!same(currBlob, givenBlob)) {
                    //8.1.absent in split point, different contents
                    writeConflict(currBlob, givenBlob, givenBlob.name);
                    conflict = true;
                }
            } else if (same(currBlob, givenBlob)) {
                //3.modified in both commits in same way
                continue;
            }
        }
        if (!splitPoint.sha1.equals(givenCommit.sha1)
                && !splitPoint.sha1.equals(head.sha1)) {
            commit("Merged " + givenBranch + " into "
                    + currentBranch + ".", true, givenBranch);
        }
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /** Private helper methods that returns whether two blobs or commits
     * have the same content, and returns false if any blob is null. */
    private boolean same(Blob c1, Blob c2) {
        if (c1 != null && c2 != null) {
            return c1.sha1.equals(c2.sha1);
        }
        return false;
    }
    private boolean same(Commit c1, Commit c2) {
        if (c1 != null && c2 != null) {
            return c1.sha1.equals(c2.sha1);
        }
        return false;
    }

    private void writeConflict(Blob currBlob, Blob givenBlob, String filename) {
        String currContent = "";
        String givenContent = "";
        if (currBlob != null) {
            currContent = new String(currBlob.contents);
        }
        if (givenBlob != null) {
            givenContent = new String(givenBlob.contents);
        }
        String merged = "<<<<<<< HEAD\n" + currContent + "=======\n"
                + givenContent + ">>>>>>>\n";
        File file = Utils.join(CWD, filename);
        Utils.writeContents(file, merged);
        add(filename);
    }

    public void addRemote(String remoteName, String remotePath) {
        if (allRemotes.containsKey(remoteName)) {
            System.out.println("A remote with that name already exists.");
        } else {
            String separator = java.io.File.separator;
            remotePath = remotePath.replace("/", separator);
            allRemotes.put(remoteName, remotePath);
        }
    }

    public void rmRemote(String remoteName) {
        if (!allRemotes.containsKey(remoteName)) {
            System.out.println("A remote with that name does not exist.");
        } else {
            allRemotes.remove(remoteName);
        }
    }

    public void push(String remoteName, String remoteBranch) {
        String remotePath = allRemotes.get(remoteName);
        File remoteDir = Paths.get(remotePath).toFile();
        File remoteGitDir = Paths.get(remotePath, "GITDIR").toFile();
        if (!remoteGitDir.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }

        Gitlet remote = (Gitlet) Utils.readObject(Utils.join(remoteDir, "data"));
        File remoteCWD = (File) Utils.readObject(Utils.join(remoteDir, "CWD"));
        Commit remoteHead = (Commit) Utils.readObject(Utils.join(remoteDir, "head"));
        HashMap<String, Commit> remoteCommits = (HashMap<String, Commit>)
                Utils.readObject(Utils.join(remoteDir, "commits"));
        HashMap<String, Commit> remoteBranches = (HashMap<String, Commit>)
                Utils.readObject(Utils.join(remoteDir, "branches"));
        if (!allCommits.containsKey(remoteHead.sha1)) {
            System.out.println("Please pull down remote changes before pushing.");
        } else {
            Commit commit = head;
            LinkedList<Commit> commitsToPush = new LinkedList<>();
            //find all commits need to be pushed
            while (commit != null && !same(commit, remoteHead)
                    && !remoteCommits.containsKey(commit.sha1)) {
                commitsToPush.addFirst(commit);
                commit = allCommits.get(commit.parentSha1);
            }
            //append these commits to remote gitlet
            for (Commit c : commitsToPush) {
                remoteCommits.put(c.sha1, c);
                remoteBranches.put(remoteBranch, c);
            }
            remote.reset(head.sha1, remoteCWD); //reset remote gitlet to front (head) commit
            remote.serialize(remoteCWD);
        }
    }

    public void fetch(String remoteName, String remoteBranch) {
        String remotePath = allRemotes.get(remoteName);
        File remoteDir = Paths.get(remotePath).toFile();
        File remoteGitDir = Paths.get(remotePath, "GITDIR").toFile();
        if (!remoteGitDir.exists()) {
            System.out.println("Remote directory not found.");
            return;
        }

        Gitlet remote = (Gitlet) Utils.readObject(Utils.join(remoteDir, "data"));
        Commit remoteHead = (Commit) Utils.readObject(Utils.join(remoteDir, "head"));
        if (!remote.branches.containsKey(remoteBranch)) {
            System.out.println("That remote does not have that branch.");
        } else {
            String newBranch = remoteName + "/" + remoteBranch;
            if (!branches.containsKey(newBranch)) {
                branch(newBranch);
            }
            Commit commit = remoteHead;
            LinkedList<Commit> commitsToPush = new LinkedList<>();
            //find all commits need to be copied
            while (commit != null && !same(commit, head)
                    && !allCommits.containsKey(commit.sha1)) {
                commitsToPush.addFirst(commit);
                commit = allCommits.get(commit.parentSha1);
            }
            //append these commits to this gitlet
            for (Commit c : commitsToPush) {
                allCommits.put(c.sha1, c);
                branches.put(newBranch, c);
            }
        }
    }

    public void pull(String remoteName, String remoteBranch) {
        fetch(remoteName, remoteBranch);
        merge(remoteName + "/" + remoteBranch);
    }

}
