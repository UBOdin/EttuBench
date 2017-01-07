#' compute DunnIndex measure
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @return a number that measure the DunnIndex of the pairwise distance matrix given the labels of queries
#' @author Duc Luong
#'@references Zaki, Mohammed J., and Wagner Meira Jr. Data mining and analysis: fundamental concepts and algorithms. Cambridge University Press, 2014, Chapter 17.
#' @export

DunnIndex <- function(distMat, label) {
  # find minimum intercluster distance
  max_clust <- max(label)

  minInter <- 10000
  for (i in 1:(max_clust - 1)) {
    for (j in (i+1):max_clust) {
      temp <- minWeights(distMat, label, i, j)
      if (temp < minInter) {
        minInter <- temp
      }
    }
  }

  maxIntra <- -1
  for (i in 1:max_clust) {
    temp <- maxWeights(distMat, label, i, i)
    if (temp > maxIntra)
      maxIntra <- temp
  }
  result <- minInter / maxIntra
}

#' compute the BetaCV
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @return a number that measure the DunnIndex of the pairwise distance matrix given the labels of queries
#' @author Duc Luong
#' @details
#' The BetaCV measure is the ratio of the mean intracluster distance to the mean intercluster
#' distance. The smaller the BetaCV ratio, the better the clustering
#'@references Zaki, Mohammed J., and Wagner Meira Jr. Data mining and analysis: fundamental concepts and algorithms. Cambridge University Press, 2014, Chapter 17.
#' @export

BetaCV <- function(distMat, label) {
  max_clust <- max(label)
  # compute sum of all intracluster weights
  Win <- 0
  for (i in 1:max_clust) {
    Win <- Win + sumWeights(distMat, label, i, i)
  }
  # compute sum of all intercluster weights
  Wout <- 0
  for (i in 1:(max_clust-1)) {
    for (j in (i+1):max_clust) {
      Wout <- Wout + sumWeights(distMat, label, i, j)
    }
  }
  # number of intracluster edges
  Nin <- 0
  for (i in 1:max_clust) {
    n <- sum(label == i)
    Nin <- Nin + (n * (n-1) / 2)
  }
  # number of intercluster edges
  Nout <- 0
  for (i in 1:(max_clust - 1)) {
    for (j in (i+1):max_clust) {
      Nout <- Nout + sum(label == i) * sum(label == j)
    }
  }
  result <- (Win / Nin) / (Wout / Nout)
}

#' compute the sum of weight of all the connections between two clusters
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @param i,j cluster labels that we want to compute the sum of weights of all the edges between cluster i and cluster j
#' @return the sum of weights of all the connections between two clusters
#' @author Duc Luong

sumWeights <- function(distMat, label, i, j) {
  cluster_i <- which(label == i)
  if (i == j) {
    indices <- t(combn(cluster_i, 2))
  } else {
    cluster_j <- which(label == j)
    indices <- as.matrix(expand.grid(cluster_i, cluster_j))
  }
  result <- sum(distMat[indices])
}

#' compute the minimum weight of all of the connections between two clusters
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @param i,j cluster labels that we want to compute the minimum weight among all the edges between cluster i and cluster j
#' @return the minimum weight among all connections between two clusters
#' @author Duc Luong

minWeights <- function(distMat, label, i, j) {
  cluster_i <- which(label == i)
  if (i == j) {
    indices <- t(combn(cluster_i, 2))
  } else {
    cluster_j <- which(label == j)
    indices <- as.matrix(expand.grid(cluster_i, cluster_j))
  }
  v <- distMat[indices]
  v <- v[v > 1e-15]
  min(v)
}

#' compute the maximum weight of all of the connections between two clusters
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @param i,j cluster labels that we want to compute the maximum weight among all the edges between cluster i and cluster j
#' @return the maximum weight among all connections between two clusters
#' @author Duc Luong

maxWeights <- function(distMat, label, i, j) {
  cluster_i <- which(label == i)
  if (i == j) {
    indices <- t(combn(cluster_i, 2))
  } else {
    cluster_j <- which(label == j)
    indices <- as.matrix(expand.grid(cluster_i, cluster_j))
  }
  max(distMat[indices])
}

#' compute the average value of silhouette coefficients
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @return the average value of silhouette coefficients
#' @author Duc Luong
#' @export

avgSilhoette <- function(distMat, label) {
  sil <- silhouette(label, as.dist(distMat))
  result <- summary(sil)$avg.width
  result
}

#' produce silhoette plot of the sql query similarity with their respective labels
#' @param distMat a n x n matrix of pairwise distance between a set of n queries
#' @param label a vector of size n that contains the labels of n queries
#' @param fileName the output file of the silhoutte plot
#' @author Duc Luong
#' @export
silhouettePlot <- function(distMat, label, fileName) {
  sil <- silhouette(label, as.dist(distMat))
  fviz_silhouette(sil, print.summary = FALSE) +
    ylab("Silhouette Coefficient") + xlab("Cluster") +
    ggsave(filename = fileName)
}

#' read full distance matrix
#' @param fileName the input file that contains full n x n matrix (n rows and n columns)
#' @return distMat a n x n matrix of pairwise distance between a set of n queries
#' @author Duc Luong
#' @export
readDistMat <- function(fileName) {
  as.matrix(read.csv(file = fileName, header = FALSE))
}

#' read sparse distance matrix (not yet implement)
#' @param fileName the input file that contains matrix in sparse representation (csv file with header row, col, val)
#' @return distMat a n x n matrix of pairwise distance between a set of n queries
#' @author Duc Luong
#' @export
readSparseDistMat <- function(fileName) {
  # not yet implement
}



#' read feature vector file in sparse representation
#' @param fileName the input file with 3 columns (row, col, val)
#' @return a n x p matrix where n is the number of queries and p is the number of features
#' @author Duc Luong
#' @export
readFeatureVectors <- function(fileName) {
  dataset <- read.csv(file = fileName, header = TRUE)
  featureMat <- sparseMatrix(i = dataset$row, j = dataset$column, x = dataset$value)
  #featureMat <- featureMat[rowSums(featureMat) != 0, colSums(featureMat) != 0]
}

#' compute pairwise distance between queries from the set of feature vectors using cosine similarity
#' @param featureMat a nxp matrix where n is the number of queries and p is the number of features
#' @return a nxn matrix in which each entry contains a pairwise distance between queries
#' @author Duc Luong
#' @export
computeCosinePairwiseDistance <- function(featureMat) {
  distMat <- 1-cosine(as.matrix(t(featureMat)))
}

#' read the label file that contains the list of labels which outputs from WL algorithm
#' @param filename file that contains label file
#' @return the list of labels
#' @author Duc Luong
#' @export
readLabelFile <- function(filename) {
  labels <- c()
  read_con <- file(filename, "rt")
  label <- ""
  count <- 0
  while (TRUE) {
    lines <- readLines(read_con, 50000) # read 50000 line at a time
    if (length(lines) < 1) {
      break
    } else {
      for (line in lines) {
        if (grepl("^[0-9]", line) && grepl("[0-9]$", line)) {
          if (as.integer(line) == count + 1) {
            if (label != "") {
              labels <- c(labels, label)
              label <- ""
            }
            count <- count +1
          } else {
            label <- paste0(label, line)
          }

        } else {
          label <- paste0(label, line)
        }
      }
    }
  }
  labels <- c(labels, label)
  # close connections
  close(read_con)
  labels
}

#' perform topic modeling using feature vector file and compare query similarity using Hellinger distance
#' @param labelFile file that contains list labels
#' @param featureVecFile file that contains feature vectors in sparse representation (row, col, val). Each row is a feature vector for one corresponding query
#' @param datasetFile file that contains other information about queries (query itself, id, label)
#' @param numTopic number of topics
#' @param SEED the random seed for LDA using VEM algorithm
#' @return the n x n matrix that contains distance between queries using Hellinger distance
#' @author Duc Luong
#' @export
computeDistMatTopic <- function(labelFile, featureVecFile, datasetFile, numTopic, SEED = 2010) {
  VEM <- getTopicModelResult(labelFile, featureVecFile, datasetFile, numTopic)
  # get the distribution of topics for each query, each row is a query
  topic_dist <- attr(VEM, "gamma")
  # compute the pairwise distance between queries using topic distribution
  result <- distHellinger(topic_dist)
}

#' perform topic modeling using feature vector file
#' @param labelFile file that contains list labels
#' @param featureVecFile file that contains feature vectors in sparse representation (row, col, val). Each row is a feature vector for one corresponding query
#' @param datasetFile file that contains other information about queries (query itself, id, label)
#' @param numTopic number of topics
#' @param SEED the random seed for LDA using VEM algorithm
#' @return the topic model result object
#' @author Duc Luong
#' @export
getTopicModelResult <- function(labelFile, featureVecFile, datasetFile, numTopic, SEED = 2010) {
  labels <- readLabelFile(labelFile)
  featureMat <- readFeatureVectors(featureVecFile)
  dataset <- read.csv(file = datasetFile, header = TRUE, sep = "\t")
  colnames(featureMat) <- labels
  rownames(featureMat) <- dataset$id
  dtm <- as.DocumentTermMatrix(featureMat, weighting = weightTf)
  VEM <- LDA(dtm, k = numTopic, control = list(seed = SEED, estimate.beta = TRUE))
}
