package opengl.modern;

import java.nio.FloatBuffer;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

/*
 This example renders with an own OpenGL 4 core context (OpenGL 4.1 core on
 macOS) in the 3D view: shaders, vertex buffers, a vertex array and instanced
 drawing with an own camera. The Bio7 scene (grid, iso surface, etc.) is not
 drawn while the model is compiled (getGLProfile returns GLProfile.GL4).

 To show the Bio7 scene again compile a model without an own profile, e.g.,
 the example InstancedCubesGL4.

 Compile the class (the context is created) and start the animation in the
 3D options!
 */

public class ModernContextGL4 extends com.eco.bio7.compile.Model {

	private static final int COUNT = 5000;

	private static final String VERTEX_SHADER = String.join("\n",
			"#version 410 core",
			"layout(location = 0) in vec3 position;",
			"layout(location = 1) in vec3 normal;",
			"layout(location = 2) in vec4 instance;", // x, y, z and a phase
			"uniform mat4 projection;",
			"uniform mat4 view;",
			"uniform float time;",
			"out vec3 vNormal;",
			"out vec3 vColor;",
			"void main() {",
			"    float wave = sin(time * 1.5 + instance.w);",
			"    vec3 p = position * (0.04 + 0.02 * wave) + instance.xyz * (1.0 + 0.05 * wave);",
			"    gl_Position = projection * view * vec4(p, 1.0);",
			"    vNormal = mat3(view) * normal;",
			"    vColor = 0.5 + 0.5 * cos(vec3(0.0, 2.0, 4.0) + instance.w + time * 0.5);",
			"}");

	private static final String FRAGMENT_SHADER = String.join("\n",
			"#version 410 core",
			"in vec3 vNormal;",
			"in vec3 vColor;",
			"out vec4 color;",
			"void main() {",
			"    float light = 0.25 + 0.75 * max(dot(normalize(vNormal), normalize(vec3(0.3, 0.6, 1.0))), 0.0);",
			"    color = vec4(vColor * light, 1.0);",
			"}");

	private int program;
	private int vao;
	private int projectionLocation, viewLocation, timeLocation;
	private final long start = System.nanoTime();

	/* An own OpenGL 4 core context without the Bio7 scene! */
	public String getGLProfile() {
		return GLProfile.GL4;
	}

	public void setup(GLAutoDrawable drawable) {
		GL4 gl = drawable.getGL().getGL4();
		System.out.println("OpenGL " + gl.glGetString(GL.GL_VERSION));
		if (program != 0) {
			gl.glDeleteProgram(program);
			gl.glDeleteVertexArrays(1, new int[] { vao }, 0);
		}
		program = createProgram(gl, VERTEX_SHADER, FRAGMENT_SHADER);
		projectionLocation = gl.glGetUniformLocation(program, "projection");
		viewLocation = gl.glGetUniformLocation(program, "view");
		timeLocation = gl.glGetUniformLocation(program, "time");

		int[] ids = new int[1];
		gl.glGenVertexArrays(1, ids, 0);
		vao = ids[0];
		gl.glBindVertexArray(vao);
		/* The cube (positions and normals) */
		int cubeBuffer = createBuffer(gl, cube());
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, cubeBuffer);
		gl.glEnableVertexAttribArray(0);
		gl.glVertexAttribPointer(0, 3, GL.GL_FLOAT, false, 6 * Float.BYTES, 0);
		gl.glEnableVertexAttribArray(1);
		gl.glVertexAttribPointer(1, 3, GL.GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
		/* One position (in a sphere) and phase per cube (instanced) */
		float[] instances = new float[COUNT * 4];
		java.util.Random random = new java.util.Random(7);
		for (int i = 0; i < COUNT; i++) {
			double theta = random.nextDouble() * 2 * Math.PI, phi = Math.acos(2 * random.nextDouble() - 1);
			double r = 1.0 + 0.15 * random.nextGaussian();
			instances[i * 4] = (float) (r * Math.sin(phi) * Math.cos(theta));
			instances[i * 4 + 1] = (float) (r * Math.cos(phi));
			instances[i * 4 + 2] = (float) (r * Math.sin(phi) * Math.sin(theta));
			instances[i * 4 + 3] = random.nextFloat() * 6.283f;
		}
		int instanceBuffer = createBuffer(gl, instances);
		gl.glBindBuffer(GL.GL_ARRAY_BUFFER, instanceBuffer);
		gl.glEnableVertexAttribArray(2);
		gl.glVertexAttribPointer(2, 4, GL.GL_FLOAT, false, 0, 0);
		gl.glVertexAttribDivisor(2, 1);
		gl.glBindVertexArray(0);
	}

	public void run(GLAutoDrawable drawable) {
		if (program == 0) {
			return;
		}
		GL4 gl = drawable.getGL().getGL4();
		float time = (System.nanoTime() - start) / 1e9f;
		gl.glClearColor(0.08f, 0.09f, 0.12f, 1);
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
		gl.glEnable(GL.GL_DEPTH_TEST);
		float aspect = drawable.getSurfaceWidth() / (float) Math.max(1, drawable.getSurfaceHeight());
		gl.glUseProgram(program);
		gl.glUniformMatrix4fv(projectionLocation, 1, false, perspective(45, aspect, 0.1f, 100), 0);
		gl.glUniformMatrix4fv(viewLocation, 1, false, view(time * 0.3f, 0.4f, 3.5f), 0);
		gl.glUniform1f(timeLocation, time);
		gl.glBindVertexArray(vao);
		gl.glDrawArraysInstanced(GL.GL_TRIANGLES, 0, 36, COUNT);
		gl.glBindVertexArray(0);
		gl.glUseProgram(0);
	}

	/* A perspective projection (column major)! */
	private static float[] perspective(float fovY, float aspect, float near, float far) {
		float f = (float) (1 / Math.tan(Math.toRadians(fovY) / 2));
		return new float[] { f / aspect, 0, 0, 0, 0, f, 0, 0, 0, 0, (far + near) / (near - far), -1, 0, 0,
				2 * far * near / (near - far), 0 };
	}

	/* A camera rotated around the y and x axis at a distance (column major)! */
	private static float[] view(float angleY, float angleX, float distance) {
		float cy = (float) Math.cos(angleY), sy = (float) Math.sin(angleY);
		float cx = (float) Math.cos(angleX), sx = (float) Math.sin(angleX);
		return new float[] { cy, sx * sy, -cx * sy, 0, 0, cx, sx, 0, sy, -sx * cy, cx * cy, 0, 0, 0, -distance, 1 };
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
