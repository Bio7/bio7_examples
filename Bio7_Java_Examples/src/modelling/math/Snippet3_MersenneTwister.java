package modelling.math;

import cern.jet.random.tfloat.FloatUniform;
import cern.jet.random.tfloat.engine.FloatMersenneTwister;

/*
 This Snippet produces uniformly distributed random numbers 
 with a mersenne twister random generator of the colt library !
 */
public class Snippet3_MersenneTwister extends com.eco.bio7.compile.Model {
	private static FloatUniform uni = new FloatUniform(0.0f, 100.0f,
			new FloatMersenneTwister(new java.util.Date()));
	//
	public float random;

	public void run() {
		/* We only want to get integers ! */
		random = uni.nextFloat();
		System.out.println(random);

	}
}