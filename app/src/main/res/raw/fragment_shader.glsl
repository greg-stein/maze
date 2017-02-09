precision mediump float;
varying vec3 v_Position;		// Interpolated position for this fragment.
varying vec4 v_Color;          	// This is the color from the vertex shader interpolated across the
                                // triangle per fragment.
void main() {
    gl_FragColor = v_Color;
}
