#version 150

uniform mat4 projectionMatrix;

in vec2 position;
in vec2 texCoord;

out vec2 texCoords;

void main() {
  texCoords = texCoord;
  gl_Position = projectionMatrix * vec4(position.x, position.y, 0.0, 1.0);
}