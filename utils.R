#' read full distance matrix
#' @param fileName the input file that contains full n x n matrix (n rows and n columns)
#' @return distMat a n x n matrix of pairwise distance between a set of n queries
#' @author Duc Luong
#' @export
readDistMat <- function(fileName) {
  as.matrix(read.csv(file = fileName, header = FALSE))
}

