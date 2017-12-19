#!/bin/bash
#
#Note the directories listed in the parameters below must exist for this scripts to work!
#
#Set all of the parameters which will be used for squashing.
filedate=$(date +"%Y%m%d%H%M%S") 		#Date variable which can be added to filenames.
fin="pcmm.csv"							#Input file name which will be used.
fout="Squashed.csv"						#Output file name which will be used.
directory="/net_home/ahanson/PCMM"		#the original working directory in which the modifications should operate.
working="working"						#Directory where working modifications are made. This directory should be empty before and after this script operates.
done="done"								#Directory where the completed files should be placed.
old="old"								#Directory where the original file we received is placed with a date as an audit of what was previously transformed.
logfile="$directory/logs/$filedate.log"	#Directory where the log files will be stored.
#
#Check to make sure that all of the directories exist and output an error then exit if they do not.
cd "$directory" || (echo "Failed to find parent directory ($directory)." && exit)
cd "$working" || (echo "Failed to find $working directory." >> "$logfile" && exit)
cd .. #Move back to the original directory.
cd "$done" || (echo "Failed to find $done directory." >> "$logfile" && exit)
cd .. #Move back to the original directory.
cd "$old" || (echo "Failed to find $old directory." >> "$logfile" && exit)
cd ..
#
#Prepare the screen output by adding a blank line.
echo " "


#
#Move the file to be squashed into the working directory and change directories to it.
cp "$fin" "./$working/$fin" && (printf "%s Copied input to working directory.\n" $(date +"%Y%m%d%H%M%S") | tee -a "$logfile")
cd "$working" #Don't need the extra check for this directory as that was confirmed above!
#
#Make sure that all of the variables are initialized which will be used for file manipulation.
o1="" 
o2=""
o3=""
o4=""
o5=""
o6=""
o7=""
o8=""
o9=""
o10=""
o11=""
#o12="" #These 2 original values will be compressed in the file into o11 instead of using their own variable.
#o13=""
o14=""
o15=""
count=0
#
#Loop through the file aggregating and making changes.
printf "%s Beginning file loop" $(date +"%Y%m%d%H%M%S") | tee -a "$logfile"	#Print without line break so that it will show as continued by the periodic loop output.
while IFS=',' read -r f1 f2 f3 f4 f5 f6 f7 f8 f9 f10 f11 f12 f13 f14 f15  
do
	#
	#Trim the line break from the last data column to prevent the output file from containing extra linebreaks.
	f15=${f15//[$'\n\r']}
	#
	#Update the line cout and if this line is a multiple of 100 or 1000 print data to the screen to let the end user know that this is still working.
	count=$((++count))
	if ! ((count % 100)); then printf "." | tee -a "$logfile"; fi
	if ! ((count % 1000)); then printf "%d" $((count/1000)) | tee -a "$logfile"; fi
	#
	#Handle the inputs from this file line based on the line and the current state of the internal storage.
	if  [ -z "$o1" ]; then  #If the original values haven't been set we are on the first line of the file.
		o1=$f1
		o2=$f2
		o3=$f3
		o4=$f4
		o5=$f5
		o6=$f6
		o7=$f7
		o8=$f8
		o9=$f9
		o10=$f10
		o11="$f11\' $f12 $f13"
		#o12=$f12	#Again these values are not in use as they get added above.
		#o13=$f13
		o14=$f14
		o15=$f15
	elif [ "$o1" == "$f1" ]; then  #If this line matches the previous then we need to aggregate it!
		o1=$f1
		o2=$f2
		o3=$f3
		o4=$f4
		o5=$f5
		o6=$f6
		o7=$f7
		o8=$f8
		o9="$o9~$f9"
		o10="$o10~$f10"
		o11="$o11~$f11\' $f12 $f13"
		#o12="$o12~$f12"  #Again these values are not in use as they get added above.
		#o13="$o13~$f13"
		o14="$o14~$f14"
		o15="$o15~$f15"
	else
		#Print out the values to the output file. Then reset the internal storage variables to match the new line data.
		echo "$o1,$o2,$o3,$o4,$o5,$o6,$o7,$o8,$o9,$o10,$o11,$o14,$o15" >> "$fout"
		o1=$f1
		o2=$f2
		o3=$f3
		o4=$f4
		o5=$f5
		o6=$f6
		o7=$f7
		o8=$f8
		o9=$f9
		o10=$f10
		o11="$f11\' $f12 $f13"
		#o12=$f12	#Again these values are not in use as they get added above.
		#o13=$f13
		o14=$f14
		o15=$f15
	fi
done <"$fin"
#Write the final line so it isn't lost.
echo "$o1,$o2,$o3,$o4,$o5,$o6,$o7,$o8,$o9,$o10,$o11,$o14,$o15" >> "$fout"
printf "done.\n" | tee -a "$logfile"	#add a new line to prevent output from piling up on single line.
printf "%s Cleanup all working files.\n" $(date +"%Y%m%d%H%M%S") | tee -a "$logfile"
#
#Move the file to the done location with an appropriate date name and delete from the working directory.
cp "./$fout" "../$done" #Copy the output file to the done directory.
rm "$fout"
rm "$fin"
cd .. #back out of the working directory.
#
#Move the original file for later audit and remove it from the parent directory.
cp "./$fin" "./$old/Original - $filedate.csv"
rm "$fin" ##remove the original so that the processing placing this here knows it has been completed.
printf "%s Complete!\n" $(date +"%Y%m%d%H%M%S") | tee -a "$logfile"
echo " "	#add extra line break to make reading output easier.
