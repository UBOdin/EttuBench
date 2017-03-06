## Introduction
This repository contains all the code and data we use to produce experimental results in paper "Towards Effective Log Clustering"

## Organization of the repository
- data folder: Contains all data files that are used in the experiments
- figure folder: This folder is used to store all the output figures from experiments
- SQLFeatureExtraction: This folder stores Java code that we used to extract features from SQL queries and producing pairwise distance matrix 
- evaluation.R: contains implementation of 3 clustering validation measures including (average silhouette coefficients, Dunn Index, BetaCV). It also contains function to provide plot for distribution of silhouette coefficients.
- utils.R: other supporting functions such as reading distance matrix.
- script_figure_2.R: produce Figure 2 as shown in the paper.
- script_figure_3.R: produce Figure 3 as shown in the paper.
- script_figure_4.R: produce Figure 4 as shown in the paper.

## Organization of *data* folder
There are three datasets that are used in all experiments including: IIT Bombay (bombay), UB Exam (ub) and PocketData-Google+ (googleplus) datasets. The queries with corresponding labels in each dataset is stored in the file **[data_set]_queries.csv**.

There are three different SQL query similarity metrics that are used in all experiments including: Aligon (aligon), Aouiche (aouiche), Makiyama (Makiyama). 

The pairwise distance matrix between queries in a dataset using a particular similarity metric without regularization step is stored in the file **[dataset]_[similarity_metric].csv**. For instance, the distance matrix for PocketData-Google+ dataset with Makiyama similarity metric without regularization is stored in the file *googleplus_makiyama.csv*.

When regularization is used, the pairwise distance matrix is stored in the file **[dataset]_[similarity_metric]_regularization.csv**. For example, the distance matrix for UB Exam dataset with Aoiuche similarity metric when regularization is applied is stored in the file *ub_aouiche_regularization.csv*.

In order to evaluate different modules in regularization step, we consider 4 different modules: Naming (denote as 1), Expression Standardization (2), FROM-nested Subquery (3) and UNION Pull-out (4). The pairwise distance matrix when each module is applied individually is stored in the file **[dataset]_[similarity_metric]_regularization_[module_number]**. For example, the distance matrix for IIT Bombay dataset with Aligon similariy metric when Naming module in regularization step is used is stored in the file *bombay_aligon_regularization_1.csv*.

**result.csv** contains all the numbers that are needed to produce Figure 3 in the paper.

**modules.csv** contains all the number that are needed to produce Figure 4 in the paper.

## Reproducing experimental results
### Extracting features from SQL queries and computing pairwise distance matrix
Our Java code for extracting features frm SQL queries and compute the pairwise distance matrix among queries is given in folder **SQLFeatureExtraction**. The Java source code is provided in folder **SQLFeatureExtraction/src**. 

**SQLComparison.jar** is an executable file that user can use to reproduce all pairwise distance matrices with all possible options for regularization. This jar file can be run from command line as follow:

    java -jar SQLComparison.jar [-options]

where the possible options are as follow:
- dataset(*ub*, *bombay* or *googleplus*) to be applied by using option *-input*. If no *-input* option is given, all three datasets *ub*, *bombay* and *googleplus* will be used by default to reproduce all possible pairwise distance matrices with all regularization options. For example, the *ub* dataset can be specified as follow:

    java -jar SQLComparison.jar -input ub

- similarity metric (*aligon*, *makiyama* or *aouiche*) to be applied by using option *-metric*. If no *-metric* option is given, all three metrics including *aligon*, *aouiche* and *makiyama* will be used by default. For example, user can specify the metric as *aligon* using the following command:

    java -jar SQLComparison.jar -metric aligon

- query regularization module (ID=1: Naming; ID=2: Expression Standardization; ID=3: Flattening From-Nested Sub-query; ID=4: Union Pull-out) can be specified by using option *-modules*. User can specify multiple modules by using their IDs with "&" delimiter. If no *-modules* option given, all modules will be used by default. For example, if user wants modules Naming(ID=1) and Expression Standardization(ID=2) to be applied, the following command can be used: 

    java -jar SQLComparison.jar -modules 1&2

The output distance matrices can be found in folder *SQLFeatureExtraction/data/*

### Reproduce figure 2
In order to reproduce distribution of silhouette coefficients when using Aligon similarity without regularization and when regularization is applied as shown in Figure 2 of the paper, users can open the file *script_figure_2.R*. Running this script file will produce the silhouette plots in folder *figure*.

### Reproduce figure 3
In order to reproduce the plots for comparison between three similarity metrics (Aligon, Aouiche, Makiyama) on three datasets (IIT Bombay, UB Exam and PocketData-Google+ datasets) with and without regularization as shown in Figure 3 of the paper, users can use the file *script_figure_3.R*. 

This script requires an input file *result.csv* in *data* folder. We have filled all the numbers for this file. For reproducibility, the numbers in *result.csv* can be manually filled by running the following commands in R:

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
    distMat <- readDistMat("./data/bombay_aligon_regularization.csv") 
    
    # print different clustering validation measures
    print(avgSilhoette(distMat, dataset$label))
    print(BetaCV(distMat, dataset$label))
    print(DunnIndex(distMat, dataset$label))

When the input file is ready, running *script_figure_3.R* file will produce the corresponding figures in folder *figure*.

### Reproduce figure 4
In order to reproduce the plots for comparing the effect of different modules in regularization as shown in Figure 4 of the paper, users can use the file *script_figure_4.R*. 

This script requires an input file *modules.csv* in *data* folder. We have filled all the numbers in this file. For reproducibility, the numbers in this file can be manually filled by computing average silhouette coefficients, BetaCV and Dunn Index for each module in regularization.

When the input file is ready, running this *script_figure_4.R* will produce the corresponding figure in folder *figure*.