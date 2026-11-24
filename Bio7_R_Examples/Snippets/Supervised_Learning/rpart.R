#RScript - please enter your code here !
#This Snippet demonstrates the use of supervised learning
#for image analysis.
#
#For this example the image 'FluorescentCells.jpg' was used (dim: 512*512, RGB) and the 'randomForest' package 
#(install if necessary).
#
#1. 	Please open an R,G,B image and split it into its R,G,B components
#		(Image perspective->ImageJ menu->Image->Colours->Split Colours).
# 
#2. 	Create different selections (e.g with the freehand selection of ImageJ).
#		Use the Roi Manager for the selections (Add [t]): ImageJ menu->Edit-Selection->Add to Manager

#3.		Rename them in the ROI-Manager of ImageJ, e.g., to matrix1,matrix2,matrix3..... 
#		Sort it in the ROI Manager (for the transfer!)

#4. 	Use the 'Pixel RM' action from the Image-Methods view
#		(Image perspective->ImageJ-Canvas->Window->Bio7-Toolbar)
#		and transfer the different selections (e.g with the freehand selection of ImageJ).
#      For the transfer select a increasing signature (Class) in the dialog (deselect 'Create Custom Class Signature...'). 
#		You can of course type a special signature for more than one sample of a special signature (Class)
#		Signatures from all opened images (or layers) will be transferred.

#5. 	Select the whole image as training data and add it as 'matrixAll'. 
#		Transfer it with the 'Pixel' action (without a signature)
#		
#6.		Execute the script below!

library(randomForest)# We use the lib package rpart
m<-rbind(matrix1,matrix2,matrix3,matrix4)#Combine matrices which represent a special signature.
dftrain<-as.data.frame(m)#Create a dataframe dftrain with the matrices 1-3
res<-randomForest(Class~layer1+layer2+layer3,data=dftrain)#Train with the selection matrices (layer=colors, layer1=Red....) combined in one matrix
final<-predict(res,as.data.frame(matrixAll))#Predict the whole image
imageMatrix<-matrix(final,512,512)#Create a image matrix
#You can now transfer the image back as a greyscale image (Image perspective->ImageJ-Canvas->Window->Bio7-Toolbar)
#to ImageJ to see the results!