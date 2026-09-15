### Linux Compatibility

1. Clone the repo 

```bash
git clone https://github.com/ClupTheGreat/Protter-linux.git
```

2. Download MiKTeX from https://miktex.org/download or run the following commands

```bash
curl -fsSL https://miktex.org/download/key | sudo gpg --dearmor -o /usr/share/keyrings/miktex.gpg
echo "deb [signed-by=/usr/share/keyrings/miktex.gpg] https://miktex.org/download/ubuntu jammy universe" | sudo tee /etc/apt/sources.list.d/miktex.list
sudo apt-get update
sudo apt-get install miktex
miktexsetup finish
```

3. Make sure port 8081 isnt being used, or use another port by setting the port variable to your desired port in protter.config

4. Run this command

```bash
java --add-opens=java.base/java.lang=ALL-UNNAMED -jar protterServer.jar
```

### Using Docker (Recommended)

1. Build the docker image

```bash
cd Protter-linux
docker build -t protter .
```
2. Create and run a docker container with the protter image.

```bash
docker run -d -p 8081:8081 protter
```

***


Welcome to Protter � the open-source tool for interactive integration and visualization of annotated and predicted protein sequence features together with experimental proteomic evidence!

### Version 1.0 released!
The code for the final version 1.0 has just been released, check it out at GitHub (https://github.com/ulo/Protter).
A public instance of the Protter Server can be found at http://wlab.ethz.ch/protter/

### How to install your own Protter server on  Windows (XP or higher):
1. make sur you have an up-to-date Java runtime enivronment
2. download MiKTeX from http://miktex.org/download
3. install MiKTeX using default settings and make sure to select "Yes" for the "Install missing packages on-the-fly" option during setup.
4. download the complete Protter package from http://ulo.github.io/Protter/ and extract the archive to your local drive (e.g. "C:\Protter")
5. take a look at the "protter.config" file to configure the server to your enivronment
6. double-click the extracted "protterServer.jar" file and open your browser at http://localhost:81/

Note #1: The very first time your Protter server is asked to generate a protein visualization, a MiKTeX window will pop-up where you have to confirm installation of tex\latex\textopo\textopo.sty.
Note #2: The API to UniProt is changing every second month or so. If Protter gives you error messages regarding UniProt, shut down the Protter server, get the latest [uniprotjapi.jar](http://www.ebi.ac.uk/uniprot/remotingAPI/) and copy it to the protterServer_lib subfolder (e.g. C:\Protter\protterServer_lib).
Note #3: To access you Protter server from other machines in your network, make sure to open the used port (81 per default) in the Windows Firewall.

### Protter manuscript published
A manuscript on Protter has been published in Bioinformatics, 2013:
http://bioinformatics.oxfordjournals.org/content/early/2013/11/21/bioinformatics.btt607.long

### Contact
If you have questions, encounter any error or bug, or have suggestions on how to improve Protter, please contribute to the Protter discussion group: https://groups.google.com/d/forum/protter

### License
This projected is licensed under the terms of the GNU General Public License v3.0.