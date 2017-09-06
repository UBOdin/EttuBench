# set working directory
#setwd("~/Downloads/EttuBench")

# load supporting libraries
library(ggplot2)

comparison <- read.csv(file = "./data/result.csv", header = TRUE)
comparison$dataset <- factor(comparison$dataset, 
                             levels = c("IIT Bombay Dataset", 
                                        "UB Exam Dataset", 
                                        "PocketData-Google+"))

ggplot(data = comparison, aes(x = metric, y = silhouette, fill = regularization)) + 
  geom_bar(position="dodge", stat="identity") + facet_grid(~ dataset) + 
  ylab("Average Silhouette Coefficient") + xlab("Metric") + 
  theme_bw(base_size = 18) + theme(legend.position = "top") + scale_fill_grey() +
  ggsave(filename = "./figure/compare_silhouette.pdf", height = 5)

ggplot(data = comparison, aes(x = metric, y = beta_cv, fill = regularization)) + 
  geom_bar(position="dodge", stat="identity") + facet_grid(~ dataset) + 
  ylab("BetaCV") + xlab("Metric") + 
  theme_bw(base_size = 18) + theme(legend.position = "top") + scale_fill_grey() +
  ggsave(filename = "./figure/compare_betacv.pdf", height = 5)

ggplot(data = comparison, aes(x = metric, y = dunn, fill = regularization)) + 
  geom_bar(position="dodge", stat="identity") + facet_grid(~ dataset) + 
  ylab("Dunn Index") + xlab("Metric") + 
  theme_bw(base_size = 18) + theme(legend.position = "top") + scale_fill_grey() +
  ggsave(filename = "./figure/compare_dunn.pdf", height = 5)

