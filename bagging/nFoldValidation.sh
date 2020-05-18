RED="printf '\033[31m'"
GREEN="printf '\033[32m'"
YELLOW="printf '\033[33m'"
BLUE="printf '\033[34m'"
PINK="printf '\033[35m'"
NORMAL="printf '\033[0;39m'"

WINWEBSEC=" /home/brett/Projects/hidden-markov-model/Opcodes/winwebsec/converted_to_symbols"
ZBOT=" /home/brett/Projects/hidden-markov-model/Opcodes/zbot/converted_to_symbols"
ZERO_ACCESS=" /home/brett/Projects/hidden-markov-model/Opcodes/zeroaccess/converted_to_symbols"
BAGGING_DIR="/home/brett/Projects/hidden-markov-model/bagging"

ALIASES=( "${WINWEBSEC}" "${ZBOT}" "${ZERO_ACCESS}" )

alias goHome='cd /home/brett/Projects/hidden-markov-model/bagging'

printf "Generating bags...\n"
eval $PINK

for FAM_DIR in "${ALIASES[@]}"
do
  cd $FAM_DIR

  TRAIN_SIZE=$(ls | wc -l)
  TEST_SIZE=$(ls testSet/ | wc -l)
  ALL_SIZE=$((TEST_SIZE+TRAIN_SIZE))
  
  mv testSet/* ./  2> /dev/null
  rm -rf testSet 2> /dev/null

  ./makeTesting.sh  | head -n 1 2> /dev/null

  cd $BAGGING_DIR
done

javac Bagging.java

printf 'Training'
java Bagging train

printf 'Testing'
java Bagging test
