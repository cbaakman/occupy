#version 150

in vec3 position;
in vec2 texCoord;
in vec3 normal;

out VertexData {
  vec2 texCoord;
  vec3 normal;
} vertexOut;

uniform mat4 modelviewMatrix;
uniform mat4 projectionMatrix;

void main() {
  gl_Position = projectionMatrix * modelviewMatrix * vec4(position, 1.0);
  mat4 normalMatrix = transpose(inverse(modelviewMatrix));
  vertexOut.texCoord = texCoord;
  vertexOut.normal = (normalMatrix * vec4(normal, 1.0)).xyz;
}