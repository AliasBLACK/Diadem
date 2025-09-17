/*******************************************************************************
 * Copyright 2024
 * Mario Zechner <badlogicgames@gmail.com>
 * Nathan Sweet <nathan.sweet@gmail.com> 
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package black.alias.diadem;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.Value;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * WebGL2 Context that bridges JavaScript WebGL2 API calls to Java GLES implementation.
 * Provides a complete WebGL2-compatible interface for JavaScript applications.
 */
public class JSContext implements AutoCloseable {
    private final Context jsContext;
    
    public JSContext() {
        this.jsContext = Context.newBuilder("js")
            .allowAllAccess(true)
            .allowExperimentalOptions(true)
            .option("js.esm-eval-returns-exports", "true")
            .option("js.ecmascript-version", "2022")
            .build();
        
        // Load and initialize the WebGL2 bridge
        try {
            String bridgeScript = loadBridgeScript();
            jsContext.eval("js", bridgeScript);
            
        } catch (IOException e) {
            throw new RuntimeException("Failed to load WebGL2 bridge script", e);
        }
    }
    
    private String loadBridgeScript() throws IOException {
        // Load the bridge script from resources
        return new String(Files.readAllBytes(Paths.get("src/main/lib/lwjwebgl.js")));
    }
    
    /**
     * Execute JavaScript code with WebGL2 context available
     */
    public Value executeScript(String script) {
        return jsContext.eval("js", script);
    }
    
    /**
     * Execute ES6 module code with WebGL2 context available
     */
    public Value executeModule(String moduleCode) {
        try {
            return jsContext.eval(org.graalvm.polyglot.Source.newBuilder("js", moduleCode, "module.mjs")
                .mimeType("application/javascript+module")
                .build());
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute ES6 module", e);
        }
    }
    
    /**
     * Execute JavaScript code from a file
     */
    public Value executeScriptFile(String filename) throws IOException {
        String script = new String(Files.readAllBytes(Paths.get(filename)));
        return executeScript(script);
    }
    
    /**
     * Get the JavaScript context for advanced operations
     */
    public Context getJavaScriptContext() {
        return jsContext;
    }
    
    /**
     * Close the context and free resources
     */
    public void close() {
        jsContext.close();
    }
}
