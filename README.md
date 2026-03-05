# UCMIS M2M Transformation — Project Documentation

**Version:** 1.0  
**Last Updated:** 2026-03-05  
**Compatibility:** Eclipse 2025-12 · QVTo 3.11.1 · Java 21

---

## 1. Summary for a Non-Technical Audience

### What does this tool do?

The UCMIS M2M Transformation tool converts a UML model (a formal diagram describing the structure of data and concepts) from one representation into another. Specifically, it reads the **DDI-CDI** model — a widely used international standard for describing survey and research data concepts — and produces a **profile model** (such as DDSC or CDIF) that contains a carefully chosen subset of the DDI-CDI concepts, adapted to the requirements of that profile.

This conversion would traditionally require a data architect to manually replicate dozens of class definitions, attributes, and relationships across two large model files. The tool automates this fully: a single configuration file describes which concepts to include and how they should be mapped, and the tool does the rest.

### Why is this useful?

Different communities working with research data (statisticians, social scientists, archivists) use different but related conceptual models. The DDI-CDI model is the common reference point. Profile models derived from it must stay consistent with DDI-CDI while being lighter and more focused. Maintaining this consistency by hand is error-prone and time-consuming. This tool makes it repeatable, transparent, and auditable.

### What does the output look like?

The output is a standard UML model file (`.uml` format) that can be opened in any UML modelling tool that supports Eclipse UML2, such as Papyrus or Eclipse itself. The output contains:

- All selected classes, with their attributes and definitions.
- All selected associations between classes.
- A metadata block recording the model's name, version, language, and URI.
- Provenance records — machine-readable links back to the DDI-CDI concepts each profile concept was derived from, in both human-readable and RDF Turtle formats.

### What does a user need to do?

The only file a user normally needs to edit is the **JSON configuration file** (described in detail in section 5.7). This file lists which classes, attributes, and associations from DDI-CDI should appear in the profile model, and allows overriding names, definitions, and multiplicities as needed. The tool is then run from the command line. No programming is required to operate the tool once it is built.

---

## 2. Requirements

### 2.1 Runtime Requirements

The built product is a self-contained, headless Eclipse application. It includes all necessary Eclipse and EMF runtime libraries. The only external requirement to run the built binary is:

| Requirement | Version | Notes |
|---|---|---|
| Java (JRE or JDK) | 21 or later | OpenJDK or Oracle JDK both supported |

No Eclipse installation is needed at runtime. The tool is distributed as a directory containing a launcher executable and a `plugins/` directory.

### 2.2 Build Requirements

To build the tool from source, the following are required:

| Requirement | Version | Notes |
|---|---|---|
| Java Development Kit (JDK) | 21 | Must be JDK (not just JRE) |
| Apache Maven | 3.9 or later | Standard Maven installation |
| Internet access | — | Required to download Eclipse p2 dependencies during first build |

The build system uses **Eclipse Tycho** (a Maven plugin for building Eclipse applications). Tycho downloads all Eclipse dependencies automatically from the configured p2 repositories. No manual Eclipse installation is required to build.

### 2.3 Eclipse IDE Requirements (Development Only)

For working on the source code in Eclipse:

| Requirement | Version |
|---|---|
| Eclipse IDE for RCP and RAP Developers | 2025-12 |
| Eclipse QVTo SDK (installed via Help → Install New Software) | 3.11.1 |
| Eclipse UML2 (installed via Help → Install New Software) | 5.x (part of Eclipse Modeling Tools) |

The target platform file `ucmism2m.target/ucmism2m.target.target` must be activated in Eclipse before any development work (Window → Preferences → Plug-in Development → Target Platform → activate `UCMIS M2M Target Platform`).

---

## 3. Usage: Building and Running the Tool

### 3.1 Building

From the project root directory (the directory containing the top-level `pom.xml`), run:

```bash
# Build for the current platform only (Linux x86_64 by default — fastest)
mvn clean verify

# Build for all platforms (Linux, Windows, macOS x86_64 and ARM)
mvn clean verify -P all-platforms

# Build for Windows only
mvn clean verify -P windows-only

# Build for macOS only
mvn clean verify -P macos-only
```

The build output is placed in:

```
ucmism2m.product/target/products/ucmism2m/
```

Platform-specific ZIP archives are also created in that directory:

| Archive | Platform |
|---|---|
| `ucmism2m-linux.gtk.x86_64.zip` | Linux 64-bit |
| `ucmism2m-win32.win32.x86_64.zip` | Windows 64-bit |
| `ucmism2m-macosx.cocoa.x86_64.zip` | macOS Intel |
| `ucmism2m-macosx.cocoa.aarch64.zip` | macOS Apple Silicon |

### 3.2 Location of Binaries

After building and extracting the ZIP for your platform, the product directory has the following layout:

```
ucmism2m/                       ← root of the extracted archive
├── ucmism2m                    ← launcher executable (Linux/macOS)
├── ucmism2m.exe                ← launcher executable (Windows)
├── plugins/                    ← all OSGi bundles
│   ├── ucmism2m.app_1.0.0...jar
│   ├── ucmism2m.blackbox_1.0.0...jar
│   ├── ucmism2m.transformation_1.0.0...jar
│   └── ... (Eclipse runtime bundles)
└── configuration/
    └── config.ini
```

The main executable is `ucmism2m` (Linux/macOS) or `ucmism2m.exe` (Windows) in the root of the extracted directory.

### 3.3 Running the Tool

```bash
./ucmism2m \
  -input  /path/to/source-model.uml \
  -output /path/to/output-model.uml \
  -config /path/to/mapping-config.json
```

**Arguments:**

| Argument | Required | Description |
|---|---|---|
| `-input <path>` | Yes | Path to the source UML model file (`.uml` format, Eclipse UML2) |
| `-output <path>` | Yes | Path where the output UML model will be written (`.uml` format) |
| `-config <path>` | Yes | Path to the JSON mapping configuration file |

**Example (DDI-CDI to DDSC):**

```bash
./ucmism2m \
  -input  /data/models/DDI-CDI.uml \
  -output /data/models/DDSC.uml \
  -config /data/configs/ddi-cdi_to_ddsc.json
```

### 3.4 Interpreting Output

The tool prints progress to standard output:

```
UCMIS M2M Transformation Application
Eclipse 2025-12 | QVTo 3.11.1 | Java 21
=====================================
Input model: /data/models/DDI-CDI.uml
Output model: /data/models/DDSC.uml
Configuration: /data/configs/ddi-cdi_to_ddsc.json

Loading input model...
Input model loaded: 1 root elements
Loading transformation...
Transformation loaded successfully

Executing transformation...
Transformation completed successfully!

Saving output model...
Output model saved: /data/models/DDSC.uml
```

If the transformation fails, an error and stack trace are printed to standard error and the process exits with code 1.
