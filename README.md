# Alloy MCP Server

A local Model Context Protocol (MCP) server that provides a headless bridge between Claude Desktop (or Roo Code) and the Alloy Analyzer (Kodkod SAT solver). This allows AI agents to write, compile, and execute formal verification models and read the raw mathematical graph structures directly back into their context window.

## Features
* **Fast Syntax Checking:** Validates Alloy grammar using file paths without booting the heavy SAT solver.
* **Asynchronous Execution & Timeouts:** Handles highly complex models that take minutes or hours to solve. The server safely detaches background tasks to prevent LLM tool timeouts.
* **Context Window Protection:** Automatically archives *all* discovered counterexamples locally to `all_counterexamples.txt`, but only returns the first 5 to the LLM to prevent token bloat.
* **Alloy 6 Temporal Support:** Natively unpacks and formats behavioral traces (`var` signatures) step-by-step so the LLM can easily read state mutations over time.

## Prerequisites
To run this server locally, you must have the following installed:
* **Node.js** (v18 or higher)
* **Java Development Kit (JDK)** (v11 or higher)
* **Claude Desktop App** (or Roo Code inside VS Code)

## Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone https://github.com/joel6603/alloy-mcp-server.git
   cd alloy-mcp-server
   ```

2. **Install Node dependencies:**
   ```bash
   npm install
   ```

3. **Download the Alloy Engine:**
   * Download the official Alloy 6 `.jar` file from [AlloyTools](https://alloytools.org/).
   * Rename the file to `org.alloytools.alloy.dist.jar`.
   * Place it directly in the root folder of this project.

4. **Compile the Java Backend:**
   ```bash
   # On Windows:
   javac -cp "org.alloytools.alloy.dist.jar;." RunAlloy.java

   # On Mac/Linux:
   javac -cp "org.alloytools.alloy.dist.jar:." RunAlloy.java
   ```

## Connecting to Claude Desktop

Locate your Claude Desktop configuration file:
* **Windows:** `%APPDATA%\Claude\claude_desktop_config.json`
* **Mac:** `~/Library/Application Support/Claude/claude_desktop_config.json`

Add the server to your configuration, ensuring the absolute path points to where you cloned this repository:

```json
{
  "mcpServers": {
    "alloy": {
      "command": "node",
      "args": [
        "D:/path/to/your/repo/alloy-mcp-server/index.js"
      ]
    }
  }
}
```
*Note: Restart Claude Desktop completely after saving this file.*

## Available MCP Tools

* **`check_alloy_syntax`**
  * **Input:** `filename` (Absolute path to the `.als` file).
  * **Description:** Reads the file directly from the hard drive and instantly returns line/column syntax errors without triggering the Kodkod solver.

* **`run_alloy_model`**
  * **Input:** `filename` (Absolute path) and an optional `commandName` (defaults to "all").
  * **Description:** Executes the SAT solver. 
    * If execution is fast, it returns a maximum of 5 formatted counterexamples to the chat.
    * If execution exceeds 45 seconds, it returns a safe "Background Execution Started" message to the AI while continuing to run silently.
    * In all cases, *every* discovered solution (up to 200) is saved to `all_counterexamples.txt` in the server root for manual review.
