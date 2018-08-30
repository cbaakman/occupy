#version 150

uniform sampler2D texture;

in vec2 texCoords;

out vec4 fragColor;

void main() {
  fragColor = texture(texture, texCoords);
}
