# occupy
an occupation game

# Requirements to build
* maven2 (https://maven.apache.org/)
* Python 2.6 (https://www.python.org/download/releases/2.6/)
* The xml mesh exporter (https://github.com/cbaakman/mesh-exporter)
* blender 2.49b (http://download.blender.org/release/)
* gimp (https://www.gimp.org/)

## Building the meshes
Use the blender xml exporter to export from the .blend files as xml into src/main/resources/mesh

## Building the images
Use gimp to export from the .xcf files as png into src/main/resources/images

## Building the jars
Run mvn package from the project root directory
