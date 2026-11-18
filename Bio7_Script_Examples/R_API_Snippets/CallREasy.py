# An easy example get and set values in R with the Bio7 API!

from com.eco.bio7.rbridge import REval


for i in range (1,5):		
	REval.evalR("plot(runif(1000)); ", None);
	REval.toR("count",str(i));
	REval.evalR("print(paste('Plot finished Nr.',count,sep=''))", None);
	#Get Values from R!
	rValue=REval.fromR("count");
	print("The last count was Nr."+rValue.firstString());

