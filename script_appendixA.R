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

df <- data.frame(dataset = "", projection_weight = 0.0,
                 groupby_weight = 0.0, selection_weight = 0.0, avg_sil = 0.0, 
                 betacv = 0.0, dunn_index = 0.0)
df$dataset <- as.character(df$dataset)

for (dat in c("bombay", "ub", "googleplus")) {
  if (dat == "bombay") {
    dataset_name <- "IIT Bombay Dataset"  
  } else if (dat == "ub") {
    dataset_name <- "UB Exam Dataset"
  } else {
    dataset_name <- "PocketData-Google+"
  }
  # read data file
  dataset <- read.csv(file = paste0("./data/", dat,"_queries.csv"), 
                      header = TRUE, sep = "\t")
  
  prefix <- paste0(dat, "_aligon")
  
  
  for (weight_case in 1:7) {
    if (weight_case == 1) {
      projection_weight <- 0.33
      groupby_weight <- 0.33
      selection_weight <- 0.33
    } else if (weight_case == 2) {
      projection_weight <- 0.5
      groupby_weight <- 0.25
      selection_weight <- 0.25
    } else if (weight_case == 3) {
      projection_weight <- 0.25
      groupby_weight <- 0.5
      selection_weight <- 0.25
    } else if (weight_case == 4) {
      projection_weight <- 0.25
      groupby_weight <- 0.25
      selection_weight <- 0.5
    } else if (weight_case == 5) {
      projection_weight <- 0.4
      groupby_weight <- 0.4
      selection_weight <- 0.2
    } else if (weight_case == 6) {
      projection_weight <- 0.4
      groupby_weight <- 0.2
      selection_weight <- 0.4
    } else {
      projection_weight <- 0.2
      groupby_weight <- 0.4
      selection_weight <- 0.4
    } 
    final_filename <- paste0("./data/", prefix, "_", 
                             format(projection_weight, nsmall = 2, format = "f"), 
                             "_",
                             format(groupby_weight, nsmall = 2, format = "f"), 
                             "_", 
                             format(selection_weight, nsmall = 2, format = "f"), 
                             ".csv")
    distMat <- readDistMat(final_filename)
    avg_sil <- avgSilhoette(distMat, dataset$label)
    betacv <- BetaCV(distMat, dataset$label)
    dunn_index <- DunnIndex(distMat, dataset$label)
    print(paste0(final_filename, " ", avg_sil))
    df <- rbind(df, c(dataset_name, projection_weight,
                      groupby_weight, selection_weight, 
                      avg_sil, betacv, dunn_index))
  }
}

df <- df[-1, ]
df$projection_weight <- as.numeric(df$projection_weight)
df$groupby_weight <- as.numeric(df$groupby_weight)
df$selection_weight <- as.numeric(df$selection_weight)
df$avg_sil <- as.numeric(df$avg_sil)
df$betacv <- as.numeric(df$betacv)
df$dunn_index <- as.numeric(df$dunn_index)

df$dataset <- factor(df$dataset, levels = c("IIT Bombay Dataset", "UB Exam Dataset", "PocketData-Google+"))

for (dat in c("IIT Bombay Dataset", "UB Exam Dataset", "PocketData-Google+")) {
  if (dat == "IIT Bombay Dataset") {
    dat_abbreviation <- "bombay"
  } else if (dat == "UB Exam Dataset") {
    dat_abbreviation <- "ub"
  } else {
    dat_abbreviation <- "googleplus"
  }
    
  ggplot(data = filter(df, dataset == dat), 
         aes(x = projection_weight, y = groupby_weight, size = avg_sil)) +
    geom_point() + scale_size_area() +
    xlab("projection weight") + ylab("group-by weight") + theme_bw(base_size = 18) +
    ggsave(filename = paste0("./figure/aligon_weight_", dat_abbreviation, ".eps"))
}
