package com.webex.agentic.gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Registry that maps primitives to their runtime configurations
 */
@Service
public class PluginRegistry {
    
    private static final Logger log = LoggerFactory.getLogger(PluginRegistry.class);

    private final Map<String, PluginSpec> registry = new HashMap<>();

    public PluginRegistry() {
        // Register plugins
        registry.put("add_numbers", new PluginSpec(
            "add_numbers",
            "1.0.0",
            "java",
            "java-runtime-supervisor:9091",
            "AddPlugin"
        ));

        registry.put("multiply_numbers", new PluginSpec(
            "multiply_numbers",
            "1.0.0",
            "java",
            "java-runtime-supervisor:9091",
            "MultiplyPlugin"
        ));

        registry.put("subtract_numbers", new PluginSpec(
            "subtract_numbers",
            "1.0.0",
            "python",
            "python-runtime-supervisor:9092",
            "subtract_plugin.py"
        ));

        log.info("Registered {} plugins", registry.size());
    }

    public PluginSpec getPlugin(String primitiveId) {
        PluginSpec spec = registry.get(primitiveId);
        if (spec == null) {
            throw new IllegalArgumentException("Unknown primitive: " + primitiveId);
        }
        return spec;
    }

    public static class PluginSpec {
        private final String id;
        private final String version;
        private final String language;
        private final String runtimeAddress;
        private final String entrypoint;
        
        public PluginSpec(String id, String version, String language, String runtimeAddress, String entrypoint) {
            this.id = id;
            this.version = version;
            this.language = language;
            this.runtimeAddress = runtimeAddress;
            this.entrypoint = entrypoint;
        }
        
        public String getId() {
            return id;
        }
        
        public String getVersion() {
            return version;
        }
        
        public String getLanguage() {
            return language;
        }
        
        public String getRuntimeAddress() {
            return runtimeAddress;
        }
        
        public String getEntrypoint() {
            return entrypoint;
        }
    }
}

