#setwd("~/study/Insider Threats/SQLQuerySimilarityEvaluation")
library(SQLQuerySimilarityEvaluation)

compare_table <- data.frame(metric = rep(c("Aligon", "Makiyama", "Aouiche", "TopicModel"), 2),
                            regularization = c(rep(TRUE, 4), rep(FALSE, 4)),
                            betaCV = rep(0, 8), avgSilhouette = rep(0, 8),
                            dunn = rep(0, 8))

dataset <- read.csv(file = "data/bombay_query.csv", header = TRUE, sep = "\t")

for (i in 1:nrow(compare_table)) {
  print(i)
  metric <- compare_table[i, "metric"]
  regularization <- compare_table[i, "regularization"]
  if (metric != "TopicModel") {
    filename <- paste0("data/bombay_", tolower(metric))
    if (regularization == TRUE) {
      filename <- paste0(filename, "_preprocessed")
    }
    filename <- paste0(filename, ".csv")
    distMat <- readDistMat(fileName = filename)

  } else { # topic model
    labelFileName <- "data/label_bombay"
    featureVecFile <- "data/bombay_wl"
    if (regularization == TRUE) {
      labelFileName <- paste0(labelFileName, "_preprocessed")
      featureVecFile <- paste0(featureVecFile, "_preprocessed")
    }
    labelFileName <- paste0(labelFileName, ".txt")
    featureVecFile <- paste0(featureVecFile, ".csv")
    distMat <- computeDistMatTopic(labelFile = labelFileName,
                                   featureVecFile = featureVecFile,
                                   datasetFile = "data/bombay_query.csv",
                                   numTopic = 50)
  }
  compare_table[i, "betaCV"] <- BetaCV(distMat, dataset$label)
  compare_table[i, "avgSilhouette"] <- avgSilhoette(distMat, dataset$label)
  compare_table[i, "dunn"] <- DunnIndex(distMat, dataset$label)
}

ggplot(data = compare_table, aes(x = metric, y = betaCV)) + facet_grid(. ~ regularization) +
  geom_bar(stat = "identity") + ggsave("data/betaCV.pdf")

ggplot(data = compare_table, aes(x = metric, y = avgSilhouette)) + facet_grid(. ~ regularization) +
  geom_bar(stat = "identity") + ggsave("data/avgSilhouette.pdf")

ggplot(data = compare_table, aes(x = metric, y = dunn)) + facet_grid(. ~ regularization) +
  geom_bar(stat = "identity") + ggsave("data/DunnIndex.pdf")

distMat <- computeDistMatTopic(labelFile = "data/label_bombay.txt",
                               featureVecFile = "data/bombay_wl.csv",
                               datasetFile = "data/bombay_query.csv",
                               numTopic = 50)
silhouettePlot(distMat, dataset$label, "data/sil.pdf")

VEM <- getTopicModelResult(labelFile = "data/label_bombay.txt",
                           featureVecFile = "data/bombay_wl.csv",
                           datasetFile = "data/bombay_query.csv",
                           numTopic = 50)

labels <- readLabelFile("data/label_bombay.txt")

termTopicDistribution <- exp(attr(VEM, "beta"))
library(wordcloud)
for (topic in 1:50) {
  index <- which(termTopicDistribution[topic, ] > 0.01)
  pdf(paste0("data/topic", topic, ".pdf"))
  wordcloud(labels[index], termTopicDistribution[topic, index])
  dev.off()
}

