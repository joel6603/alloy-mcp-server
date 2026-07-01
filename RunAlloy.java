import edu.mit.csail.sdg.alloy4.A4Reporter;
import edu.mit.csail.sdg.alloy4.Err;
import edu.mit.csail.sdg.ast.Command;
import edu.mit.csail.sdg.ast.Module;
import edu.mit.csail.sdg.parser.CompUtil;
import edu.mit.csail.sdg.translator.A4Options;
import edu.mit.csail.sdg.translator.A4Solution;
import edu.mit.csail.sdg.translator.TranslateAlloyToKodkod;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

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
            Module world = CompUtil.parseEverything_fromFile(rep, null, filename);

            if (mode.equals("--syntax-only")) {
                System.out.println("Syntax check passed! No errors found.");
                return;
            }

            A4Options options = new A4Options();

            try (PrintWriter fileWriter = new PrintWriter(new FileWriter("all_counterexamples.txt"))) {
                
                for (Command command : world.getAllCommands()) {
                    if (!targetCommand.equals("all") && !command.label.equals(targetCommand)) {
                        continue;
                    }

                    System.out.println("Executing command: " + command.label);
                    fileWriter.println("=== Execution Log for Command: " + command.label + " ===");
                    
                    A4Solution ans = TranslateAlloyToKodkod.execute_command(rep, world.getAllReachableSigs(), command, options);
                    
                    if (!ans.satisfiable()) {
                        String msg = command.check ? "Result: No counterexample found. (Assertion holds)" : "Result: No instance found. (Predicate is inconsistent)";
                        System.out.println(msg);
                        fileWriter.println(msg);
                        continue;
                    }

                    String msg = command.check ? "Result: Counterexample(s) found! (Assertion failed)" : "Result: Instance(s) found. (Predicate is consistent)";
                    System.out.println(msg);
                    fileWriter.println(msg);

                    int instanceCount = 0;
                    int maxFileInstances = 200; 

                    while (ans.satisfiable() && instanceCount < maxFileInstances) {
                        instanceCount++;
                        
                        // 1. ALWAYS write the native graph data (static or temporal) to the local file
                        fileWriter.println("\n=========================================");
                        fileWriter.println("--- Solution #" + instanceCount + " ---");
                        fileWriter.println("=========================================");
                        fileWriter.println(ans.toString());
                        
                        // 2. ONLY print the first 5 to the console to save LLM tokens
                        if (instanceCount <= 5) {
                            System.out.println("\n=========================================");
                            System.out.println("--- Solution #" + instanceCount + " (Sent to LLM) ---");
                            System.out.println("=========================================");
                            System.out.println(ans.toString());
                        }
                        
                        ans = ans.next();
                    }
                    
                    System.out.println("\n[Note: " + instanceCount + " total solutions found. All archived in 'all_counterexamples.txt'.]");
                    if (instanceCount > 5) {
                        System.out.println("[LLM displayed the first 5 to protect context window limits.]");
                    }
                    System.out.println("-----------------------------------------\n");
                    
                    fileWriter.println("\nTotal solutions archived: " + instanceCount);
                    fileWriter.println("-----------------------------------------\n");
                }
            } catch (IOException e) {
                System.err.println("Warning: Failed to write to 'all_counterexamples.txt': " + e.getMessage());
            }

        } catch (Err e) {
            System.out.println("--- Alloy Error Detected ---");
            System.out.println("Error Type: " + e.getClass().getSimpleName());
            System.out.println("Location: Line " + e.pos.y + ", Column " + e.pos.x);
            System.out.println("Message: " + e.msg);
        }
    }
}