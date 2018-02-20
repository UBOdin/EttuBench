# set working directory
#setwd("~/Downloads/EttuBench")

# load supporting libraries
library(ggplot2)

comparison <- read.csv(file = "./data/modules.csv")
comparison$Regularization <- factor(comparison$Regularization, 
                                    levels = c("No Regularization",
                                               "Naming",
                                               "Expression Standardization",
                                               "FROM-nested Subquery",
                                               "UNION Pull-out"))
comparison$Dataset <- factor(comparison$Dataset, 
                             levels = c("IIT Bombay Dataset", "UB Exam Dataset",
                                        "PhoneLab-Google+"))

# individual module analysis
ggplot(data = comparison, aes(x = Metric, y = Silhouette, fill=Regularization)) + 
  geom_bar(position="dodge", stat="identity") + facet_grid(~ Dataset) + 
  ylab("Average Silhouette Coefficient") + xlab("Metric") + 
  theme_bw(base_size = 14) + theme(legend.position = "top", legend.title = element_blank()) + 
  #scale_fill_brewer(palette = "Dark2") +
  scale_fill_grey() +
  ggsave(file = "./figure/module.pdf")
