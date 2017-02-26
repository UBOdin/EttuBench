## Reproducing experimental results
The feature extraction process and query comparison process have been wrapped up into a maven project rooted at "SQLFeatureExtraction" directory.
user can just change directory to "SQLFeatureExtraction" and use mvn clean install or mvn install 

Above command will install the maven project and create a executable jar file named "SQLComparison.jar" in the directory "SQLFeatureExtraction/target"
User can change directory to that folder and type java -jar SQLComparison.jar [-options]

There are options that enables user to change 
(1) dataset(ub ,bombay or googleplus dataset) to be applied by using -input. e.g. java -jar SQLComparison.jar -input ub
If no -input option given, all datasets ub,bombay and googleplus will be all used by default to reproduce the full experiment result.
(2) Similarity Metrics (Aligon,Makiyama or Aouiche) to be applied by using -metric ;  e.g. java -jar SQLComparison.jar -metric aligon
If no -metric option given, all metrics aligon,aouiche and makiyama will be all used by default to reproduce the full experiment result.
(3) Query Regularization Modules (ID=1: Naming; ID=2: Expression Standardization; ID=3: Flattening From-Nested Sub-query; ID=4: Union Pull-out) by using -modules. User can specify multiple modules to be applied by using their IDs with "&" delimiter. e.g. if the user want modules Naming(ID=1) and Expression Standardization(ID=2) to be applied, type java -jar SQLComparison.jar -modules 1&2
If no -modules option given, all modules will in turn be used by default to reproduce the full experiment result.
