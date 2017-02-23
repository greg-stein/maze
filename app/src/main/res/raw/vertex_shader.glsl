// This matrix member variable provides a hook to manipulate
// the coordinates of the objects that use this vertex shader
uniform mat4 uMVPMatrix;
uniform mat4 u_MVPMatrix;
attribute vec4 a_Position; // Per-vertex position information we will pass in.
attribute vec4 a_Color;    // Per-vertex color information we will pass in.

varying vec3 v_Position;
varying vec4 v_Color; // This will be passed into the fragment shader.
void main() {
    v_Position = vec3(uMVPMatrix * a_Position);
    // Pass through the color.
    v_Color = a_Color;
    // the matrix must be included as a modifier of gl_Position
    // Note that the uMVPMatrix factor *must be first* in order
    // for the matrix multiplication product to be correct.
    gl_Position = u_MVPMatrix * a_Position;
}