## Introduction
This repository is used to store all the code and data we use to produce experimental results in paper "Towards Effective Log Clustering"

## Organization of the repository
- data folder: Contains all data files that are used in the experiments
- figure folder: This folder is used to store all the output figures from experiments
- evaluation.R: contains implementation of 3 clustering validation measures including (average silhouette coefficients, Dunn Index, BetaCV). It also contains function to provide plot for distribution of silhouette coefficients.
- utils.R: other supporting functions such as reading distance matrix.
- script_figure_2.R: produce Figure 2 as shown in the paper.
- script_figure_3.R: produce Figure 3 as shown in the paper.
- script_figure_4.R: produce Figure 4 as shown in the paper.

## Reproducing experimental results
### Reproduce figure 2
In order to reproduce distribution of silhouette coefficients when using Aligon similarity without regularization and when regularization is applied as shown in Figure 2 of the paper, users can open the file *script_figure_2.R*. Running this script file will produce the silhouette plots in folder *figure*.

### Reproduce figure 3
In order to reproduce the plots for comparison between three similarity metrics (Aligon, Aouiche, Makiyama) on three datasets (IIT Bombay, UB Exam and PocketData-Google+ datasets) with and without regularization as shown in Figure 3 of the paper, users can use the file *script_figure_3.R*. This script requires an input file *result.csv* in *data* folder. The number in *result.csv* can be filled in by running the following commands in R:

    # load two files evaluation.R and utils.R
    source(file = "./evaluation.R")
    source(file = "./utils.R")
    
    # load supporting libraries
    library(cluster)
    library(factoextra)
    library(RColorBrewer)
    
    # read data file
    dataset <- read.csv(file = "./data/bombay_queries.csv", header = TRUE, sep = "\t")
    
    # read distance matrix
    distMat <- readDistMat("./data/bombay_aligon.csv") 
    
    # print different clustering validation measures
    print(avgSilhoette(distMat, dataset$label))
    print(BetaCV(distMat, dataset$label))
    print(DunnIndex(distMat, dataset$label))

When the input file is ready, running 'script_figure_3.R' file will produce the corresponding figures in folder *figure*.

### Reproduce figure 4
In order to reproduce the plots for comparing the effect of different modules in regularization as shown in Figure 4 of the paper, users can use the file *script_figure_4.R*. This script requires an input file *modules.csv* in *data* folder. Running this script will produce the corresponding figure in folder *figure*.
