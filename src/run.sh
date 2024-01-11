#!/bin/bash

javac *.java **/*.java

java DatabaseNode -tcpport 9991 -record 1:2 &
java DatabaseNode -tcpport 9992 -record 3:4 -connect localhost:9991 &&
fg

kill -9 $(lsof -t -i:9991)
kill -9 $(lsof -t -i:9992)
