#A spatial analysis example!
library(spatial)
pines<-ppinit("pines.dat");
par(mfrow=c(1,2),pty="s")


plot(pines,pch=16)
plot(Kfn(pines,5),type="s",xlab="dne",ylab="L(t)")
boxplot(pines)
