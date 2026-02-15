# Development Guide

## Project Structure

- **ucmism2m.blackbox**: Java black-box operations for JSON configuration
- **ucmism2m.transformation**: QVTo transformation scripts
- **ucmism2m.feature**: Eclipse feature bundling all components
- **ucmism2m.app**: Headless application launcher
- **ucmism2m.product**: Product configuration for executable export

## Building
```bash
mvn clean verify
```

## Testing in Eclipse

1. Import all projects
2. Set target platform
3. Run QVTo transformations from IDE

## Adding Black-box Operations

1. Add method to `JSONConfigLoader.java`
2. Add query definition to `JSONConfigLoaderLib.qvto`
3. Rebuild project
