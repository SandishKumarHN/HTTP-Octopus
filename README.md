HTTP Octopus
Every day an http-octopus takes some http-food from the internet. It has several limbs so may easily grab meals simultaneously. Sometimes the octopus moves glasses full of http-liquids and does that very carefully to prevent spills.

The goal is to simulate the octopus in a console tool.

Input:
n (number of limbs) the number of downloading threads (1,2,3,4...)
l (carefulness) global download limit, bytes/second with suffixes (k=1024, m=1024*1024)
i (ingredients) input file with links (might be equal)
o (meals) output folder for downloaded files

Input file format:
http_link downloaded_file_name

Example of the input file (the links are for example purpose only):
http://octopus-market.com/eggs.zip scrambled_eggs.zip
http://octopus-shop.com/water.jpg tea.jpg
http://octopus-shop.com/water.jpg coffee.jpg
http://octopus-bakery.com/bread.bin sandwich.bin

Some working links:
http://ipv4.download.thinkbroadband.com/1MB.zip
http://ipv4.download.thinkbroadband.com/2MB.zip
http://ipv4.download.thinkbroadband.com/5MB.zip

Output:
The time spent and number of downloaded bytes
Running time, seconds: N
Bytes downloaded: M

Example of a command to run the tool:
sbt "runMain com.octopus.http.core.OctopusMain -n 8 -l 1024k -o breakfast_folder -i ingredients.txt"

Example:
sbt "runMain com.octopus.http.core.OctopusMain -n 4 -l 5m -i /Users/sany/Downloads/inputSnap.txt -o /Users/sany/Downloads/"
