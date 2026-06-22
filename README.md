# Alloy MCP Server

A local Model Context Protocol (MCP) server that provides a headless bridge between Claude Desktop and the Alloy Analyzer (Kodkod SAT solver). This allows Claude to write, compile, and execute formal verification models and read the raw mathematical graph structures directly back into its context window.

## Features
* **Fast Syntax Checking:** Validates Alloy grammar without booting the heavy SAT solver.
* **Targeted Execution:** Isolate and test specific `run` or `check` commands.
* **Raw Graph Extraction:** Exposes the underlying atoms and relations (`ans.toString()`) so the AI can reason about the counterexample structurally.
* **Black Box Logging:** Automatically saves all system outputs and Java errors to a local text file for manual debugging.

## Prerequisites
To run this server locally, you must have the following installed:
* **Node.js** (v18 or higher)
* **Java Development Kit (JDK)** (v11 or higher)
* **Claude Desktop App**

## Installation & Setup

1. **Clone the repository:**
   ```bash
   git clone (https://github.com/joel6603/alloy-mcp-server.git)
   cd alloy-mcp-server

2. Install Node dependencies:
    ```bash
    npm install

3. Download the Alloy Engine:
    Download the official Alloy 6 .jar file from [AlloyTools](https://alloytools.org/).
    Rename the file to org.alloytools.alloy.dist.jar.
    Place it directly in the root folder of this project.

4. Compile the Java Backend
    # On Windows:
    javac -cp "org.alloytools.alloy.dist.jar;." RunAlloy.java

    # On Mac/Linux:
    javac -cp "org.alloytools.alloy.dist.jar:." RunAlloy.java

5. Connecting to Claude Desktop:
    Windows: %APPDATA%\Claude\claude_desktop_config.json

    Mac: ~/Library/Application Support/Claude/claude_desktop_config.json

    >> Add the server to your configuration, ensuring the absolute path points to where you cloned this repository:

    JSON
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

    >> Restart Claude Desktop completely.

    Available MCP Tools:

    check_alloy_syntax: Accepts raw .als code and instantly returns line/column syntax errors.

    run_alloy_model: Accepts raw .als code and an optional command_name. Executes the SAT solver and returns the resulting instances or counterexamples as formatted text.