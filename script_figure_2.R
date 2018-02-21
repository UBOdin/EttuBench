rm(list = ls())
# set working directory
#setwd("~/Downloads/EttuBench")

# load two files evaluation.R and utils.R
source(file = "./evaluation.R")
source(file = "./utils.R")

# load supporting libraries
library(cluster)
library(factoextra)
library(RColorBrewer)

dataset <- read.csv(file = "./data/bombay_queries.csv", header = TRUE, sep = "\t")

distMat <- readDistMat("./data/bombay_aligon.csv")
silhouettePlot(distMat, dataset$label, "./figure/sil_bombay_Aligon.pdf")

distMat <- readDistMat("./data/bombay_aligon_regularization.csv")
silhouettePlot(distMat, dataset$label, "./figure/sil_bombay_Aligon_regularization.pdf")

dataset <- read.csv(file = "./data/ub_queries.csv", header = TRUE, sep = "\t")

distMat <- readDistMat("./data/ub_aligon.csv")
silhouettePlot(distMat, dataset$label, "./figure/sil_ub_Aligon.pdf")

distMat <- readDistMat("./data/ub_aligon_regularization.csv")
silhouettePlot(distMat, dataset$label, "./figure/sil_ub_Aligon_regularization.pdf")

dataset <- read.csv(file = "./data/googleplus_queries.csv", header = TRUE, sep = "\t")

distMat <- readDistMat("./data/googleplus_aligon.csv")
silhouettePlot(distMat, dataset$label, "./figure/sil_googleplus_Aligon.pdf")

distMat <- readDistMat("./data/googleplus_aligon_regularization.csv")
silhouettePlot(distMat, dataset$label, "./figure/sil_googleplus_Aligon_regularization.pdf")

