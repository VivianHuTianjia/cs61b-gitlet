# Gitlet Design Document

**Viviana Hu**:

## Classes and Data Structures

### Main

This class handles the different input commands (by calling the appropriate method in Gitlet.java) and various related error cases. It also performs deserialization (before) and serialization (after).

#### Fields

This class has two private Lists that keep track of how many arguments should each valid command has.


### Gitlet

This class is where most stuff actually takes place. Almost all command excutions  happen here.

#### Fields

1. `private static head`, `headSha1`, `currentBranch` -- Keep track of the head and current branch.
2. `private boolean switchBranch` -- Keep track of whether "branch [branchName] has been called.
3. `private static HashMap<String, Blob> filesInCWD, filesToAdd, filesToRM` -- Keep track of files in the CWD, and files staged to be added / removed (blob name, blob).
4. `private static HashMap<String, Commit> branches, allCommits, splitPoints` -- Keep track of all branches (branch name, commit object), all commits (sha1 value, commit object), and all split points (branch name, commit object).


### Repository

This is where the directories are created and tracked.

#### Fields

1. `public static final File CWD = new File(System.getProperty("user.dir"))` -- The current working directory.
2. `public static final File GITLET_DIR = join(CWD, ".gitlet")` -- The .gitlet directory. 
3. `public static final File STAGING_DIR = join(GITLET_DIR, ".staging")` --  The staging directory (currently unused, may discard).
4. `public static final File COMMITS_DIR = join(GITLET_DIR, ".commits")` -- The commit directory (currently unused, may discard).


### Commit

This represents a commit object, containing all useful informations about the commit (message, date, files, sha1, etc.).

#### Fields

1. `SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z")`, `Date date`, `String formattedDate` -- The time when the Commit is created, and format for the time.
2. `String message, sha1` -- The message of this Commit, and its unique sha-1 string.
3. `String branch, parentSha1, parent2Sha1` -- The branch it belongs to, its Parent Commit`s sha1, and its second Parent`s sha1 (if any).
4. `File thisCommitDir` -- The unique directory for this Commit.
5. `HashMap<String, Blob> files, parentFiles` -- all files tracked in this Commit (name, blob), and in its Parent Commit.


### Blob

This represents a blob object, containing all useful informations about the blob (directory, file, name, sha1, etc.).

#### Fields

1. `String name, sha1` -- Name of Blob`s file and sha1 value.
2. `File file` -- File object of the Blob.
3. `byte[] contents` -- Content of Blob`s file.

## Algorithms

### Gitlet.java

#### `private void updateCWD()`

Private helper method that keeps track of the files in CWD and updates them as needed, making it easier to iterate through the files.

#### `public void init()`

Creates the Gitlet directory and initial commit.

#### `public void add(String fileName)`

If the file of the given name exists in CWD and is different from any file of the same name tracked by the head commit, stage it for addition by putting it in the hashmap `filesToAdd`; else if it`s the same as the tracked file, remove it from the staging area. Also, if it will be no longer staged for removal if it was previously.

#### `public void commit(String message, boolean merged, String givenBranch)`

Creates a new commit, and take care of any files staged for addition / removal. If is a merged commit, set its second parent`s sha1 value to be that of `givenBranch`. Set the commit as the new `head`, keep track of the commit`s branch and sha1 by putting it in `branches` and `allCommits`, and clear the staging hashmaps.

#### `public void rm(String nameFile)`

If the file of the given name is tracked in the head commit, remove it from CWD and stage it for removal by putting it in `filesToRM`. Also, it will be no longer staged for addition if it was previously.

#### `public void log()`

Prints out the commits, using the helper method `logPrinter`, starting from `head`, ignoring any second parent.

####  `public void globalLog()`

Prints out all commits ever made by iterating through `allCommits`, using the helper method `logPrinter`. 

#### `private void logPrinter(Commit commit)`

Private helper method that formats each log.

#### `find(String commitMes)`

Find if a commit with the given commit message exists by iterating through `allCommits`.

#### `public void status()`

Prints out each branch in order by iterating through a sorted List of `branches`. Prints out all files staged for addition / removal by iterating through sorted Lists of `filesToAdd` and `filesToRM`. 

Prints out any file that's modified or deleted but not staged by iterating through `filesToAdd` again as well as the sorted List of `head.files`. Prints out any file in CWD that's untracked by `head` by iterating through `filesInCWD`.

#### `fileCheckout(String dashes, String filename)`

Checks out the file with the given name from `head` to CWD, using helper method `checkoutHelper`.

#### `public void fileIDCheckout(String commitID, String dashes, String filename)`

Check if the commit ID is valid using helper method `abbreviated`. Then checks out the file with the given name from the given commit (if exists) to CWD, using helper method `checkoutHelper`.

#### `private String abbreviated(String commitID`

If it's a shortened ID and valid, returns the valid normal ID. Else if it's a valid normal ID, returns itself. Else, returns null. 

#### `private void checkoutHelper(Commit commit, String filename)`

If the blob for the given filename exists, create a new file in CWD and write the given blob's content into the new file.

#### `public void branchCheckout(String branchName, Commit givenCommit, boolean checkBranch)`

Returns if checking out the given commit will overwrite any file in CWD using helper method `overwrite`. Else, checks out all file tracked by the given commit to CWD, using helper method `checkoutHelper`. If any file is tracked in head branch but in not the new branch, remove the file.

If no specific commit is passed in, find the head commit of the given branch using `branches`. If `switchBranch` is true, simply set the current branch to be the given branch. If `branchCheckout` is false, don't check whether the given branch is the current branch.

#### `private boolean overwrite(Commit commit, String filename)`

Helper method that returns true if a file is not tracked in current branch but is in new branch (that is, if it will cause overwriting).

#### `public void branch(String branchName)`

Creates a new branch at `head`, put the current branch and new branch in `splitPoints` for later reference, and set `switchBranch` to true.

#### `public void rmBranch(String branchName)`

Removes a branch from `branches`.

#### `public void reset(String commitID)`

Checks if the commit ID is valid using helper method `abbreviated`. Find the branch of the given commit, and checks out the branch using `branchCheckout`. Remember to pass in `checkBranch` as false.

#### `private Commit getSP(Commit c1, Commit c2)`

Helper method that returns the latest common ancestor (split point) of two commits.

####  `private ArrayList<Commit> allParents(Commit c)`

Helper method that finds all ancestors of a commit.

#### `public void merge(String givenBranch)`

Checks for various failure cases and if a merge is necessary. If not, calls helper method `mergeHelper`.

#### `private void mergeHelper(String givenBranch, Commit givenCommit, Commit splitPoint)`

Iterates through the files tracked by the split point as well as given commit, and decides whether a file should be added, removed, checked out, written to a conflicted version -- or if nothing should be done to it. Each case is specified in the method's code using the "comment" function. Finally, commit the merge.

#### `private boolean same(Blob c1, Blob c2)`

Helper method that returns whether two blobs have the same content, and returns false if any blob is null.

#### `private void writeConflict(Blob currBlob, Blob givenBlob, String filename)`

Helper method that formats the contents of current blob and given blob into a single file.

### Commit.java

#### `public void commitFromStaging(HashMap<String, Blob> filesMap)`

Adds the hashmap of files passed in into `files`.

#### `public void commitFromRM(String blobName)`

Removes all files passed in from `files`.

## Persistence

#### `Gitlet.serialize()` & `Gitlet.deserialize()` 

In `Main.java`, before each command (except for init) is processed, `Gitlet.deserialize()` is called. This method will read all of `Gitlet` 's fields from the Gitlet directory. Similarly, after any command has been processed, `Main.java` will call `Gitlet.serialize()` to write all these fields back into the Gitlet directory. 

Since `Gitlet`'s fields keeps track of all the informations and states of the program, it's ensured that nothing will be lost across multiple runs.
