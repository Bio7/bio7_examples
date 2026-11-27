package flow.models;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.random.RandomGenerator;

import com.eco.bio7.rbridge.RUtil;

/**
 * Step 2 of the flow Models/6_Adaptive_Management (Bio7_Flow_Examples), in the
 * loop: one year of a red deer population with 15 age classes (0 to 14+ years).
 * Each deer is simulated with random draws (demographic noise), the weather of
 * the winter varies (environmental noise).
 *
 * <ol>
 * <li>Autumn: hunting of the quota of R (deerQuota), half of it calves.</li>
 * <li>Winter: survival depends on the age, the density (N/K) and the severity of
 * the winter. From the year deerPar$wolvesFrom wolves have recolonised the
 * forest: they take a share of the deer (deerPar$wolfPredation, more calves) -
 * management doesn't know it and has to adapt to the fewer deer.</li>
 * <li>Spring: the females (half of the deer of 2 years and older) give birth,
 * fewer at a high density.</li>
 * <li>Summer: the survey counts a share of the deer (the detection varies from
 * year to year), the browsing damage of the young trees depends on the
 * density.</li>
 * </ol>
 *
 * The population goes back to R (deerAges), the year as the data frame
 * deerThisYear (the next steps estimate the population from the count and
 * decide on the quota).
 */
public class DeerYear {

	public static void main(String[] args) throws Exception {
		double[] ages = RUtil.fromRDoubles("deerAges");
		double k = RUtil.fromRDouble("deerPar$K");
		double area = RUtil.fromRDouble("deerPar$area");
		double pDetect = RUtil.fromRDouble("deerPar$pDetect");
		int seed = (int) RUtil.fromRDouble("deerPar$seed");
		int wolvesFrom = (int) RUtil.fromRDouble("deerPar$wolvesFrom");
		double wolfPredation = RUtil.fromRDouble("deerPar$wolfPredation");
		int quota = (int) Math.round(RUtil.fromRDouble("deerQuota"));
		int year = (int) RUtil.fromRDouble("deerYear") + 1;
		/* Reproducible: the same years with the same seed (SplittableRandom mixes the seeds of the years) */
		RandomGenerator random = new SplittableRandom(seed * 1_000_003L + year);

		int[] n = new int[ages.length];
		for (int a = 0; a < n.length; a++) {
			n[a] = (int) Math.round(ages[a]);
		}

		/* 1. Autumn: the hunt (half of the quota calves, the rest adults at random) */
		int calvesShot = Math.min(n[0], quota / 2);
		n[0] -= calvesShot;
		int adultsShot = 0;
		int adultQuota = Math.min(quota - calvesShot, total(n) - n[0]);
		while (adultsShot < adultQuota) {
			int pick = random.nextInt(total(n) - n[0]);
			for (int a = 1; a < n.length; a++) {
				if (pick < n[a]) {
					n[a]--;
					break;
				}
				pick -= n[a];
			}
			adultsShot++;
		}
		int harvest = calvesShot + adultsShot;

		/* 2. Winter: survival (logit scale) with age, density and the severity of the winter */
		double winter = random.nextGaussian();
		double density = total(n) / k;
		int[] survived = new int[n.length];
		for (int a = 0; a < n.length; a++) {
			double base = a == 0 ? 0.75 : a == 1 ? 0.9 : a < 10 ? 0.95 : 0.8;
			double effect = a == 0 ? 1.0 : 0.4;
			double s = logistic(logit(base) - effect * (1.5 * density + 0.7 * Math.max(winter, -1)));
			if (year >= wolvesFrom) {
				s *= 1 - wolfPredation * (a == 0 ? 1.5 : 0.8);
			}
			survived[a] = binomial(n[a], s, random);
		}

		/* 3. Spring: births (females of 2 years and older), the age classes move on */
		int females = 0;
		for (int a = 2; a < survived.length; a++) {
			females += binomial(survived[a], 0.5, random);
		}
		double fecundity = 0.9 / (1 + Math.pow(total(survived) / k, 2));
		int births = binomial(females, fecundity, random);
		int[] next = new int[n.length];
		next[0] = births;
		for (int a = 1; a < n.length; a++) {
			next[a] = survived[a - 1];
		}
		next[n.length - 1] += survived[n.length - 1];
		int size = total(next);

		/* 4. Summer: the survey count and the browsing damage */
		double p = Math.min(0.9, pDetect * Math.exp(0.15 * random.nextGaussian()));
		int count = binomial(size, p, random);
		double damage = 1 - Math.exp(-0.08 * size / area);

		double[] result = new double[next.length];
		for (int a = 0; a < next.length; a++) {
			result[a] = next[a];
		}
		RUtil.toR("deerAges", result);
		RUtil.toR("deerYear", year);
		Map<String, Object> row = new LinkedHashMap<>();
		row.put("year", new int[] { year });
		row.put("N", new int[] { size });
		row.put("calves", new int[] { births });
		row.put("harvest", new int[] { harvest });
		row.put("count", new int[] { count });
		row.put("winter", new double[] { winter });
		row.put("damage", new double[] { damage });
		row.put("wolves", new boolean[] { year >= wolvesFrom });
		RUtil.dataFrameToR("deerThisYear", row);
		System.out.printf(java.util.Locale.ROOT, "Year %d: %d deer (%d calves, %d shot, winter %.2f), survey count %d%n", year, size, births, harvest, winter, count);
	}

	private static int total(int[] n) {
		int sum = 0;
		for (int v : n) {
			sum += v;
		}
		return sum;
	}

	private static int binomial(int n, double p, RandomGenerator random) {
		int k = 0;
		for (int i = 0; i < n; i++) {
			if (random.nextDouble() < p) {
				k++;
			}
		}
		return k;
	}

	private static double logit(double p) {
		return Math.log(p / (1 - p));
	}

	private static double logistic(double x) {
		return 1 / (1 + Math.exp(-x));
	}
}
