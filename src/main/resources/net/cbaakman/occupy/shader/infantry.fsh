#version 150

uniform sampler2D meshTexture;

const vec3 lightDirection = vec3(0.5773, -0.5773, -0.5773);

in VertexData {
  vec2 texCoord;
  vec3 normal;
} vertexIn;

out vec4 fragColor;

void main() {
  vec3 n = normalize(vertexIn.normal);
  float f = (1.0 - dot(lightDirection, n)) * 0.5;
  fragColor = f * texture(meshTexture, vertexIn.texCoord);
}