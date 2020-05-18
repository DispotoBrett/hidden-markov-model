if [ "$1" == "train" ]; then
  echo 'Training'
  javac Bagging.java
  java Bagging train
elif [ "$1" == "test" ]; then
  echo 'Testing'
  javac Bagging.java
  java Bagging test
else
  echo 'Exiting'
  exit
fi
