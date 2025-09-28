# CodeQL Docker Usage Guide

This guide shows how to use the CodeQL Docker containers for security analysis.

## Quick Start

⚠️ **Important**: After pulling the latest changes, rebuild the Docker image to ensure all CodeQL libraries are properly installed.

### 1. Build the Docker Image

```bash
# For x64/AMD64 systems (Intel/AMD processors)
docker build -f codeql/Dockerfile -t codeql:latest .

# For ARM64 systems (Apple Silicon M1/M2 Macs)
docker build -f codeql/Dockerfile.arm64 -t codeql:arm64 .
```

## Sample Analysis Walkthrough

The `/samples` directory contains:
- `sample.js` - A vulnerable Node.js Express app with XSS vulnerabilities
- `sample.ql` - A complete CodeQL query for detecting XSS (students can study and modify)
- `.gitignore` - Keeps the directory clean

### Step-by-Step Analysis

1. **Start the container with samples mounted:**
```bash
# Linux/Mac
docker run -it --rm -v $(pwd)/codeql/samples:/workspace codeql:arm64

# Windows PowerShell
docker run -it --rm -v "${PWD}/codeql/samples:/workspace" codeql:latest

# Windows Command Prompt
docker run -it --rm -v "%cd%/codeql/samples:/workspace" codeql:latest
```

2. **Create a CodeQL database:**
```bash
cd /workspace
codeql database create myapp-db --language=javascript --source-root=.
```

**Note:** If you need to recreate the database, first remove the existing one:
```bash
rm -rf myapp-db
codeql database create myapp-db --language=javascript --source-root=.
```

3. **Run the sample query:**
```bash
codeql pack download codeql/javascript-all
codeql query run sample.ql --database=myapp-db
```

result
```
codeql@9f3d6ac9c25b:/workspace$ codeql query run sample.ql --database=myapp-db
Compiling query plan for /workspace/sample.ql.
[1/1 comp 9.5s] Compiled /workspace/sample.ql.
sample.ql: Evaluation completed (614ms).
|      template       |                                        col1                                        |
+---------------------+------------------------------------------------------------------------------------+
| `<h1>Se ... }</h1>` | XSS vulnerability at line 11: Template literal contains unescaped variable 'query' |
| `<h1>We ... !</h1>` | XSS vulnerability at line 18: Template literal contains unescaped variable 'name'  |
Shutting down query evaluator.
```

4. **Run analysis with output (csv):**
```bash
codeql database analyze myapp-db sample.ql --format=csv --output=results.csv
```

5. **View results:**
```bash
cat results.csv
```

## Sample Vulnerabilities in sample.js

The sample application contains the following XSS vulnerabilities:

1. **Search endpoint (`/search`)**
   - User input from `req.query.q` directly inserted into HTML
   - Test: `/search?q=<script>alert("XSS")</script>`

2. **User profile (`/user/:name`)**
   - URL parameter directly inserted into HTML
   - Test: `/user/<img src=x onerror=alert("XSS")>`

3. **Safe endpoint (`/safe`)**
   - Shows proper HTML escaping for comparison

## Student Exercise: Complete the CodeQL Query

The `sample.ql` file is intentionally incomplete. Students should:

1. **Understand the vulnerability pattern:**
   - User input sources: `req.query`, `req.params`
   - Dangerous sinks: `res.send()` with unescaped user data

2. **Complete the query to detect:**
   - Template literals that contain user input
   - Property access on request objects
   - Data flow from sources to sinks

3. **Advanced challenges:**
   - Add data flow analysis
   - Detect different types of XSS (reflected, stored, DOM-based)
   - Add sanitizer detection

## Useful CodeQL Resources

- **Official Documentation:** https://codeql.github.com/docs/
- **Query Examples:** https://github.com/github/codeql/tree/main/javascript/ql/src/Security
- **Language Reference:** https://codeql.github.com/docs/ql-language-reference/
- **JavaScript Libraries:** https://codeql.github.com/docs/codeql-language-guides/analyzing-javascript-and-typescript/


## Troubleshooting

### Common Issues

1. **Database creation fails:**
   - Ensure source code is properly mounted
   - Check language detection with `codeql resolve languages`
   - If database already exists, remove it first: `rm -rf myapp-db`
   - For permission issues, try: `sudo rm -rf myapp-db` (if needed)

2. **Query compilation errors:**
   - check existence of `qlpack.yml`
   - Verify query syntax with `codeql query compile`
   - Check import statements  
   - The container includes all necessary CodeQL libraries
   - If imports fail, try rebuilding the container

3. **No results found:**
   - Verify database was created successfully
   - Test with simpler queries first
   - Check query logic step by step

### Getting Help

```bash
# CodeQL help
codeql --help
codeql database --help
codeql query --help

# Check CodeQL version
codeql version

# List available languages
codeql resolve languages
```

## Security Note

Always run CodeQL containers with appropriate security measures:
- Use `--rm` flag to remove containers after use
- Avoid running as root in production
- Be careful with volume mounts to prevent data exposure
