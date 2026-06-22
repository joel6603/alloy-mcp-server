import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

public class RunAlloy {
    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Usage: java RunAlloy <filename> <--syntax-only | --run> [commandName]");
            return;
        }

        String filename = args[0];
        String mode = args[1];
        String targetCommand = (args.length > 2) ? args[2] : "all";

        A4Reporter rep = new A4Reporter();

        try {
            // This line parses the file. If there is a syntax error, it immediately jumps to the catch block!
            Module world = CompUtil.parseEverything_fromFile(rep, null, filename);

            if (mode.equals("--syntax-only")) {
                System.out.println("Syntax check passed! No errors found.");
                return;
            }

            A4Options options = new A4Options();

            for (Command command : world.getAllCommands()) {
                // If a specific command was requested, skip the others
                if (!targetCommand.equals("all") && !command.label.equals(targetCommand)) {
                    continue; 
                }

                System.out.println("Executing: " + command.label);
                A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);
                
                if (ans.satisfiable()) {
                    if (command.check) {
                        System.out.println("Result: Counterexample found! (Assertion failed)");
                    } else {
                        System.out.println("Result: Instance found. (Predicate is consistent)");
                    }
                    
                    System.out.println("\n--- Instance / Counterexample Details ---");
                    System.out.println(ans.toString());
                    System.out.println("-----------------------------------------\n");
                    
                } else {
                    if (command.check) {
                        System.out.println("Result: No counterexample found. (Assertion holds)");
                    } else {
                        System.out.println("Result: No instance found. (Predicate is inconsistent)");
                    }
                }
            }

        } catch (Err e) {
            // This catches syntax, type, and logic errors and formats them perfectly for Claude
            System.out.println("--- ALloy Error Detected ---");
            System.out.println("Error Type: " + e.getClass().getSimpleName());
            System.out.println("Location: Line " + e.pos.y + ", Column " + e.pos.x);
            System.out.println("Message: " + e.msg);
        }
    }
}