# Hacker-Coins

https://www.hackercoins.org

## What are Hacker-Coins?

Hacker-Coins is a free open-source cryptocurrency based on blockchain technology.
The architecture allows to send transactions between addresses and to earn coins with mining.

Additionally, the Hacker-Coins systems allows the deployment of crackactions which are announcements of a reward for successfully cracking a hash. 
Similar to transactions, crackactions are mined to the blockchain as well.

Every participant can try to solve crackactions and earn stated rewards.

Please read the white paper for technical details: https://www.hackercoins.org/Hacker-Coins.pdf

## License

The Hacker-Coins agent software is released under the terms of the MIT license (https://opensource.org/licenses/MIT).

## Requirements

Java Runtime Environment (Version >= 1.7)

## How to use

Click on the .jar-file to use the agent in gui-mode (e.g. on Windows).

Execute the .jar-file with the following command line options in order to run it in non-gui-mode:

$ java -jar hackercoins.jar mining=(threads) cracking=(threads) send=(address-to-wich-rewards-are-sent) passwd=(for-wallet)