javac -classpath . com/botbox/util/*.java
javac -classpath . se/sics/isl/transport/*.java
javac -classpath . se/sics/isl/gui/*.java
javac -classpath . se/sics/isl/util/*.java
javac -classpath . se/sics/tasim/props/*.java
javac -classpath . se/sics/tasim/aw/*.java
javac -classpath . se/sics/tasim/aw/client/*.java
javac -classpath . se/sics/tasim/tac03/aw/*.java
javac -classpath . *.java
jar cfm scmaw.jar AWManifest.txt images/*.* com/botbox/util/*.class se/sics/isl/transport/*.class se/sics/isl/gui/*.class se/sics/isl/util/*.class se/sics/tasim/props/*.class se/sics/tasim/aw/*.class se/sics/tasim/aw/client/*.class se/sics/tasim/tac03/aw/*.class *.class
