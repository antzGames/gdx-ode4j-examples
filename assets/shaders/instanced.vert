in vec3 a_position;
in mat4 i_worldTrans;
in vec4 i_color;
out vec4 v_color;

uniform mat4 u_projViewTrans;

void main () {
    v_color = i_color;
    gl_Position = u_projViewTrans * i_worldTrans * vec4(a_position, 1.0);
}
