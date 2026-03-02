const JsonSchemaStaticDocs = require("json-schema-static-docs");

(async () => {
  const docs = new JsonSchemaStaticDocs({
    inputPath: ".",  // folder with .json/.yml schemas
    outputPath: "./docs",
    jsonSchemaVersion: "https://json-schema.org/draft/2020-12/schema",
    ajvOptions: { allowUnionTypes: true }
  });
  await docs.generate();
  console.log("HTML/MD docs generated.");
})();
