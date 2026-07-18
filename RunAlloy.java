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
        if (args.length == 0) {
            System.err.println("Error: No file provided.");
            return;
        }

        String filename = args[0];
        boolean syntaxOnly = false;
        String targetCommand = "all";
        
        // Dynamic limits with defaults
        int maxArchive = 200;
        int maxReturn = 5;

        // Parse incoming command-line arguments from Node.js
        for (int i = 1; i < args.length; i++) {
            if (args[i].equals("--syntax-only")) {
                syntaxOnly = true;
            } else if (args[i].equals("--run") && i + 1 < args.length) {
                targetCommand = args[++i];
                if (targetCommand.startsWith("\"") && targetCommand.endsWith("\"")) {
                    targetCommand = targetCommand.substring(1, targetCommand.length() - 1);
                }
            } else if (args[i].equals("--max-archive") && i + 1 < args.length) {
                maxArchive = Integer.parseInt(args[++i]);
            } else if (args[i].equals("--max-return") && i + 1 < args.length) {
                maxReturn = Integer.parseInt(args[++i]);
            }
        }

        A4Reporter rep = new A4Reporter();

        try {
            Module world = CompUtil.parseEverything_fromFile(rep, null, filename);

            if (syntaxOnly) {
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

                    while (ans.satisfiable() && instanceCount < maxArchive) {
                        instanceCount++;
                        
                        // 1. ALWAYS write to the local file up to maxArchive
                        fileWriter.println("\n=========================================");
                        fileWriter.println("--- Solution #" + instanceCount + " ---");
                        fileWriter.println("=========================================");
                        fileWriter.println(ans.toString());
                        
                        // 2. ONLY print to the console up to maxReturn
                        if (instanceCount <= maxReturn) {
                            System.out.println("\n=========================================");
                            System.out.println("--- Solution #" + instanceCount + " (Sent to LLM) ---");
                            System.out.println("=========================================");
                            System.out.println(ans.toString());
                        }
                        
                        ans = ans.next();
                    }
                    
                    System.out.println("\n[Note: " + instanceCount + " total solutions found. All archived in 'all_counterexamples.txt'.]");
                    if (instanceCount > maxReturn) {
                        System.out.println("[LLM displayed the first " + maxReturn + " to protect context window limits.]");
                    }
                    System.out.println("-----------------------------------------\n");
                    
                    fileWriter.println("\nTotal solutions archived: " + instanceCount);
                    fileWriter.println("-----------------------------------------\n");
                }
            } catch (IOException e) {
                System.err.println("Warning: Failed to write to 'all_counterexamples.txt': " + e.getMessage());
            }

        // Your robust error catching remains intact!
        } catch (Err e) {
            System.out.println("--- Alloy Error Detected ---");
            System.out.println("Error Type: " + e.getClass().getSimpleName());
            System.out.println("Location: Line " + e.pos.y + ", Column " + e.pos.x);
            System.out.println("Message: " + e.msg);
        } catch (Exception e) {
            System.out.println("--- System Error Detected ---");
            System.out.println(e.getMessage());
        }
    }
}