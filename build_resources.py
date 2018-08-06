#!/usr/bin/python3

import tempfile
import sys
from glob import glob
import os
import subprocess
from string import ascii_uppercase


image_list = ['infantry']
mesh_list = ['infantry']


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


# Verify that all necessary software is present and derive their paths:
blender_exe = find_blender()
xml_exporter = find_xml_exporter()
gimp_exe = find_gimp()


# Build meshes:
for name in mesh_list:

    blend_path = os.path.join('art', 'mesh', '%s.blend' % name)
    xml_path = os.path.join('src', 'main', 'resources', 'mesh', '%s.xml' % name)

    r = subprocess.call([
        blender_exe, blend_path, '--background', '--python', xml_exporter,
        '--', name, xml_path
    ])
    if r != 0:
        sys.exit(r)

    if not os.path.isfile(xml_path):
        raise RuntimeError("%s was not generated" % xml_path)


# Build images:
error_path = tempfile.mktemp()
script_str = """
import gimpfu
import os
import traceback

def convert(xcf_path, png_path):
    img = pdb.gimp_file_load(xcf_path, xcf_path)
    layer = pdb.gimp_image_merge_visible_layers(img, gimpfu.CLIP_TO_IMAGE)
    pdb.gimp_file_save(img, layer, png_path, png_path)
    pdb.gimp_image_delete(img)
    if not os.path.isfile(png_path):
        raise RuntimeError("%%s was not generated" %% png_path)


try:
    for name in %s:
        xcf_path = os.path.join('art', 'image', '%%s.xcf' %% name)
        png_path = os.path.join('src', 'main', 'resources', 'image', '%%s.png' %% name)

        convert(xcf_path, png_path)
    pdb.gimp_quit(0)
except:
    traceback.print_exc(file=open('%s', 'w'))
    pdb.gimp_quit(1)
""" % (str(image_list), error_path)

subprocess.call([
    gimp_exe, '--verbose', '-n', '--batch-interpreter=python-fu-eval', '-b',
    script_str
])
# Since gimpfu always exits with 0, we must set an error flag on a file:
if os.path.isfile(error_path):
    os.remove(error_path)
    sys.exit(1)


# Check that everything was generated:
print("\nAll resources have been built successfully.")
