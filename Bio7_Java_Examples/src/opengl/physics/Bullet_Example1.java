/*Install Java3D (vecmath) and then remove the block comment below to run this example!!!!*/

/*package opengl.physics;

import java.util.ArrayList;
import java.util.List;

import com.bulletphysics.collision.broadphase.AxisSweep3;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.*;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.dispatch.CollisionObject;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.eco.bio7.spatial.SpatialCamera;
import com.jogamp.opengl.util.gl2.GLUT;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import javax.vecmath.Vector3f;
import static com.jogamp.opengl.GL2.*;


 This example demonstrates the use of the JBullet physics library
 (http://jbullet.advel.cz/) in Bio7.

 For an manual and overview see
 http://www.bulletphysics.com/Bullet/wordpress/

 Invoke the setup and the execution method to see the result!
 Switch to the custom camera to change to a special perspective
 (The z-achis has to point to the user for this perspective).
 
public class Bullet_Example1 extends com.eco.bio7.compile.Model {
	private static final int ARRAY_SIZE_X = 5;
	private static final int ARRAY_SIZE_Y = 5;
	private static final int ARRAY_SIZE_Z = 5;

	// maximum number of objects (and allow user to shoot additional boxes)
	private static final int MAX_PROXIES = (ARRAY_SIZE_X * ARRAY_SIZE_Y
			* ARRAY_SIZE_Z + 1024);

	private static final int START_POS_X = -50;
	private static final int START_POS_Y = -50;
	private static final int START_POS_Z = 1000;

	// keep the collision shapes, for deletion/cleanup
	private List collisionShapes = new ArrayList();
	private BroadphaseInterface overlappingPairCache;
	private CollisionDispatcher dispatcher;
	private ConstraintSolver solver;
	private DefaultCollisionConfiguration collisionConfiguration;
	public DiscreteDynamicsWorld dynamicsWorld;
	public CollisionShape colShape;
	public Vector3f worldAabbMin;
	public Vector3f worldAabbMax;
	private final Transform m = new Transform();
	float[] no_mat = { 0.0f, 0.0f, 0.0f, 1.0f };
	float[] fBrightLight = { 1.0f, 1.0f, 1.0f, 1.0f };
	Vector3f vec;

	double[] c1 = new double[128];
	double[] c2 = new double[128];
	double[] c3 = new double[128];

	public void setup(GL2 gl, GLU glu, GLUT glut) {

		gl.glLightModeli(GL_LIGHT_MODEL_COLOR_CONTROL,
				GL_SEPARATE_SPECULAR_COLOR);
		initPhysics();
		int numObjects = dynamicsWorld.getNumCollisionObjects();
		// System.out.println(numObjects);
		for (int i = 0; i < numObjects; i++) {
			c1[i] = Math.random();
			c2[i] = Math.random();
			c3[i] = Math.random();
		}
	}

	public void run(GL2 gl, GLU glu, GLUT glut) {

		// dynamicsWorld.stepSimulation(1,1,0);
		if (dynamicsWorld != null) {
			dynamicsWorld.stepSimulation(1f / 3f, 0);
			int numObjects = dynamicsWorld.getNumCollisionObjects();
			// System.out.println(numObjects);
			for (int i = 0; i < numObjects; i++) {

				CollisionObject colObj = (CollisionObject) dynamicsWorld
						.getCollisionObjectArray().get(i);
				RigidBody body = RigidBody.upcast(colObj);
				// body.translate(new Vector3f(1,1,1));
				body.setRestitution(0.0f);
				body.setFriction(1.0f);
				body.setDamping(0.0f, 1.0f);

				if (body != null && body.getMotionState() != null) {
					DefaultMotionState myMotionState = (DefaultMotionState) body
							.getMotionState();
					m.set(myMotionState.graphicsWorldTrans);
				}
				vec = body.getCenterOfMassPosition(new Vector3f());
				gl.glColor4d(c1[i], c2[i], c3[i], 1.0f);
				if (i > 1) {
					 Here we draw the shapes with the GL context!!!!! 
					gl.glPushMatrix();

					gl.glTranslated(vec.x, vec.y, vec.z);

					gl.glEnable(GL_COLOR_MATERIAL);

					gl.glEnable(GL_COLOR_MATERIAL);
					gl.glColorMaterial(GL.GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
					gl.glMaterialfv(GL.GL_FRONT, GL_SPECULAR, fBrightLight, 0);

					gl.glMateriali(GL.GL_FRONT, GL_SHININESS, 110);

					glut.glutSolidSphere(10, 10, 5);

					gl.glPopMatrix();
				} else {
					 We draw the big central Sphere! 
					gl.glPushMatrix();

					gl.glTranslated(vec.x, vec.y, vec.z);
					gl.glColor4d(c1[2], c2[2], c3[2], 1.0f);
					gl.glEnable(GL_COLOR_MATERIAL);

					gl.glEnable(GL_COLOR_MATERIAL);
					gl.glColorMaterial(GL.GL_FRONT, GL_AMBIENT_AND_DIFFUSE);
					gl.glMaterialfv(GL.GL_FRONT, GL_SPECULAR, fBrightLight, 0);

					gl.glMateriali(GL.GL_FRONT, GL_SHININESS, 110);

					glut.glutSolidSphere(100, 10, 20);

					gl.glPopMatrix();

				}
			}
			 We fly with the last sphere if the custom camera is enabled! 
			SpatialCamera.setCustomCamera(vec.x, vec.y, vec.z + 50, vec.x,
					vec.y, vec.z);

		}

	}

	public void initPhysics() {

		// collision configuration contains default setup for memory, collision
		// setup
		collisionConfiguration = new DefaultCollisionConfiguration();

		// use the default collision dispatcher. For parallel processing you can
		// use a diffent dispatcher (see Extras/BulletMultiThreaded)
		dispatcher = new CollisionDispatcher(collisionConfiguration);

		// the maximum size of the collision world. Make sure objects stay
		// within these boundaries
		// TODO: AxisSweep3
		// Don't make the world AABB size too large, it will harm simulation
		// quality and performance
		Vector3f worldAabbMin = new Vector3f(-10000, -10000, -10000);
		Vector3f worldAabbMax = new Vector3f(10000, 10000, 10000);
		// overlappingPairCache = new
		// AxisSweep3(worldAabbMin,worldAabbMax,MAX_PROXIES);
		overlappingPairCache = new AxisSweep3(worldAabbMin, worldAabbMax,
				MAX_PROXIES);
		// overlappingPairCache = new _BrutalforceBroadphase();

		// the default constraint solver. For parallel processing you can use a
		// different solver (see Extras/BulletMultiThreaded)
		SequentialImpulseConstraintSolver sol = new SequentialImpulseConstraintSolver();
		solver = sol;

		// TODO: needed for SimpleDynamicsWorld
		// sol.setSolverMode(sol.getSolverMode() &
		// ~SolverMode.SOLVER_CACHE_FRIENDLY.getMask());

		dynamicsWorld = new DiscreteDynamicsWorld(dispatcher,
				overlappingPairCache, solver, collisionConfiguration);
		// dynamicsWorld = new SimpleDynamicsWorld(dispatcher,
		// overlappingPairCache, solver, collisionConfiguration);

		dynamicsWorld.setGravity(new Vector3f(0f, 0f, -9.8f));

		// create a few basic rigid bodies
		CollisionShape groundShape = new StaticPlaneShape(
				new Vector3f(0, 0, 1), 0);
		CollisionShape groundShape2 = new SphereShape(100f);

		collisionShapes.add(groundShape);
		collisionShapes.add(groundShape2);

		Transform groundTransform = new Transform();
		groundTransform.setIdentity();
		groundTransform.origin.set(0, 0, 0);

		// We can also use DemoApplication::localCreateRigidBody, but for
		// clarity it is provided here:
		{
			float mass = 0.0f;

			// rigidbody is dynamic if and only if mass is non zero, otherwise
			// static
			boolean isDynamic = (mass != 0f);

			Vector3f localInertia = new Vector3f(0, 0, 0);
			if (isDynamic) {
				groundShape.calculateLocalInertia(mass, localInertia);
			}

			// using motionstate is recommended, it provides interpolation
			// capabilities, and only synchronizes 'active' objects
			DefaultMotionState myMotionState = new DefaultMotionState(
					groundTransform);
			RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
					mass, myMotionState, groundShape, localInertia);
			RigidBody body = new RigidBody(rbInfo);

			RigidBodyConstructionInfo rbInfo2 = new RigidBodyConstructionInfo(
					mass, myMotionState, groundShape2, localInertia);
			RigidBody body2 = new RigidBody(rbInfo2);
			// add the body to the dynamics world
			dynamicsWorld.addRigidBody(body);
			dynamicsWorld.addRigidBody(body2);
		}

		{
			// create a few dynamic rigidbodies
			// Re-using the same collision is better for memory usage and
			// performance

			colShape = new SphereShape(10.0f);
			// CollisionShape colShape = new SphereShape(1f);
			collisionShapes.add(colShape);

			// Create Dynamic Objects
			Transform startTransform = new Transform();
			startTransform.setIdentity();

			float mass = 1.0f;

			// rigidbody is dynamic if and only if mass is non zero, otherwise
			// static
			boolean isDynamic = (mass != 0f);

			Vector3f localInertia = new Vector3f(0, 0, 0);
			if (isDynamic) {
				colShape.calculateLocalInertia(mass, localInertia);
			}

			float start_x = START_POS_X - ARRAY_SIZE_X / 2;
			float start_y = START_POS_Y;
			float start_z = START_POS_Z - ARRAY_SIZE_Z / 2;

			for (int k = 0; k < ARRAY_SIZE_Y; k++) {
				for (int i = 0; i < ARRAY_SIZE_X; i++) {
					for (int j = 0; j < ARRAY_SIZE_Z; j++) {
						startTransform.origin.set(20f * i + start_x, 20f * k
								+ start_y, 20f * j + start_z);

						// using motionstate is recommended, it provides
						// interpolation capabilities, and only synchronizes
						// 'active' objects
						DefaultMotionState myMotionState = new DefaultMotionState(
								startTransform);
						RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
								mass, myMotionState, colShape, localInertia);
						RigidBody body = new RigidBody(rbInfo);

						dynamicsWorld.addRigidBody(body);
					}
				}
			}
		}
	}
}
*/