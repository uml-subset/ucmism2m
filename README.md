# UCMIS M2M Transformation

UML Model-to-Model transformation using Eclipse QVTo.

## Technology Stack

- Eclipse Modeling Tools 2025-12
- QVTo 3.11.1
- Tycho 5.0
- Java 21
- UML2 5.0.0

## Project Structure
```
ucmism2m/
├── ucmism2m.target/           # Target platform definition
├── ucmism2m.blackbox/         # Java black-box operations
├── ucmism2m.transformation/   # QVTo transformation scripts
├── ucmism2m.feature/          # Eclipse feature
├── ucmism2m.app/              # Headless application
└── ucmism2m.product/          # Product configuration
```

## Build
```bash
mvn clean verify
```

## Usage
```bash
./ucmism2m \
  -input /path/to/input.uml \
  -output /path/to/output.uml \
  -config /path/to/config.json
```

## Documentation

See `docs/` directory for detailed documentation.

## License

[Add your license here]
