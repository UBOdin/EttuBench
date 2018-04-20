# set working directory
rm(list = ls())
# load two files evaluation.R and utils.R
source(file = "./evaluation.R")
source(file = "./utils.R")

# load supporting libraries
library(cluster)
library(factoextra)
library(RColorBrewer)
library(ggplot2)
library(dplyr)

for (threshold in c(0.2, 0.5, 0.8)) {
  dataset <- read.csv(file = paste0("./data/ub_query_", threshold, ".csv"), 
                      header = TRUE, sep = "\t")
  for (method in c("makiyama", "aligon", "aouiche")) {
    distMat <- readDistMat(paste0("./data/ub_", method, "_", threshold,".csv"))
    silhouettePlot(distMat, dataset$label, 
                   paste0("./figure/sil_ub_", method, "_", threshold, ".eps"))
  }
}
