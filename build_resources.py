#!/usr/bin/python3

import tempfile
import sys
from glob import glob
import os
import subprocess
from string import ascii_uppercase
from zipfile import ZipFile


windows_drives = list(ascii_uppercase)


def find_gimp():
    for drive in windows_drives:
        for path in glob("%s:\\Program Files*\\GIMP 2\\bin\\gimp-console-2.*.exe" % drive):
            return path

    for path in ["/usr/bin/gimp-console", "/usr/local/bin/gimp-console"]:
        if os.path.isfile(path):
            return path

    raise FileNotFoundError("cannot find gimp executable, please install")


def find_blender():
    for drive in windows_drives:
        for path in glob("%s:\\Program Files*\\Blender Foundation\\Blender\\blender.exe" % drive):
            return path

    for path in ["/usr/local/bin/blender", "/usr/bin/blender"]:
        if os.path.isfile(path):
            return path

    raise FileNotFoundError("cannot find blender executable, please install")


def find_xml_exporter():
    home = os.getenv('HOME')
    appdata = os.getenv('APPDATA')

    if home is not None:
        for path in glob("%s/.config/blender/*/scripts/addons/xml_exporter.py" % home):
            return path

        for path in glob("%s/.blender/*/scripts/addons/xml_exporter.py" % home):
            return path

    if appdata is not None:
        for path in glob("%s\\Blender Foundation\\Blender\\*\\scripts\\addons\\xml_exporter.py" % appdata):
            return path

    for path in ["xml_exporter.py", "/usr/share/blender/scripts/addons/xml_exporter.py"]:
        if os.path.isfile(path):
            return path

    raise FileNotFoundError("cannot find xml exporter, please download from \
                             https://github.com/cbaakman/mesh-exporter")


def mkdirs(path):
    dirname, basename =os.path.split(path)
    if dirname != '':
        mkdirs(dirname)
    if not os.path.isdir(path):
        os.mkdir(path)


# Verify that all necessary software is present and derive their paths:
blender_exe = find_blender()
xml_exporter = find_xml_exporter()
gimp_exe = find_gimp()


# Build directories if not present:
resource_dir = os.path.join('src', 'main', 'resources', 'net', 'cbaakman', 'occupy')
mesh_dir = os.path.join(resource_dir, 'mesh')
image_dir = os.path.join(resource_dir, 'image')
map_test_dir = os.path.join('src', 'test', 'resources', 'map')
mkdirs(mesh_dir)
mkdirs(image_dir)
mkdirs(map_test_dir)


# Build meshes:
mesh_list = [(os.path.join('art', 'mesh', 'infantry.blend'), 'infantry', os.path.join(mesh_dir, 'infantry.xml')),
             (os.path.join('art', 'mesh', 'terrain.blend'), 'terrain', os.path.join(map_test_dir, 'terrain.xml'))]
for blend_path, name, xml_path in mesh_list:
    r = subprocess.call([
        blender_exe, blend_path, '--background', '--python', xml_exporter,
        '--', name, xml_path
    ])
    if r != 0:
        sys.exit(r)

    if not os.path.isfile(xml_path):
        raise RuntimeError("%s was not generated" % xml_path)


# Build images:
image_list = [(os.path.join('art', 'image', 'infantry.xcf'), 'base', os.path.join(image_dir, 'infantry.png')),
              (os.path.join('art', 'image', 'infantry.xcf'), 'color', os.path.join(image_dir, 'infantry_color.png')),
              (os.path.join('art', 'image', 'green.xcf'), 'base', os.path.join(map_test_dir, 'green.png'))]
error_path = tempfile.mktemp()
script_str = """
import gimpfu
import os
import traceback

def convert(xcf_path, layer_name, png_path):
    img = pdb.gimp_file_load(xcf_path, xcf_path)
    for layer in img.layers:
        if layer.name == layer_name:
            pdb.gimp_layer_resize(layer, img.width, img.height, 0, 0)
            pdb.gimp_file_save(img, layer, png_path, png_path)
    pdb.gimp_image_delete(img)
    if not os.path.isfile(png_path):
        raise RuntimeError("%%s was not generated" %% png_path)


try:
    for xcf_path, name, png_path in %s:
        convert(xcf_path, name, png_path)
except:
    traceback.print_exc(file=open('%s', 'w'))
""" % (str(image_list), error_path)

subprocess.call([
    gimp_exe, '--verbose', '-n', '--batch-interpreter=python-fu-eval',
    '-b', script_str,
    '-b', "pdb.gimp_quit(1)"
])
# Since gimpfu always exits with 0, we must set an error flag on a file:
if os.path.isfile(error_path):
    message = open(error_path, 'r').read()
    os.remove(error_path)
    raise Exception(message)

# Store the map components in an archive:
map_contents = [os.path.join(map_test_dir, 'terrain.xml'),
                os.path.join(map_test_dir, 'green.png')]
map_archive = os.path.join(map_test_dir, 'testmap.zip')
with ZipFile(map_archive, 'w') as z:
    for path in map_contents:
        z.write(path, os.path.basename(path))
        os.remove(path)
    z.writestr('info.txt', "name: test")

print("\nAll resources have been built successfully.")
