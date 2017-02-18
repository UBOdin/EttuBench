#setwd("~/study/Insider Threats/SQLQuerySimilarityEvaluation")
library(SQLQuerySimilarityEvaluation)

dataset <- read.csv(file = "./data/bombay_queries.csv", header = TRUE, sep = "\t")
distMat <- readDistMat("./data/bombay_aligon.csv")

silhouettePlot(distMat, dataset$label, "./figure/sil_bombay_Aligon.pdf")
print(avgSilhoette(distMat, dataset$label))
print(BetaCV(distMat, dataset$label))
print(DunnIndex(distMat, dataset$label))
