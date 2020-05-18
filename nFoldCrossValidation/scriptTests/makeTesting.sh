if [ -d 'testSet' ]; then
  echo 'Exiting.'
  exit
fi

echo 'Spliting Dataset'
mkdir testSet

printf "Files found: "
count=$(ls | grep '.asm.txt' | wc -l)
echo $count

printf "Test set size: "
testCount=$(expr $count / 10)
echo $testCount

#Shuffle the files
files=$(ls | shuf | grep -m $testCount '.asm.txt')

for file in $files
do
  mv $file testSet/
done
