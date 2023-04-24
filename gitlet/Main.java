package gitlet;

import java.util.List;
import java.util.Arrays;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author Viviana Hu
 */
public class Main {

    public static void main(String[] args) {
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            return;
        } else if (!valid(args)) {
            System.out.println("Incorrect operands.");
            return;
        }

        String firstArg = args[0];
        Gitlet gitlet = new Gitlet();
        if (!args[0].equals("init")) {
            if (!gitlet.hasInited()) {
                System.out.println("Not in an initialized Gitlet directory.");
                return;
            }
            gitlet.deserialize(null);
        }

        switch(firstArg) {
            case "init":
                gitlet.init();
                break;
            case "add":
                String fileName = args[1];
                gitlet.add(fileName);
                break;
            case "commit":
                String message = args[1];
                gitlet.commit(message, false, "");
                break;
            case "rm":
                String nameFile = args[1];
                gitlet.rm(nameFile);
                break;
            case "log":
                gitlet.log();
                break;
            case "global-log":
                gitlet.globalLog();
                break;
            case "find":
                String commitMes = args[1];
                gitlet.find(commitMes);
                break;
            case "status":
                gitlet.status();
                break;
            case "checkout":
                if (args.length == 2) { //checkout [branchname]
                    String branch = args[1];
                    gitlet.branchCheckout(branch, null, true, null);
                } else if (args.length == 3) { //checkout -- [filename]
                    String filename = args[2];
                    gitlet.fileCheckout(args[1], filename);
                } else if (args.length == 4) { //checkout [commitID] -- [filename]
                    String commitID = args[1], filename = args[3];
                    gitlet.fileIDCheckout(commitID, args[2], filename);
                }
                break;
            case "branch":
                String branch = args[1];
                gitlet.branch(branch);
                break;
            case "rm-branch":
                String branchName = args[1];
                gitlet.rmBranch(branchName);
                break;
            case "reset":
                String commitID = args[1];
                gitlet.reset(commitID, null);
                break;
            case "merge":
                String branchname = args[1];
                gitlet.merge(branchname);
                break;
            case "add-remote":
                String remoteName = args[1];
                String remotePath = args[2];
                gitlet.addRemote(remoteName, remotePath);
                break;
            case "rm-remote":
                String remotename = args[1];
                gitlet.rmRemote(remotename);
                break;
            case "push":
                String remote_name = args[1];
                String remote_branch = args[2];
                gitlet.push(remote_name, remote_branch);
                break;
            case "fetch":
                String remote_Name = args[1];
                String remote_Branch = args[2];
                gitlet.fetch(remote_Name, remote_Branch);
                break;
            case "pull":
                String remote_name_ = args[1];
                String remote_branch_ = args[2];
                gitlet.pull(remote_name_, remote_branch_);
                break;
            default:
                System.out.println("No command with that name exists.");
        }
        gitlet.serialize(null);
    }

    private static final List<String> length_one = Arrays.asList("init", "log", "global-log", "status");
    private static final List<String> length_two = Arrays.asList("add", "commit", "rm", "find", "branch",
            "rm-branch", "reset", "merge", "rm-remote");
    private static final List<String> length_three = Arrays.asList("add-remote", "push", "fetch", "pull");
    /** Private helper method that determines the validity of input commands. */
    private static boolean valid(String[] args) {
        if (length_one.contains(args[0])) {
            return args.length == 1;
        } else if (length_two.contains(args[0])) {
            return args.length == 2;
        } else if (length_three.contains(args[0])){
            return args.length == 3;
        } else if (args[0].equals("checkout")) {
            return (args.length >= 2) && (args.length <= 4);
        }
        return true;
    }
}
