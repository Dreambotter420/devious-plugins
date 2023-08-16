version = "0.0.2"

project.extra["PluginName"] = "xblabla no external lol"
project.extra["PluginDescription"] = "Buys kebabs for me"

dependencies {
    implementation ("org.java-websocket:Java-WebSocket:1.5.3")
}

tasks {
    jar {
        manifest {
            attributes(mapOf(
                "Plugin-Version" to project.version,
                "Plugin-Id" to nameToId(project.extra["PluginName"] as String),
                "Plugin-Provider" to project.extra["PluginProvider"],
                "Plugin-Description" to project.extra["PluginDescription"],
                "Plugin-License" to project.extra["PluginLicense"]
            ))
        }
    }
}
