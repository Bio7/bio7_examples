package opengl.particle;

import com.jogamp.opengl.*;
import com.jogamp.opengl.glu.GLU;
import com.eco.bio7.batch.FileRoot;
import com.eco.bio7.spatial.SpatialLoader;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.util.texture.Texture;
import java.util.*;
import static com.jogamp.opengl.GL2.*;

public class ParticleDemo extends com.eco.bio7.compile.Model {

	/*
	 * A particle example with Bio7 adapted from the Jogl examples!
	 * 
	 * Original author:
	 * 
	 * Copyright (c) 2006 Ben Chappell (bwchappell@gmail.com) All Rights Reserved.
	 * 
	 * Redistribution and use in source and binary forms, with or without
	 * modification, are permitted provided that the following conditions are met:
	 * 
	 * - Redistribution of source code must retain the above copyright notice, this
	 * list of conditions and the following disclaimer.
	 * 
	 * - Redistribution in binary form must reproduce the above copyright notice,
	 * this list of conditions and the following disclaimer in the documentation
	 * and/or other materials provided with the distribution.
	 * 
	 * The names of Ben Chappell, Sun Microsystems, Inc. or the names of
	 * contributors may not be used to endorse or promote products derived from this
	 * software without specific prior written permission.
	 * 
	 * This software is provided "AS IS," without a warranty of any kind. ALL
	 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING ANY
	 * IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR
	 * NON-INFRINGEMENT, ARE HEREBY EXCLUDED. BEN CHAPPELL, SUN MICROSYSTEMS, INC.
	 * ("SUN"), AND SUN'S LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY
	 * LICENSEE AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
	 * DERIVATIVES. IN NO EVENT WILL BEN CHAPPELL, SUN, OR SUN'S LICENSORS BE LIABLE
	 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
	 * CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS
	 * OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE
	 * THIS SOFTWARE, EVEN IF BEN CHAPPELL OR SUN HAS BEEN ADVISED OF THE
	 * POSSIBILITY OF SUCH DAMAGES.
	 * 
	 * You acknowledge that this software is not designed or intended for use in the
	 * design, construction, operation or maintenance of any nuclear facility.
	 */

	private Engine engine;
	private Integer numParticles;
	private RGBA background;
	boolean isSetupTriggered = false;

	public void setup(GL2 gl, GLU glu, GLUT glut) {
		numParticles = new Integer(10000);
		String f;

		f = FileRoot.getCurrentCompileDir() + "/opengl/particle/particle.jpg";
		engine = new Engine(numParticles.intValue(), f);
		engine.init(gl);
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {

		/* We automatically trigger the setup method! */
		if (isSetupTriggered == false) {
			isSetupTriggered = true;
			setup(gl, glu, glut);
		}
		gl.glDisable(GL_LIGHTING);

		// Disable depth testing.
		gl.glDisable(GL.GL_DEPTH_TEST);

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE);

		engine.draw(gl);

		gl.glBlendFunc(GL.GL_SRC_ALPHA, GL.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL.GL_DEPTH_TEST);
	}

	public class Engine {

		private Texture texture;
		private List/* <Particle> */ particles;
		private String path;
		public RGBA tendToColor;

		public Engine(int numParticles, String path) {
			this.path = path;

			tendToColor = new RGBA(1.0f, 1.0f, 1.0f, 1.0f);

			particles = new ArrayList/* <Particle> */(numParticles);
			for (int i = 0; i < numParticles; i++)
				particles.add(new Particle());
		}

		public void addParticle() {
			particles.add(new Particle());
		}

		public void removeParticle() {
			if (particles.size() - 1 >= 0)
				particles.remove(particles.size() - 1);
		}

		public void draw(GL2 gl) {
			/*
			 * Removed the following lines for Bio7. We don't need to clear the Buffer!
			 */
			// gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
			// gl.glLoadIdentity();
			gl.glPushMatrix();
			gl.glScaled(10, 10, 10);
			for (int i = 0; i < particles.size(); i++) {
				((Particle) particles.get(i)).draw(gl, texture, tendToColor);

			}
			gl.glPopMatrix();
		}

		public void init(GL2 gl) {
			try {
				/* We use the texture loader of Bio7! */
				String f = FileRoot.getCurrentCompileDir() + "/opengl/particle/particle.jpg";
				texture = SpatialLoader.createTexture(f, gl);
				texture.enable(gl);
			} catch (GLException e) {
				e.printStackTrace();
			}
		}

		public void reset() {
			int numParticles = particles.size();
			particles = new ArrayList/* <Particle> */(numParticles);
			for (int i = 0; i < numParticles; i++)
				particles.add(new Particle());
		}

	}

	public class Particle {
		private XYZ currentPos;
		private RGBA rgba;

		public Particle() {
			currentPos = new XYZ((float) Math.random(), (float) Math.random(), 0.0f);

			rgba = new RGBA((float) Math.random(), (float) Math.random(), (float) Math.random(), (float) Math.random());
		}

		/* Here the textured quads are drawn! */
		public void draw(GL2 gl, Texture texture, RGBA tendToColor) {
			adjust(tendToColor);
			texture.bind(gl);
			texture.enable(gl);
			gl.glColor4f(rgba.r, rgba.g, rgba.b, rgba.a);

			gl.glBegin(GL_QUADS);
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex3f(currentPos.x, currentPos.y - 2, currentPos.z);
			gl.glTexCoord2f(1.0f, 0.0f);
			gl.glVertex3f(currentPos.x + 2, currentPos.y - 2, currentPos.z);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex3f(currentPos.x + 2, currentPos.y, currentPos.z);
			gl.glTexCoord2f(0.0f, 1.0f);
			gl.glVertex3f(currentPos.x, currentPos.y, currentPos.z);
			gl.glEnd();
			texture.disable(gl);
		}

		private void tendToColor(RGBA tendToColor) {
			float red = 0.0f;
			float blue = 0.0f;
			float green = 0.0f;
			float sign = 1.0f;

			if (Math.random() >= 0.5)
				sign = -1.0f;

			// RED
			if (tendToColor.r <= 1 - tendToColor.r)
				red = tendToColor.r;
			else
				red = 1 - tendToColor.r;

			red = (float) (Math.random() * red * sign + tendToColor.r);

			// GREEN
			if (tendToColor.g <= 1 - tendToColor.g)
				green = tendToColor.g;
			else
				green = 1 - tendToColor.g;

			green = (float) (Math.random() * green * sign + tendToColor.g);

			// BLUE
			if (tendToColor.b <= 1 - tendToColor.b)
				blue = tendToColor.b;
			else
				blue = 1 - tendToColor.b;

			blue = (float) (Math.random() * blue * sign + tendToColor.b);

			rgba = new RGBA(red, green, blue, (float) Math.random());
		}

		private void tendToPos() {
			XYZ xyz = new XYZ((float) Math.random() - 0.5f, (float) Math.random() - 0.5f, (float) Math.random() - 0.5f);
			currentPos.add(xyz);
		}

		private void adjust(RGBA tendToColor) {
			tendToPos();

			rgba.a -= Math.random() / 100;
			if (rgba.a <= 0)
				tendToColor(tendToColor);
		}

	}

	public class RGBA {

		public float r;
		public float g;
		public float b;
		public float a;

		public RGBA() {
			this.r = this.g = this.g = this.a = 0.0f;
		}

		public RGBA(float r, float g, float b, float a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
		}
	}

	public class XYZ {

		public float x;
		public float y;
		public float z;

		public XYZ() {
			this.x = this.y = this.z = 0.0f;
		}

		public XYZ(float x, float y, float z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		public XYZ add(XYZ xyz) {
			x += xyz.x;
			y += xyz.y;
			z += xyz.z;

			return this;
		}
	}
}