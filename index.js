import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { exec } from "child_process";
import { promisify } from "util";
import fs from "fs/promises";
import path, { dirname } from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const execAsync = promisify(exec);

const server = new McpServer({
  name: "alloy-mcp-server",
  version: "2.0.1"
});

const CLASSPATH = `"org.alloytools.alloy.dist.jar;."`;

// --- TOOL 1: FAST SYNTAX CHECKER ---
server.tool(
  "check_alloy_syntax",
  "Validates Alloy code for syntax and type errors. Extremely fast. Use this before running full execution.",
  { alloy_code: z.string() },
  async ({ alloy_code }) => {
    const tempFilePath = path.join(__dirname, "temp_model.als");
    const command = `java -cp ${CLASSPATH} RunAlloy "${tempFilePath}" --syntax-only`;
    
    try {
      await fs.writeFile(tempFilePath, alloy_code);
      const { stdout, stderr } = await execAsync(command, { cwd: __dirname });
      return { content: [{ type: "text", text: stdout || stderr }] };
    } catch (error) {
      return { content: [{ type: "text", text: error.stdout || error.message }] };
    }
  }
);

// --- TOOL 2: TARGETED EXECUTOR ---
// I changed the name back to run_alloy_model so Claude doesn't get confused!
server.tool(
  "run_alloy_model",
  "Executes the SAT solver on the model. Returns instances or counterexamples.",
  {
    alloy_code: z.string(),
    // Changed from "optional" to "default" to prevent Claude from sending invalid JSON
    command_name: z.string().default("all").describe("The specific run/check label to execute. Leave as 'all' to run everything.")
  },
  async ({ alloy_code, command_name }) => {
    const tempFilePath = path.join(__dirname, "temp_model.als");
    const resultsFilePath = path.join(__dirname, "last_execution_results.txt");
    
    // Safely handle the command name
    const target = (command_name && command_name !== "all") ? `"${command_name}"` : "all";
    const command = `java -cp ${CLASSPATH} RunAlloy "${tempFilePath}" --run ${target}`;
    
    try {
      await fs.writeFile(tempFilePath, alloy_code);
      const { stdout, stderr } = await execAsync(command, { cwd: __dirname });
      
      let outputText = "";
      if (stdout) outputText += `${stdout}\n`;
      if (stderr) outputText += `--- Errors ---\n${stderr}\n`;

      await fs.writeFile(resultsFilePath, outputText);
      return { content: [{ type: "text", text: outputText }] };
      
    } catch (error) {
      const errorText = `Execution failed.\nCommand: ${command}\nStdout: ${error.stdout}\nSystem Error: ${error.message}`;
      await fs.writeFile(resultsFilePath, errorText);
      return { content: [{ type: "text", text: errorText }] };
    }
  }
);

async function start() {
  const transport = new StdioServerTransport();
  await server.connect(transport);
  console.error("Alloy MCP Server running on stdio");
}

start();