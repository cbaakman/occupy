# occupy
an occupation game

# Requirements to build
* maven 3 (https://maven.apache.org/download.cgi)
* Python 3 (https://www.python.org/downloads/)
* The xml mesh exporter (https://github.com/cbaakman/mesh-exporter)
* blender 2.79b (https://www.blender.org/download/)
* gimp 2 (https://www.gimp.org/downloads/)

## Building the resources
Run the build_resources.py script from inside the project directory.

## Building the jars
Run mvn package from the project root directory

## Updating a mesh
Use the blender xml exporter to export from the art/mesh/*.blend files as xml into src/main/resources/mesh

## Updating an image
Use gimp to export from the art/image/*.xcf files as png into src/main/resources/images
