#version 150

uniform sampler2D Sampler0;

uniform vec4 ColorModulator;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// The texture only acts as a stencil: its transparent texels are cut away, the others show the vertex colour (as core/gui).
void main() {
    vec4 color = vertexColor;
    if (texture(Sampler0, texCoord0).a == 0.0 || color.a == 0.0) {
        discard;
    }
    fragColor = color * ColorModulator;
}
