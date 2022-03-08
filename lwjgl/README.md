# README 

A plugin which provides lwjgl libraries for plugins.
The plugin source code is under the GPL v2 or later.

It is "split" into platform-specific plugins:
* Each platform has its own "native" jar (if a
  platform supports more than one architecture,
  all architectures are in the jar file)

## Adding additional LWJGL libraries
Open a ticket on [JOSM Trac](https://josm.openstreetmap.de/newticket?cc=taylor.smock&keywords=lwjgl&component=Plugin)
with a request for the LWJGL library (note: to be eligible for this plugin, it
must be something recommended by LWJGL).

While not necessary, it would be nice to have a use case for the LWJGL that is
being added.

## Debugging
Depending upon what needs to be debugged, you can either start JOSM with the `--debug` flag _or_ you can start JOSM
with the VM argument `-javaagent:lwjglx-debug-1.0.0.jar` (see [lwjglx-debug](https://github.com/LWJGLX/debug)),
assuming you've customized the JOSM build process to include the `lwjgl` core module, like so:
```xml
	<dependency conf="api->default" org="org.lwjgl" name="lwjgl" rev="${lwjgl.version}">
		<artifact name="lwjgl" type="jar"/>
	</dependency>
```
Replace `${lwjgl.version}` with the version in ivy\_settings.xml. This is due to the javaagent requiring
`org.lwjgl.system.Configuration` to be available immediately, which it is not until JOSM loads the `lwjgl` plugin.
