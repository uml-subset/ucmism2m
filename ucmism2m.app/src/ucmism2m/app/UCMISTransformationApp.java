package ucmism2m.app;

import org.eclipse.emf.common.util.Diagnostic;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;
import org.eclipse.m2m.qvt.oml.BasicModelExtent;
import org.eclipse.m2m.qvt.oml.ExecutionContextImpl;
import org.eclipse.m2m.qvt.oml.ExecutionDiagnostic;
import org.eclipse.m2m.qvt.oml.ModelExtent;
import org.eclipse.m2m.qvt.oml.TransformationExecutor;
import org.eclipse.m2m.qvt.oml.util.WriterLog;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;

import java.io.File;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * UCMIS Model-to-Model Transformation Application.
 * 
 * Headless Eclipse application for executing QVTo transformations.
 * Compatible with Eclipse 2025-12, QVTo 3.11.1, Java 21.
 * 
 * Command line arguments:
 * -input <path>      Path to input UML model
 * -output <path>     Path to output UML model
 * -config <path>     Path to JSON configuration file
 */
public class UCMISTransformationApp implements IApplication {
    
    private static final String ARG_INPUT = "-input";
    private static final String ARG_OUTPUT = "-output";
    private static final String ARG_CONFIG = "-config";
    
    private static final String TRANSFORMATION_URI = 
        "platform:/plugin/ucmism2m.transformation/transforms/m2m.qvto";
    
    @Override
    public Object start(IApplicationContext context) throws Exception {
        System.out.println("UCMIS M2M Transformation Application");
        System.out.println("Eclipse 2025-12 | QVTo 3.11.1 | Java 21");
        System.out.println("=====================================");
        
        // Parse command line arguments
        String[] args = (String[]) context.getArguments().get("application.args");
        Map<String, String> arguments = parseArguments(args);
        
        // Validate arguments
        if (!validateArguments(arguments)) {
            printUsage();
            return IApplication.EXIT_OK;
        }
        
        String inputPath = arguments.get(ARG_INPUT);
        String outputPath = arguments.get(ARG_OUTPUT);
        String configPath = arguments.get(ARG_CONFIG);
        
        System.out.println("Input model: " + inputPath);
        System.out.println("Output model: " + outputPath);
        System.out.println("Configuration: " + configPath);
        System.out.println();
        
        try {
            // Execute transformation
            executeTransformation(inputPath, outputPath, configPath);
            
            System.out.println("\nTransformation completed successfully!");
            return IApplication.EXIT_OK;
            
        } catch (Exception e) {
            System.err.println("\nTransformation failed: " + e.getMessage());
            e.printStackTrace();
            return Integer.valueOf(1);
        }
    }
    
    @Override
    public void stop() {
        // Cleanup if needed
    }
    
    /**
     * Execute the QVTo transformation.
     */
    private void executeTransformation(String inputPath, String outputPath, String configPath) 
            throws Exception {
        
        // Initialize UML resources
        UMLPackage.eINSTANCE.eClass();
        
        // Create resource set
        ResourceSet resourceSet = new ResourceSetImpl();
        resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap()
            .put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
        resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
        
        // Load input model
        System.out.println("Loading input model...");
        URI inputURI = createFileURI(inputPath);
        Resource inputResource = resourceSet.getResource(inputURI, true);
        
        if (inputResource == null || inputResource.getContents().isEmpty()) {
            throw new RuntimeException("Failed to load input model: " + inputPath);
        }
        
        System.out.println("Input model loaded: " + inputResource.getContents().size() + " root elements");
        
        // Create model extents
        ModelExtent inputExtent = new BasicModelExtent(inputResource.getContents());
        ModelExtent outputExtent = new BasicModelExtent();
        
        // Load transformation
        System.out.println("Loading transformation...");
        URI transformationURI = URI.createURI(TRANSFORMATION_URI);
        TransformationExecutor executor = new TransformationExecutor(transformationURI);
        
        // Validate transformation loaded successfully
        Diagnostic loadDiagnostic = executor.loadTransformation();
        if (loadDiagnostic.getSeverity() == Diagnostic.ERROR) {
            System.err.println("Failed to load transformation:");
            printDiagnostic(loadDiagnostic, "  ");
            throw new RuntimeException("Failed to load transformation: " + TRANSFORMATION_URI);
        }
        
        System.out.println("Transformation loaded successfully");
        
        // Set up execution context
        ExecutionContextImpl executionContext = new ExecutionContextImpl();
        executionContext.setLog(new WriterLog(
            new OutputStreamWriter(System.out, StandardCharsets.UTF_8)));
        
        // Set configuration property
        if (configPath != null && !configPath.isEmpty()) {
            executionContext.setConfigProperty("configPath", configPath);
        }
        
        // Execute transformation
        System.out.println("\nExecuting transformation...");
        ExecutionDiagnostic result = executor.execute(
            executionContext, 
            inputExtent, 
            outputExtent
        );
        
        // Check execution result
        if (result.getSeverity() == Diagnostic.OK) {
            System.out.println("Transformation executed successfully");
        } else {
            System.err.println("Transformation execution diagnostic:");
            printDiagnostic(result, "");
            
            if (result.getSeverity() == Diagnostic.ERROR) {
                throw new RuntimeException("Transformation failed with errors");
            }
        }
        
        // Save output model
        System.out.println("\nSaving output model...");
        saveOutputModel(outputExtent, outputPath, resourceSet);
        
        System.out.println("Output model saved: " + outputPath);
    }
    
    /**
     * Save the output model extent to file.
     */
    private void saveOutputModel(ModelExtent outputExtent, String outputPath, 
                                 ResourceSet resourceSet) throws Exception {
        
        List<EObject> outObjects = outputExtent.getContents();
        
        if (outObjects.isEmpty()) {
            System.out.println("Warning: Output model is empty");
            return;
        }
        
        // Create output resource
        URI outputURI = createFileURI(outputPath);
        Resource outputResource = resourceSet.createResource(outputURI);
        
        // Add output objects
        outputResource.getContents().addAll(outObjects);
        
        // Resolve all proxies
        EcoreUtil.resolveAll(resourceSet);
        
        // Save with options
        Map<String, Object> saveOptions = new HashMap<>();
        saveOptions.put(Resource.OPTION_SAVE_ONLY_IF_CHANGED, Resource.OPTION_SAVE_ONLY_IF_CHANGED_MEMORY_BUFFER);
        saveOptions.put(Resource.OPTION_LINE_DELIMITER, Resource.OPTION_LINE_DELIMITER_UNSPECIFIED);
        
        outputResource.save(saveOptions);
    }
    
    /**
     * Create file URI with proper platform resolution.
     */
    private URI createFileURI(String path) {
        File file = new File(path);
        return URI.createFileURI(file.getAbsolutePath());
    }
    
    /**
     * Parse command line arguments.
     */
    private Map<String, String> parseArguments(String[] args) {
        Map<String, String> result = new HashMap<>();
        
        if (args == null) {
            return result;
        }
        
        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-") && i + 1 < args.length) {
                result.put(args[i], args[i + 1]);
                i++; // Skip next arg
            }
        }
        
        return result;
    }
    
    /**
     * Validate required arguments.
     */
    private boolean validateArguments(Map<String, String> args) {
        return args.containsKey(ARG_INPUT) && 
               args.containsKey(ARG_OUTPUT) && 
               args.containsKey(ARG_CONFIG);
    }
    
    /**
     * Print usage information.
     */
    private void printUsage() {
        System.out.println("\nUsage:");
        System.out.println("  ucmism2m -input <input.uml> -output <output.uml> -config <config.json>");
        System.out.println("\nArguments:");
        System.out.println("  -input   Path to input UML model file");
        System.out.println("  -output  Path to output UML model file");
        System.out.println("  -config  Path to JSON configuration file");
    }
    
    /**
     * Print diagnostic information recursively.
     */
    private void printDiagnostic(Diagnostic diagnostic, String indent) {
        System.out.println(indent + diagnostic.getMessage());
        
        for (Diagnostic child : diagnostic.getChildren()) {
            printDiagnostic(child, indent + "  ");
        }
    }
}
