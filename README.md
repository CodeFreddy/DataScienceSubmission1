# DataScienceSubmission1

Team 3
Github: https://github.com/CodeFreddy/DataScienceSubmission1.git

Usage:
1.	Clone the project to local or server
2.	Direct into the folder and run “mvn clean package” to generate the jar file which will located under “target” folder
3.	Run command :
“java -jar  ./target/CS953-1.0-SNAPSHOT-jar-with-dependencies.jar [index’s dir] [query file dir] [output dir]”
[index’s dir]: the directory of index, which is “/home/xl1044/ds/index ” for our project
[query file dir]: the query file’s location. We used “train.pages.cbor”
[output dir]: please specific the folder to save the run files
4.	The program runs taking a little while. Run by “nohup” will be a good idea
