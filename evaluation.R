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
  if (length(unique(label)) < 9) {
    fviz_silhouette(sil, label = FALSE, print.summary = FALSE, 
                    palette = "Dark2",
                    ylab = "Silhouette Coefficient", main="", ylim=c(-0.8, 0.9)) +
      theme_minimal(base_size=12)+
      theme(axis.text.x = element_blank(), axis.ticks.x = element_blank()) +
      ggsave(filename = fileName)  
  } else {
    getPalette = colorRampPalette(brewer.pal(9, "Set1"))
    fviz_silhouette(sil, label = FALSE, print.summary = FALSE, 
                    palette = getPalette(length(unique(label))),
                    ylab = "Silhouette Coefficient", main="", ylim=c(-0.8, 0.9)) +
      theme_minimal(base_size=12)+
      theme(axis.text.x = element_blank(), axis.ticks.x = element_blank()) +
      ggsave(filename = fileName)  
  }
  
}
