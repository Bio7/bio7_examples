# An easy example get and set values in R with the Bio7 API!

from  org.rosuda.REngine import REXP;


for i in range (1,5):		
	RServeUtil.evalR("plot(runif(1000)); ", None);
	RServeUtil.toR("count",str(i));
	RServeUtil.evalR("print(paste('Plot finished Nr.',count,sep=''))", None);
	#Get Values from R!
	rexpr=RServeUtil.fromR("count");
	print("The last count was Nr."+rexpr.asString());

