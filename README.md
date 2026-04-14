# Offensive 360 SAST — Eclipse Plugin

Deep source code analysis for Eclipse IDE. Scan your projects for security vulnerabilities with a single click.

## Requirements

- Eclipse IDE 2022-09 or later
- Java 17+
- An Offensive 360 server instance and valid access token

## Installation

### From Update Site (recommended)

1. Open Eclipse → **Help → Install New Software**
2. Click **Add** and enter:
   - Name: `O360 SAST`
   - Location: `https://github.com/offensive360/EclipsePlugin/raw/main`
3. Select **O360 SAST** and click **Next**
4. Accept the license and restart Eclipse

### Manual Installation

1. Download the latest `.jar` from [Releases](https://github.com/offensive360/EclipsePlugin/releases)
2. Place it in your Eclipse `dropins/` folder
3. Restart Eclipse

## Configuration

Press **Ctrl+Alt+D** or click the settings icon in the O360 toolbar:

- **Server URL**: Your Offensive 360 server (e.g. `https://your-server.com`)
- **Access Token**: Generated from the O360 dashboard under Settings → Tokens
- **Allow self-signed SSL certificates**: Enable for on-premise instances

## How to Use

### Scanning

- Press **Ctrl+Alt+S** to scan the current project
- Or right-click a project → **O360 SAST: Scan**
- Progress is shown in the Eclipse status bar

### Viewing Results

Results appear in the **O360 Report** tab at the bottom of the IDE:

- **Tree view** grouped by severity (Critical / High / Medium / Low)
- **Details tab**: vulnerability description, impact, affected code
- **How to Fix tab**: step-by-step remediation guidance
- **References tab**: OWASP, CWE, and related links
- **Double-click** any finding to jump to the vulnerable line

### Context Menu

Right-click a finding for:
- **Go to Code** — navigate to the vulnerable line
- **Suppress** — mark as false positive
- **Get Help** — view references and fix guidance
- **Clear All** — remove all findings

## Features

- Smart caching: zero server requests when no files changed
- 6 retries with exponential backoff for server errors
- 4-hour timeout for large projects
- Base64 code snippet decoding
- Identical file exclusion rules as VS/AS/VSCode plugins
- Check for Updates notification

## Support

For issues and feature requests, open an issue on this repository.
