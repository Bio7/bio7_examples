#This Snippet calculates the interpoint distances from existent x,y values!
#Uses the library spatstat!

library(spatstat)
d<-nndist(x, y)
dmean<-mean(d)
dmax<-max(d)
dmin<-min(d)