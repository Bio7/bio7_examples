package opengl.modern;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL4bc;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;

/*
 This example draws 20000 animated cubes with OpenGL 4 (shaders, vertex
 buffers, a vertex array and instanced drawing) in the Bio7 scene. The cubes
 use the camera of the 3D view (gl_ModelViewProjectionMatrix of the
 compatibility profile) and are hidden correctly by the other objects.

 Needs the OpenGL 4 compatibility profile (Windows and Linux with a current
 driver). On macOS the scene has OpenGL 2.1 (see the example ModernContextGL4
 for OpenGL 4.1 on macOS).

 Compile the class, press "Setup" and start the animation in the 3D options!
 */

public class InstancedCubesGL4 extends com.eco.bio7.compile.Model {

	private static final int COUNT = 20000;

	private static final String VERTEX_SHADER = String.join("\n",
			"#version 400 compatibility",
			"layout(location = 0) in vec3 position;",
			"layout(location = 1) in vec3 normal;",
			"layout(location = 2) in vec4 instance;", // x, y, z and a phase
			"uniform float time;",
			"out vec3 vNormal;",
			"out vec3 vColor;",
			"void main() {",
			"    float wave = sin(time * 2.0 + instance.w);",
			"    vec3 p = position * (6.0 + 3.0 * wave) + instance.xyz;",
			"    p.y += 40.0 * wave;",
			"    gl_Position = gl_ModelViewProjectionMatrix * vec4(p, 1.0);",
			"    vNormal = normalize(gl_NormalMatrix * normal);",
			"    vColor = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + instance.w + time);",
			"}");

	private static final String FRAGMENT_SHADER = String.join("\n",
			"#version 400 compatibility",
			"in vec3 vNormal;",
			"in vec3 vColor;",
			"out vec4 color;",
			"void main() {",
			"    float light = 0.3 + 0.7 * abs(dot(normalize(vNormal), normalize(vec3(0.4, 0.8, 0.5))));",
			"    color = vec4(vColor * light, 1.0);",
			"}");

	private int program;
	private int vao;
	private int timeLocation;
	private boolean available;
	private final long start = System.nanoTime();

	public void setup(GLAutoDrawable drawable) {
		if (!drawable.getGL().isGL4bc()) {
			System.out.println("This example needs the OpenGL 4 compatibility profile. Available: "
					+ drawable.getGL().glGetString(GL.GL_VERSION));
			available = false;
			return;
		}
		GL4bc gl = drawable.getGL().getGL4bc();
		System.out.println("OpenGL " + gl.glGetString(GL.GL_VERSION));
		if (program != 0) {
			gl.glDeleteProgram(program);
			gl.glDeleteVertexArrays(1, new int[] { vao }, 0);
		}
		program = createProgram(gl, VERTEX_SHADER, FRAGMENT_SHADER);
		timeLocation = gl.glGetUniformLocation(program, "time");

		int[] ids = new int[1];
		gl.glGenVertexArrays(1, ids, 0);
		vao = ids[0];
		gl.glBindVertexArray(vao);
		/* The cube (positions and normals) */
		float[] cube = cube();
		int cubeBuffer = createBuffer(gl, cube);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, cubeBuffer);
		gl.glEnableVertexAttribArray(0);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 6 * Float.BYTES, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
		/* One position and phase per cube (instanced) on the grid of the 3D view */
		float[] instances = new float[COUNT * 4];
		java.util.Random random = new java.util.Random(7);
		for (int i = 0; i < COUNT; i++) {
			instances[i * 4] = random.nextFloat() * 1000;
			instances[i * 4 + 1] = 100 + random.nextFloat() * 200;
			instances[i * 4 + 2] = random.nextFloat() * 1000;
			instances[i * 4 + 3] = random.nextFloat() * 6.283f;
		}
		int instanceBuffer = createBuffer(gl, instances);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, instanceBuffer);
		gl.glEnableVertexAttribArray(2);
		gl.glVertexAttribPointer(2, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glVertexAttribDivisor(2, 1);
		gl.glBindVertexArray(0);
		available = true;
	}

	public void run(GLAutoDrawable drawable) {
		if (!available) {
			return;
		}
		GL4bc gl = drawable.getGL().getGL4bc();
		gl.glUseProgram(program);
		gl.glUniform1f(timeLocation, (System.nanoTime() - start) / 1e9f);
		gl.glBindVertexArray(vao);
		gl.glDrawArraysInstanced(GL.GL_TRIANGLES, 0, 36, COUNT);
		/* Bio7 unbinds the program, the vertex array and the buffers after the method! */
	}

	/* Compiles and links a shader program (throws an exception with the log on errors)! */
	static int createProgram(GL2ES2 gl, String vertexSource, String fragmentSource) {
		int vertex = compile(gl, GL2ES2.GL_VERTEX_SHADER, vertexSource);
		int fragment = compile(gl, GL2ES2.GL_FRAGMENT_SHADER, fragmentSource);
		int program = gl.glCreateProgram();
		gl.glAttachShader(program, vertex);
		gl.glAttachShader(program, fragment);
		gl.glLinkProgram(program);
		gl.glDeleteShader(vertex);
		gl.glDeleteShader(fragment);
		int[] status = new int[1];
		gl.glGetProgramiv(program, GL2ES2.GL_LINK_STATUS, status, 0);
		if (status[0] == GL.GL_FALSE) {
			throw new GLException("The shader program can't be linked:\n" + programLog(gl, program));
		}
		return program;
	}

	private static int compile(GL2ES2 gl, int type, String source) {
		int shader = gl.glCreateShader(type);
		gl.glShaderSource(shader, 1, new String[] { source }, null, 0);
		gl.glCompileShader(shader);
		int[] status = new int[1];
		gl.glGetShaderiv(shader, GL2ES2.GL_COMPILE_STATUS, status, 0);
		if (status[0] == GL.GL_FALSE) {
			int[] length = new int[1];
			gl.glGetShaderiv(shader, GL2ES2.GL_INFO_LOG_LENGTH, length, 0);
			byte[] log = new byte[Math.max(1, length[0])];
			gl.glGetShaderInfoLog(shader, log.length, null, 0, log, 0);
			throw new GLException("The shader can't be compiled:\n" + new String(log).trim());
		}
		return shader;
	}

	private static String programLog(GL2ES2 gl, int program) {
		int[] length = new int[1];
		gl.glGetProgramiv(program, GL2ES2.GL_INFO_LOG_LENGTH, length, 0);
		byte[] log = new byte[Math.max(1, length[0])];
		gl.glGetProgramInfoLog(program, log.length, null, 0, log, 0);
		return new String(log).trim();
	}

	/* Creates a vertex buffer with the values! */
	static int createBuffer(GL gl, float[] values) {
		int[] ids = new int[1];
		gl.glGenBuffers(1, ids, 0);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, ids[0]);
		FloatBuffer data = Buffers.newDirectFloatBuffer(values);
		gl.glBufferData(GL.GL_ARRAY_BUFFER, (long) values.length * Float.BYTES, data, GL.GL_STATIC_DRAW);
		return ids[0];
	}

	/* A cube (-1 to 1) of 36 vertices: x, y, z and the normal x, y, z! */
	static float[] cube() {
		float[][] normals = { { 1, 0, 0 }, { -1, 0, 0 }, { 0, 1, 0 }, { 0, -1, 0 }, { 0, 0, 1 }, { 0, 0, -1 } };
		float[] cube = new float[36 * 6];
		int i = 0;
		for (float[] n : normals) {
			/* Two axes of the face */
			float[] u = { n[1], n[2], n[0] };
			float[] v = { n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0] };
			float[][] corners = { { -1, -1 }, { 1, -1 }, { 1, 1 }, { -1, -1 }, { 1, 1 }, { -1, 1 } };
			for (float[] c : corners) {
				for (int k = 0; k < 3; k++) {
					cube[i + k] = n[k] + c[0] * u[k] + c[1] * v[k];
					cube[i + 3 + k] = n[k];
				}
				i += 6;
			}
		}
		return cube;
	}
}
